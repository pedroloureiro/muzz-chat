# Muzz Chat — Domain Glossary

## User
A participant in the conversation. Two fixed users exist: `ME` (the local user) and `SARAH` (the other participant). Each has a stable UUID and a display name. No authentication exists — users are hardcoded constants in `chat/domain/model`.

## Message
A unit of communication between two Users. Contains a unique UUID (`messageId`), a `senderId` (UUID), a `Content`, a `timestamp` (epoch millis), and a `MessageDeliveryStatus`. Has no edit flag and no delete flag — these are deliberate omissions for this scope. A domain model, stored as `MessageEntity` in `chat/data`.

## Content
The payload of a Message. Has a `ContentType` (currently `TEXT` only) and a `value` (String). Modelled as a data class to allow future content types without changing the Message schema.

## MessageDirection
Whether a rendered message is `OUTGOING` (sent by ME) or `INCOMING` (sent by SARAH). A UI-layer concept derived from comparing `senderId` to `Users.ME.id`. Not to be confused with delivery status.

## ChatItem
A UI-layer model representing one renderable row in the message list. Either a `Message` (with `messageId`, `direction`, `content: String`, `timestamp`, `isGrouped`, `deliveryStatus`) or a `TimestampHeader` (with a pre-formatted `label`). Built by `ChatItemMapper`.

## TimestampHeader
A `ChatItem` variant rendered as a centred date/time divider. Inserted before a message when the gap from the previous message exceeds one hour, or when it is the first message in the list. Format: `{day} {HH:mm}` e.g. `Thursday 11:59`.

## Grouped Message
A `ChatItem.Message` where `isGrouped = true`. Occurs when the following message is sent by the same User AND within 20 seconds. Rendered with reduced top spacing and no tail on the bubble.

## ChatItemMapper
A pure Kotlin class (no Android dependencies) responsible for transforming `List<Message>` into `List<ChatItem>`. Inserts `TimestampHeader` items and computes `isGrouped` flags. The primary unit under TDD.

## ChatUiState
The single state object the ViewModel exposes to the UI via `StateFlow`. Contains `messages: List<ChatItem>` and `inputText: String`. No loading or error state — deliberate trade-off documented in decisions log.

## MessageDeliveryStatus
The delivery state of an outgoing Message. `SENDING` means the message has been optimistically inserted into Room and the server echo is pending. `SENT` means the echo was received and the message is confirmed. `FAILED` and `RETRYING` are reserved for future error handling and retry logic. Only applies to outgoing messages — incoming messages from SARAH are always inserted with `SENT`.

## ChatRepository
The domain interface for working with messages. Exposes `getMessages(): Flow<List<Message>>`, `sendMessage(message)`, `loadSampleData()`, and `clearMessages()`. `ChatRepositoryImpl` in `chat/data` implements it and is the single ingestion point for all messages.

## SendMessageUseCase
Trims and validates message text, ignores blank input, and builds a `Message` with status `SENDING` before passing it to `ChatRepository`. The only use case, because the other operations have no logic of their own.

## ChatRemoteDataSource
The interface representing the remote messaging service. Exposes `sendMessage(dto): Result<MessageDto>` for outgoing messages and `incomingMessages(): Flow<MessageDto>` for the incoming stream (WebSocket in production). `MockChatRemoteDataSource` implements this by echoing sent messages back on `incomingMessages()` after a ~200ms simulated delay.

## ChatLocalDataSource
The interface over Room. Exposes `insertMessage(entity)`, `updateStatus(messageId, status)`, `getMessages(): Flow<List<MessageEntity>>`, and `clearAll()`. `getMessages()` emits the newest 500 messages in ascending timestamp order. The single source of truth for the UI.

## MockData
The seed content loader in `chat/data/mock/`. Reads a pre-written conversation from `assets/mock_messages.json`, used by the "Load sample data" overflow menu action.
