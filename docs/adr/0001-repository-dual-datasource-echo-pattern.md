# ADR-0001: Repository dual data source with mock echo pattern and optimistic delivery status

## Status
Accepted. [ADR-0002](0002-chat-domain-layer.md) adds `SendMessageUseCase` ahead of the repository in the send flow.

## Context
The exercise is local-only with no real server. However, the architecture should mirror how a production chat system works so that swapping in a real backend requires minimal change.

In production chat apps, messages the local user sends never enter the local database directly. They are POSTed to the server, which echoes them back over a WebSocket. The app persists a message after receiving the server's authoritative copy. This guarantees server-side ordering, deduplication, and rejection handling.

The alternative — writing directly to Room on send — is simpler but diverges from production behaviour, creates two code paths (sent vs received), and makes error handling and retry logic impossible to retrofit cleanly.

Adding a simulated delay (~200ms) on the mock echo makes delivery status necessary: the user needs immediate visual feedback that their message is in-flight before confirmation arrives.

## Decision

### Dual data source
The repository coordinates two data sources:

- `ChatRemoteDataSource` — handles outgoing sends and exposes `incomingMessages(): Flow<MessageDto>` (WebSocket in production)
- `ChatLocalDataSource` — Room; the single source of truth for the UI

### Optimistic insert with status tracking
On send, the message is inserted into Room immediately with `status = SENDING`. When the echo arrives, the status is updated to `SENT`. This gives instant UI feedback while preserving the server-echo-as-source-of-truth contract.

```kotlin
enum class MessageDeliveryStatus {
    SENDING,    // optimistically in Room, echo pending
    SENT,       // echo confirmed by server
    // FAILED   // server rejected or timeout — add with retry logic
    // RETRYING // queued for resend — add with offline queue
}
```

### Full send flow
```
User taps send
    → Repository inserts MessageEntity(status = SENDING) into Room
    → UI shows message immediately with SENDING indicator
    → Repository calls RemoteDataSource.send(dto)   ← returns Result<MessageDto>
    → MockRemoteDataSource echoes dto after ~200ms
    → Repository receives echo → updates status = SENT in Room
    → Room Flow emits → ChatItemMapper → UI updates indicator

On failure (future):
    → Repository updates status = FAILED in Room
    → UI renders retry affordance
    → repository.retrySend(messageId) follows same send path
```

### Incoming messages (from other user or real WebSocket)
```
RemoteDataSource.incomingMessages() emits MessageDto
    → Repository converts dto → MessageEntity(status = SENT)
    → LocalDataSource.insert(entity)
    → Room Flow emits → UI
```

### Error handling seam
`ChatRemoteDataSource.send()` returns `Result<MessageDto>` not `Unit`. The failure branch exists in the repository with a `// TODO` comment marking where `FAILED` status and retry logic should be wired in. The architecture supports it without structural change.

## Consequences
- Room is the single source of truth — UI reads only from local DB
- `MessageDirection` is derived at mapping time from `senderId`, never from code path
- `MockChatRemoteDataSource` can be swapped for a real `WebSocketChatDataSource` with no changes above the repository
- A sent message appears in the UI immediately (SENDING) then updates to SENT after ~200ms — intentionally mirrors real behaviour
- Error handling and retry logic can be added without architectural change — the seam is already defined
