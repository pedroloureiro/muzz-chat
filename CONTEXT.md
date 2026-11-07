# Muzz Chat — Domain Glossary

## User
A participant in the conversation. Two fixed users exist: `ME` (the local user) and `SARAH` (the other participant). Each has a stable UUID and a display name. No authentication exists — users are hardcoded constants in `chat/domain/model`.

## Message
A unit of communication between two Users. A sealed class with two kinds, `Message.Pending` and `Message.Confirmed`. Both have a `senderId` (UUID), a `Content` and a `timestampMillis` (epoch millis, so always UTC). Has no edit flag and no delete flag — these are deliberate omissions for this scope. A domain model, stored as `MessageEntity` in one Room table in `chat/data`. See [ADR-0001](docs/adr/0001-repository-dual-datasource-echo-pattern.md).

## Pending Message
A `Message.Pending`: a message this device has sent that the server has not confirmed yet. Identified by its `localId`. Its `timestampMillis` is the device time. It has no server `id` and no `sequenceId`, so it sorts after every confirmed message. Shown as `SENDING`.

## Confirmed Message
A `Message.Confirmed`: a message the server has stored. Has the server's `id`, `sequenceId` and `timestampMillis`. When this device sent it, it also keeps the `localId` it was sent with. Shown as `SENT`.

## localId
A UUID generated on the device for each message it sends. It matches the server's echo to the pending message it replaces, keeps the message's key stable in the UI, and lets a resend be recognised by the server instead of stored twice.

## sequenceId
The server-assigned position of a confirmed message in the conversation. It starts at 1 and increases by one with no gaps. It alone decides the order of confirmed messages, and it is the paging cursor. `timestampMillis` is never used for order.

## Echo
The server's stored copy of a message this device sent, returned with its `localId`. In one Room transaction, the repository deletes the pending message with that `localId` and inserts the confirmed one.

## MessageWithNext
A row of the paged message list: a `Message` together with the `senderId` and `timestampMillis` of the next message in conversation order, or nulls when there is none. Computed by the paged Room query and never stored. It exists because the bubble tail depends on the next message, and paging maps one row at a time.

## Content
The payload of a Message. Has a `ContentType` (currently `TEXT` only) and a `value` (String). Modelled as a data class to allow future content types without changing the Message schema.

## MessageDirection
Whether a rendered message is `OUTGOING` (sent by ME) or `INCOMING` (sent by SARAH). A UI-layer concept derived from comparing `senderId` to `Users.ME.id`. Not to be confused with delivery status.

## ChatItem
A UI-layer model representing one renderable row in the message list. Either a `Message` (with `key`, `direction`, `content: String`, `timestampMillis`, `isGrouped`, `deliveryStatus`) or a `TimestampHeader` (with a pre-formatted `label` and a `key`). A message's `key` is its `localId` when it has one, and otherwise its server `id`. A header's `key` is derived from the message below it. Built by `ChatItemMapper`.

## TimestampHeader
A `ChatItem` variant rendered as a centred date/time divider. Inserted above a message when the gap from the older message exceeds one hour, or when it is the oldest message in the conversation. The format depends on how many calendar days ago the message was sent, in the device's time zone: today `11:59`, 1–6 days ago `Thursday 11:59`, otherwise `12 Mar 2026 11:59`.

## Grouped Message
A `ChatItem.Message` where `isGrouped = true`. Occurs when the next message is sent by the same User AND within 20 seconds. Rendered with no tail on the bubble and the small gap below it, because the next bubble continues the run.

## ChatItemMapper
A pure Kotlin object (no Android dependencies) holding the display rules, each looking at one message and one neighbour. `toMessageItem` builds a `ChatItem.Message` and computes `isGrouped` from the next message. `headerAbove` returns a `TimestampHeader` when one belongs above a message, choosing its `DateTimeFormat`. Paging applies them with `map` and `insertSeparators`. Date arithmetic and formatting are done by `calendarDaysBetween` and `formatUtc` in `utils`. The primary unit under TDD.

## ChatUiState
The state object the ViewModel exposes to the UI via `StateFlow`. Contains `inputText: String`. The message list is exposed separately as `Flow<PagingData<ChatItem>>`, because paged data is a stream. No loading or error state — deliberate trade-off documented in decisions log.

## MessageDeliveryStatus
The delivery state of a message, as shown in the UI. Follows from the message type: `Pending` is `SENDING` and `Confirmed` is `SENT`. `FAILED` and `RETRYING` are reserved for future error handling, and will be states of a pending message.

## ChatRepository
The domain interface for working with messages. Exposes `getMessages(): Flow<PagingData<MessageWithNext>>`, `syncIncomingMessages()`, `sendMessage(message: Message.Pending)`, `loadMessages()`, and `clearMessages()`. `syncIncomingMessages()` stores messages from `ChatRemoteDataSource` until its caller cancels it. `loadMessages()` refreshes the history from the server. `ChatRepositoryImpl` in `chat/data` implements it and is the single ingestion point for all messages.

## SendMessageUseCase
Trims and validates message text, ignores blank input, and builds a `Message.Pending` with a new `localId` and the device time before passing it to `ChatRepository`. The only use case, because the other operations have no logic of their own.

## ChatRemoteDataSource
The interface representing the remote messaging service. Exposes `sendMessage(dto): Result<MessageDto>` for outgoing messages, `fetchMessages(beforeSequenceId: Long?, limit: Int): Result<List<MessageDto>>` for history pages (newest first), and `incomingMessages(): Flow<MessageDto>` for the incoming stream (WebSocket in production). The order it delivers messages in does not matter, because Room orders them. `MockChatRemoteDataSource` implements this using `MockData`, echoing sent messages back on `incomingMessages()` after a ~200ms simulated delay.

## ChatLocalDataSource
The interface over Room, and the single source of truth for the UI. Stores pending and confirmed messages in one table, replaces a pending message with its echo in one transaction, and exposes the paged `MessageWithNext` query, newest first: confirmed messages by `sequenceId`, then pending messages by device timestamp.

## MessageRemoteMediator
The Paging 3 `RemoteMediator` in `chat/data`. When the user scrolls past the stored history, it fetches the page before the smallest stored `sequenceId` and writes it into Room. History is complete once `sequenceId` 1 is stored. See [ADR-0003](docs/adr/0003-cursor-paging.md).

## MockData
The fake server's messages, in `chat/data/mock/`. Holds a pre-written conversation from `assets/mock_messages.json`, large enough to fill several pages, plus any messages sent since the app started. It assigns each sent message the next `sequenceId`, a server `id` and the server time, and returns history pages by `sequenceId`. Only `MockChatRemoteDataSource` uses it. The "Load sample data" overflow menu action reaches it through `ChatRepository.loadMessages()`.
