# ADR-0001: Repository dual data source, with optimistic pending messages confirmed in place by the server's copy

## Status
Accepted, revised. The first version updated a message's status in place when its echo arrived. The second replaced the pending message with the server's copy, matched by an id only the sending device knew, so that the server owns each message's order. A later revision moved order from a UUIDv7 `id` to a per-Chat `seq` with no holes, so that gaps can be detected. This version gives every message one key from the moment it is sent, its `senderId` and `clientMessageId`, which the server returns on every message from any sender, and confirms the pending row in place. [ADR-0002](0002-chat-domain-layer.md) adds `SendMessageUseCase` ahead of the repository in the send flow, and [ADR-0003](0003-cursor-paging.md) adds paging on top of this flow.

## Context
The exercise is local-only with no real server. The architecture should still mirror how a production chat system works, so that a real backend can replace the mock with minimal change.

In production chat apps, the server is the authority on every message. It assigns the message's server ID, its position in the Chat and its timestamp, and it sends the stored copy back to the sender, usually over a WebSocket. The sender's device only proposes a message.

The user still needs immediate feedback, so the app shows the message before the server confirms it. That leaves two copies of one message: the device's proposal and the server's confirmed copy. They need a shared key, so the app can confirm one with the other.

## Decision

### Dual data source
The repository coordinates two data sources:

- `ChatRemoteDataSource`: sends messages to a Chat, fetches a Chat's history in pages, and exposes `incomingMessages(): Flow<MessageDto>` for every Chat (one WebSocket in production).
- `ChatLocalDataSource`: Room, the single source of truth for the UI.

### Two kinds of message
`Message` is a sealed class:

- `Message.Pending` is a message this device has sent that the server has not confirmed. It has a `clientMessageId` generated on the device and a device `timestampMillis`, and no `serverMessageId` or `seq`.
- `Message.Confirmed` is a message the server has stored. It has the `clientMessageId` it was sent with, and the server's `serverMessageId`, `seq` and `timestampMillis`.

### One key from the moment a message is sent
The sender's device generates a `clientMessageId`, a random UUID, for each message it sends. The server stores it and returns it on every message, whichever device sent it, so every message has one.

A device only generates ids for its own messages, so a `clientMessageId` is unique per sender, not across senders. Another sender can reuse one, by accident or on purpose, and every member of the Chat sees each message's `clientMessageId`. A message is therefore identified by its `senderId` and `clientMessageId` together:

- On the server, `(senderId, clientMessageId)` is unique, and it is the idempotency key. The server takes the `senderId` from the authenticated call, never from the message, so a device can send only as its own user. If the device resends a pending message, for example after the app was killed before the echo arrived, the server recognises the pair and returns the message it already stored instead of storing a second one. The same `clientMessageId` from another sender is a different message.
- In Room, `(senderId, clientMessageId)` is the primary key. Pending and confirmed messages are stored in one table, and a row with no `serverMessageId` is pending. `serverMessageId` has a unique index.
- In the UI, a message's key is made from the same pair, so the bubble stays in place as it changes from pending to confirmed, rather than being removed and added again.

### The echo confirms the pending message in place
On send, the repository inserts a `Pending` row so the message shows at once. The server returns its stored copy as the send response and echoes it on the incoming stream, both with the `clientMessageId` it was sent with. Whichever arrives first confirms the message: one upsert by `(senderId, clientMessageId)` updates the pending row in place with the server's `serverMessageId`, `seq` and `timestampMillis`. It is a single write, so Room notifies observers once, and the list never shows both copies or neither.

The second copy upserts the same values, so it changes nothing. A confirmed message never changes after the server stores it (see `decisions.md`), so a repeated or late copy cannot overwrite newer state.

### Full send flow
```
User taps send
    → SendMessageUseCase builds Message.Pending(clientMessageId, timestampMillis = device time)
    → Repository inserts the pending row into Room
    → UI shows the message at the end of the list with the SENDING indicator
    → Repository calls RemoteDataSource.sendMessage(dto)   ← returns Result<MessageDto>
    → The server (the mock) stores it, gives it a serverMessageId, the Chat's next seq and a timestamp,
      and returns and echoes it with the clientMessageId after ~250ms
    → Repository, on the first copy: upsert by (senderId, clientMessageId), updating the pending row in place
    → Room invalidates the paged query → ChatItemMapper → UI shows it as SENT, in its server position

On failure (future):
    → Repository marks the pending message FAILED
    → UI renders a retry affordance
    → A retry resends the same clientMessageId, so the server cannot store it twice
```

### Incoming messages
```
RemoteDataSource.incomingMessages() emits MessageDto
    → Upsert by (senderId, clientMessageId)
    → If it is the echo of a pending message this device sent, that row is confirmed in place
    → Otherwise it is inserted as a confirmed message
    → Room invalidates the paged query → UI
```
History pages go through the same upsert, so a message that arrives both in a page and on the stream is stored once.

### Ordering
Confirmed messages are ordered by their `seq`: the server gives each message of a Chat the next number, starting at 1, with no holes. The `serverMessageId` is an opaque UUID and says nothing about order, and neither does the `clientMessageId`. Pending messages have no `seq`. They sort after every confirmed message, by device timestamp. `timestampMillis` is used only for display and grouping, never for order. See [ADR-0003](0003-cursor-paging.md) for paging and gap handling.

The device never assigns `seq`. If it did, a device clock or a second device could place messages out of order.

### Error handling seam
`ChatRemoteDataSource.sendMessage()` returns `Result<MessageDto>`, not `Unit`. The failure branch exists in the repository with a `// TODO` marking where failure and retry are wired in.

## Alternatives
- **Delete the pending row and insert the confirmed one**, matched by an id only the sender knew (the previous version): messages from other devices had no such id, so the UI key fell back to the server's id, and confirming took two writes in a transaction.
- **`clientMessageId` alone as the key**, with the server rejecting a reuse by another sender: one column instead of two, but the client's key would depend on a server rule it cannot check, and a server that did not enforce it would let one sender's message overwrite another's in Room.

## Consequences
- Room is the single source of truth, and the UI reads only from the local database.
- The server owns each message's `serverMessageId`, order and timestamp. A device clock that is wrong cannot reorder the conversation.
- Every message has the same key from pending to confirmed, on every device, so the UI never needs a fallback key.
- Confirming is a single upsert, and a repeated echo is harmless by construction.
- `MessageDirection` is derived at mapping time from `senderId`, never from the code path.
- `MockChatRemoteDataSource` can be replaced by a real `WebSocketChatDataSource` with no changes above the repository.
- A sent message appears at once as SENDING, then moves to its server position as SENT after ~250ms. This intentionally mirrors real behaviour.
- The delivery status follows from the message type: `Pending` is `SENDING` and `Confirmed` is `SENT`. Failure handling adds a state to `Pending` without changing the structure.
