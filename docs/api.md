# API assumptions

What the app expects of the backend, which the exercise does not have. Every guarantee below is relied on by the client, so a real backend must give it, or the client must change. The reasons are in the linked ADRs and decisions, and the vocabulary in [`CONTEXT.md`](../CONTEXT.md). The last section explains how the mock backend plays this role.

---

## Identity

- **Every request is authenticated.** Each call, and the incoming message stream, carries the signed-in user's token, for example a JWT in the `Authorization` header. The server knows who is calling from the token alone.
- **The server sets the sender from the token.** An outgoing message has no `senderId`. The server fills it in from the authenticated request and never from the message, so a device can send only as its own user. See [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md).
- **`me()` returns the signed-in user**, with the same `id` the server puts in `senderId` on that user's messages. The client compares the two to tell outgoing from incoming. See "The current user is not cached" in [`decisions.md`](decisions.md).

## Sending a message

- **`sendMessage(chatId, OutgoingMessageDto)`** sends `clientMessageId`, `content` and the device's `timestampMillis`, and returns the stored `MessageDto`.
- **The server owns the stored message.** It assigns a random UUID `serverMessageId`, the Chat's next `seq` and its own `timestampMillis`, which replaces the device's.
- **Sends are idempotent.** `(senderId, clientMessageId)` is unique on the server. A resend of the same pair returns the message already stored, with the same `seq`, and stores nothing new. The same `clientMessageId` from another sender is a different message. See [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md).
- **Stored messages never change.** There are no edits or deletes, so a repeated or late copy of a message can't overwrite newer state. See "Messages cannot be edited or deleted" in [`decisions.md`](decisions.md).

## Ordering

- **Each Chat numbers its messages by `seq`**, starting at 1 with no holes, assigned in the same transaction that stores the message. `seq` is the only order of confirmed messages and the paging cursor. A missing `seq` therefore means a missed message. See "The server orders messages by a per-Chat sequence number" in [`decisions.md`](decisions.md).
- **Timestamps are epoch millis**, so always UTC, and are used only for display and grouping, never for order. See "Message timestamps are epoch millis" in [`decisions.md`](decisions.md).

## Incoming messages

- **One stream for every Chat**, `incomingMessages()`, a WebSocket in production. It carries every message stored in any Chat the user belongs to, including the sender's own messages, as the echo that confirms them.
- **Every copy carries `clientMessageId`**, whoever sent it, so each message has the same key from the moment it is sent.
- **Delivery order does not matter, and the stream may miss messages.** Room orders by `seq`, and a message that skips a `seq` starts a catch-up through the history calls. See [ADR-0003](adr/0003-cursor-paging.md).

## History

- **`fetchMessagesBefore(chatId, beforeSeq, limit)`** returns up to `limit` messages older than `beforeSeq`, newest first. With no `beforeSeq`, it returns the newest page.
- **`fetchMessagesAfter(chatId, afterSeq, limit)`** returns up to `limit` messages newer than `afterSeq`, oldest first. The catch-up calls it until `hasMore` is false.
- **Each page is a `MessagePageDto`**: the `messages`, a `nextCursor` (the `seq` of the last message in the page, the cursor for the next call in the same direction) and `hasMore`. `hasMore = false` on an older page means the start of the Chat. See [ADR-0003](adr/0003-cursor-paging.md).
- **Content** is a `type` name, only `TEXT` for now, and a `value`.

## Left out

The client assumes the backend is always reachable and always succeeds. The mock never fails and answers after a fixed ~200ms delay, so the paths below would never run:

- No network errors, timeouts, 4xx or 5xx responses. A failed send would stay `SENDING`, and nothing retries it. See "Sends are not retried and cannot fail" in [`decisions.md`](decisions.md).
- No loading, error or connection state in the UI. See "The UI shows no loading or error state" in [`decisions.md`](decisions.md).
- No reconnection or catch-up after the stream drops. Opening a Chat catches up, and so does a message that skips a `seq`.
- No sign-in, token refresh or sign-out, and no expired token handling.
- No rate limits, payload size limits or content types beyond `TEXT`.

---

## The mock backend

Everything here is marked `MOCK DATA` in the code and is replaced by a real backend.

### Chat: `MockChatApi` and `MockChatRemoteDataSource`

`MockChatApi` in `chat/data` plays the server. `MockChatRemoteDataSource` in `chat/data/remote` implements `ChatRemoteDataSource` on top of it, adds a ~200ms delay to every call, and pushes each stored message on a `MutableSharedFlow` that stands in for the WebSocket.

- **Seed**: `app/src/main/assets/mock_messages.json`, a JSON array of `MessageDto`. It holds 200 messages in one Chat, `00000000-0000-0000-0000-000000000c01`, with `seq` 1 to 200, between the current user (`…0001`) and Sarah (`…0002`). It is read from the APK's assets every time messages are listed, and never written.
- **Sent messages**: `mock_sent_messages.json` in the app's files directory (`Context.filesDir`), a JSON array of `MessageDto`. It is read once when `MockChatApi` is created, and rewritten in full after every new message, so the mock never reuses a `seq` that Room already stores. It is deleted only with the app's data. See "The mock backend keeps sent messages in a file" in [`decisions.md`](decisions.md).
- **Listing**: the seed, then the sent messages, filtered to one Chat and sliced by `seq` into pages.
- **Storing**: a new message gets a random UUID `serverMessageId`, the highest `seq` in its Chat plus one, and the current time. A resend of a stored `(senderId, clientMessageId)` returns the stored message.
- **Sender**: `send(chatId, dto)` stores the message as the current user, read from `UserRepository`, in place of the token. `sendAs(chatId, senderId, dto)` stores it as anyone, only for `OtherUserDevice`, which sends Sarah's messages from this device. A real API has no `sendAs`, and it goes when `OtherUserDevice` does.

### User: `MockUserRemoteDataSource`

`MockUserRemoteDataSource` in `user/data/remote` answers `me()` at once with the fixed current user, `00000000-0000-0000-0000-000000000001`, named "You". It is the only code that knows this id, apart from the seed file, whose messages use it.

### Starting over

Nothing resets the mock. Clearing the app's data deletes `mock_sent_messages.json` and Room together, and the app starts again from the seed. See "The chat has no clear or reset action" in [`decisions.md`](decisions.md).
