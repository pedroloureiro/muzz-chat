# ADR-0001: Repository dual data source, with optimistic pending messages swapped for the server's copy

## Status
Accepted, revised. The first version updated a message's status in place when its echo arrived. This version replaces the pending message with the server's copy, matched by `localId`, so that the server owns each message's identity and order. [ADR-0002](0002-chat-domain-layer.md) adds `SendMessageUseCase` ahead of the repository in the send flow, and [ADR-0003](0003-cursor-paging.md) adds paging on top of this flow.

## Context
The exercise is local-only with no real server. The architecture should still mirror how a production chat system works, so that a real backend can replace the mock with minimal change.

In production chat apps, the server is the authority on every message. It assigns the message's ID, its position in the conversation and its timestamp, and it sends the stored copy back to the sender, usually over a WebSocket. The sender's device only proposes a message.

The user still needs immediate feedback, so the app shows the message before the server confirms it. That leaves two copies of one message: the device's proposal and the server's confirmed copy. They need a shared key, so the app can replace one with the other.

## Decision

### Dual data source
The repository coordinates two data sources:

- `ChatRemoteDataSource`: sends messages, fetches history in pages, and exposes `incomingMessages(): Flow<MessageDto>` (a WebSocket in production).
- `ChatLocalDataSource`: Room, the single source of truth for the UI.

### Two kinds of message
`Message` is a sealed class:

- `Message.Pending` is a message this device has sent that the server has not confirmed. It has a `localId` generated on the device and a device `timestampMillis`, and no server `id` or `sequenceId`.
- `Message.Confirmed` is a message the server has stored. It has the server's `id`, `sequenceId` and `timestampMillis`. When this device sent it, it also keeps the `localId` it was sent with.

Both are stored in one Room table. A row with no server `id` is pending.

### The echo replaces the pending message
On send, the repository inserts a `Pending` row so the message shows at once. The server echoes its stored copy together with the `localId` it was sent with. In a single Room transaction, the repository deletes the row with that `localId` and inserts the confirmed message. Because it is one transaction, Room notifies observers once, and the list never shows both copies or neither.

The confirmed row keeps the `localId`, so the UI can key a message by `localId` when it has one. The bubble then stays in place as it changes from pending to confirmed, rather than being removed and added again.

`localId` is also an idempotency key. If the device resends a pending message, for example after the app was killed before the echo arrived, the server recognises the `localId` and returns the existing message instead of storing a second one.

### Full send flow
```
User taps send
    → SendMessageUseCase builds Message.Pending(localId, timestampMillis = device time)
    → Repository inserts the pending row into Room
    → UI shows the message at the end of the list with the SENDING indicator
    → Repository calls RemoteDataSource.sendMessage(dto)   ← returns Result<MessageDto>
    → The server (the mock) stores it, assigns id, sequenceId and timestamp,
      and echoes it with the localId after ~200ms
    → Repository, in one transaction: delete the row with that localId, insert the confirmed message
    → Room invalidates the paged query → ChatItemMapper → UI shows it as SENT, in its server position

On failure (future):
    → Repository marks the pending message FAILED
    → UI renders a retry affordance
    → A retry resends the same localId, so the server cannot store it twice
```

### Incoming messages
```
RemoteDataSource.incomingMessages() emits MessageDto
    → If it carries a localId this device sent, the same transaction replaces the pending row
    → Otherwise it is inserted as a confirmed message
    → Room invalidates the paged query → UI
```
An echo can arrive twice, once as the `sendMessage` result and once on `incomingMessages()`. The second copy is harmless: no pending row is left to delete, and the insert replaces the confirmed row with the same server `id`.

### Ordering
Confirmed messages are ordered by `sequenceId`, which the server assigns per conversation, increasing by one with no gaps. Pending messages have no `sequenceId`. They sort after every confirmed message, by device timestamp. `timestampMillis` is used only for display and grouping, never for order.

### Error handling seam
`ChatRemoteDataSource.sendMessage()` returns `Result<MessageDto>`, not `Unit`. The failure branch exists in the repository with a `// TODO` marking where failure and retry are wired in.

## Consequences
- Room is the single source of truth, and the UI reads only from the local database.
- The server owns each message's identity, order and timestamp. A device clock that is wrong cannot reorder the conversation.
- `MessageDirection` is derived at mapping time from `senderId`, never from the code path.
- `MockChatRemoteDataSource` can be replaced by a real `WebSocketChatDataSource` with no changes above the repository.
- A sent message appears at once as SENDING, then moves to its server position as SENT after ~200ms. This intentionally mirrors real behaviour.
- The delivery status follows from the message type: `Pending` is `SENDING` and `Confirmed` is `SENT`. Failure handling adds a state to `Pending` without changing the structure.
