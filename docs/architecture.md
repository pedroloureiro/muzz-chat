# Architecture

The rules every change must follow. The reasoning is in [ADR-0002](adr/0002-chat-domain-layer.md), the message flow in [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md), paging in [ADR-0003](adr/0003-cursor-paging.md), dependencies between features in [ADR-0004](adr/0004-cross-feature-dependencies.md), what the app assumes of the backend in [`api.md`](api.md), and vocabulary in [`CONTEXT.md`](../CONTEXT.md).

---

## Overview

The app follows Clean Architecture inside each feature, using packages in a single Gradle module. There are two features: `chat`, and `user`, which gives `chat` the current user. A feature may use another feature's `domain` only, and features depend one way, so `chat` imports `user/domain` and `user` imports nothing from `chat`.

```text
ui ──> domain <── data
di ──> ui, domain, data
```

| Layer | Responsibility | May depend on |
|---|---|---|
| `domain` | Business models, repository contracts, use cases | Kotlin, `kotlinx.coroutines`, `paging-common`, `java.util` |
| `data` | Implements `domain` contracts using Room, Paging and the remote service | `domain`, Android, Room, Paging, serialization |
| `ui` | Compose screens, ViewModel, UI state, display rules | `domain`, Android, Compose, Paging, top-level `ui` |
| `di` | Creates objects and wires the layers together | All layers |
| `utils` (top-level) | Static utility functions that know nothing about the app. Any layer may use them | Kotlin, the JDK |

Any layer may also use the `domain` of a feature its own feature depends on, following rule 5.

---

## Package structure

```text
com.pedroloureiro.muzzchat
├── MainActivity (and MOCK_SARAH_CHAT_ID, MOCK_SARAH_NAME, MOCK_SARAH_USER_ID), MuzzChatApplication
├── ui
│   ├── theme                 Colours, typography, spacing, dimens, shapes, alpha, scale, theme
│   └── components            Generic composables: Button, TextInput, TopBar, Avatar
├── utils                     Static utility functions: DateTimeUtils
├── user
│   ├── di                    UserModule
│   ├── domain                UserRepository (interface), User
│   └── data                  UserRepositoryImpl
│       └── remote            UserRemoteDataSource, MockUserRemoteDataSource, UserDto
└── chat
    ├── di                    ChatModule
    ├── domain                ChatRepository (interface), SendMessageUseCase, OtherUserDevice (interface)
    │   └── models            Message (Pending, Confirmed), MessageWithNext, Content, ContentType,
    │                         MessageDeliveryStatus
    ├── data                  ChatRepositoryImpl, MessageMappers, MessageRemoteMediator, MockChatApi
    │   ├── local             ChatLocalDataSource, RoomChatLocalDataSource
    │   │   └── db            MessageDatabase, Converters, MessageDao
    │   │       └── entities  MessageEntity, ChatSyncEntity, MessageWithNextRow
    │   └── remote            ChatRemoteDataSource, MockChatRemoteDataSource
    │       └── models        MessageDto, MessagePageDto, ContentDto, OutgoingMessageDto
    └── ui                    ChatScreen, ChatViewModel, ChatItemMapper
        ├── models            ChatItem, ChatUiState, MessageDirection
        └── components        MessageBubble, MessageItem, SendingIndicator, SectionHeader, StartOfChatMarker, ChatInput,
                              MockChatInput
```

A new feature gets its own package beside `chat`, with the layers it needs. Code moves to the top level only when it knows nothing about any feature. Inside a layer, packages follow rule 23.

---

## Rules

### Dependencies
1. A feature's `domain` imports no Android, Room or serialization code, and nothing from its own feature's `data`, `ui` or `di`. The one `androidx` package it may import is `androidx.paging`, from the plain Kotlin `paging-common` library.
2. A feature's `ui` imports nothing from its own feature's `data`.
3. A feature's `data` imports nothing from its own feature's `ui`.
4. Only a feature's `di` may see every layer of that feature. The top-level `ui` and `utils` packages import nothing from any feature.
5. A feature may import another feature's `domain`, and never its `data`, `ui` or `di`. Features depend one way: `chat` imports `user/domain`, and `user` imports nothing from `chat`. See [ADR-0004](adr/0004-cross-feature-dependencies.md).

### Data
6. `MessageEntity` and `MessageDto` stay inside `chat/data`. `ChatRepository` exposes only domain models.
7. `ChatRepositoryImpl` is the single ingestion point for all messages.
8. Room is the source of truth. The UI observes messages through `ChatRepository`, and message state is never duplicated.
9. Nothing outside `chat/data/local` references `MessageDao` or `MessageDatabase`, except `chat/di` to wire them.

### Use cases
10. Add a use case only when an operation has logic of its own, such as validation or building a domain object.
11. `ChatViewModel` calls `ChatRepository` directly for operations with no logic. Forwarding-only use cases are not added.
12. Use cases read the clock and generate IDs directly. Inject them only when logic depends on their values.

### Presentation
13. Composables are stateless and receive only what they need to render. A composable that reacts to press or focus receives a `MutableInteractionSource` from its caller, and the screen remembers it.
14. Display rules, such as grouping and timestamp headers, belong to `ChatItemMapper`.
15. `ChatItemMapper` has no Android dependencies, so it can run on the JVM.

### Dependency injection
16. Koin wiring lives in each feature's `di`. Classes receive dependencies through their constructors and never call Koin themselves.
17. Android types stay out of a feature's `domain` contracts. A `data` class that needs `Context` receives it through Koin.
18. Long-running work in `chat/data` is a `suspend` function whose caller owns its lifetime through a scope. Nothing uses `GlobalScope`.

### Ordering and paging
19. Every message belongs to one Chat, and every remote call and Room query is limited to one `chatId`. Confirmed messages are ordered by their server-assigned `seq`, and pending messages after them by device timestamp. `timestampMillis` is used only for display and grouping.
20. A message is identified by its `senderId` and `clientMessageId` together: the Room primary key and the UI key. An echo confirms its pending message in place, by one upsert on that key. Every write of server messages, whether a history page or an incoming message, goes through the same insert, in the same transaction as the Chat's sync state it moves.
21. The message list is paged from Room. Display rules look at one message and one neighbour, so they work on `PagingData` through `map` and `insertSeparators`. Data a rule needs from a neighbour that `map` cannot see is supplied by the paged query, as `MessageWithNext`.

### Text
22. Text shown to the user, including content descriptions, comes from `res/values/strings.xml` through `stringResource`. Names and other data go in as format arguments, such as `reply_as` = "Reply as %1$s". `@Preview` functions may use literal sample text.

### Packages
23. Files of one type, such as models, entities or components, get a package of their own when there is more than one of them, named for the type in the plural: `models`, `entities`, `components`. A file that is the only one of its type sits in its parent package. An interface and its implementations count as one file. Packages that group by role, not by type, are kept whatever their size: the layers (`domain`, `data`, `ui`, `di`, `utils`), `theme`, and `local`, `remote` and `db` in `data`.

---

## Sending a message

```text
ChatViewModel
  → SendMessageUseCase          trims and validates text, builds Message.Pending(current user, clientMessageId, device time)
  → ChatRepository.sendMessage
  → ChatRepositoryImpl          inserts the pending row into Room, then calls the remote data source
  → Echo or response received   upsert by (senderId, clientMessageId): the pending row is confirmed in place
  → Room PagingSource           invalidated, reloads PagingData<MessageWithNext>
  → ChatItemMapper              map → ChatItem.Message, insertSeparators → TimestampHeader, then StartOfChat
  → Flow<PagingData<ChatItem>>  rendered by ChatScreen
```

---

## Loading history

```text
ChatScreen scrolls past the oldest stored message
  → MessageRemoteMediator APPEND   fetchMessagesBefore(chatId, beforeSeq = sync state's oldestSeq, limit)
  → ChatRepositoryImpl             one transaction: write the page, set oldestSeq = nextCursor, reachedStart = !hasMore
  → Room PagingSource              invalidated, loads the older rows
  → ChatItemMapper                 same rules as above
```

Opening the Chat runs a REFRESH. With no sync state it fetches the newest page. Otherwise it catches up: it fetches pages after the sync state's `newestSeq` until `hasMore` is false. An incoming message that skips a `seq` is stored and starts the same catch-up. History is complete when the server reports `hasMore = false` for older messages.

---

## Testing

| Code | Approach |
|---|---|
| Classes with logic: use cases, `ChatItemMapper`, `ChatViewModel`, `ChatRepositoryImpl`, `UserRepositoryImpl`, `MessageRemoteMediator`, `Converters` | Unit tests, written first. `ChatItemMapper`'s rules are tested directly and its paging wiring with `asSnapshot()` |
| DAO queries, including the paged `MessageWithNext` query | `MessageDaoTest`, using Robolectric and an in-memory Room database, so no emulator is needed |
| Forwarding code | Not tested directly |
| Composables | Not tested |
| Dependency rules 1–5 | `ArchitectureTest` scans the imports of the Kotlin files under every feature, `ui` and `utils`. It lists the features and which others each depends on, and fails when a feature package is missing from that list |

---

## Enforcement

`ArchitectureTest` fails the test run when a dependency rule is broken. Because the layers are packages, a bad import still compiles until the test runs. The other rules rely on code review.

---

## Accepted deviations

These are the places where the app knowingly departs from strict Clean Architecture.

| # | Deviation | Why we accept it | How to correct it |
|---|---|---|---|
| 1 | `ChatViewModel` calls `ChatRepository` directly to observe messages, with no use case in between | That use case would only forward the call | Add a use case when an operation gains logic, or add all of them if a uniform rule becomes worth the code |
| 2 | Layers are packages in one Gradle module, so boundaries are checked by a test and not the compiler | With two small features, separate modules would add build setup without enough benefit | Split into `:feature:chat:domain`, `:feature:chat:data` and `:feature:chat:presentation`, with `domain` as a plain Kotlin module and `:app` as the wiring point |
| 3 | `chat/domain` has `MessageWithNext`, a row model shaped by the paged list's needs | Paging maps one row at a time, and the bubble tail needs the next message. See [ADR-0003](adr/0003-cursor-paging.md) | Move the type into `chat/data` and map it to a UI-facing row in the ViewModel, if the domain should stay free of display concerns |
| 4 | `MessageEntity` stores the domain `ContentType` enum by name, so renaming a value changes what is stored | A separate enum for storage would duplicate it for little benefit | Add a storage-only enum or stable codes, and map them in `chat/data` |
| 5 | `chat/domain` uses `Flow` from `kotlinx.coroutines` and `PagingData` from `paging-common` | Neither has an Android dependency, and they are how the UI observes and pages Room | Define domain stream and paging abstractions, which is not worth doing unless the libraries must go |
| 6 | The open Chat's id, Sarah's name and Sarah's user id are `MOCK_SARAH_CHAT_ID`, `MOCK_SARAH_NAME` and `MOCK_SARAH_USER_ID`, private constants in `MainActivity.kt` marked as mock data. `MainActivity`, the app's entry point, passes them to `ChatScreen` as its `chatId`, `title` and `otherUserId` parameters, where navigation arguments would come from. `ChatScreen` shows the `title` in `TopBar` and on the "Reply as" button, and passes `chatId` and `otherUserId` to `ChatViewModel` through `parametersOf` | The app has one screen and no chat list to navigate from, so it has no navigation library | Add navigation, open `ChatScreen` from a chat list with the selected Chat's id and title as navigation arguments, and delete the constants. `otherUserId` goes with deviation 7 |
| 7 | `ChatScreen` shows `MockChatInput`, marked as mock data, which adds a "Reply as Sarah" button after Send in the real `ChatInput`. It calls `ChatViewModel.sendAsOther`, which sends the input as `otherUserId` through `OtherUserDevice`, a mock domain contract that `MockChatRemoteDataSource` implements: it stores the message as Sarah's through `MockChatApi.sendAs`, the only send that names its sender, and pushes it on the incoming stream, so this device never writes it | Sarah has no device in the exercise, so her messages are sent from this one | Show `ChatInput` alone, and delete `MockChatInput`, `sendAsOther`, `otherUserId`, `OtherUserDevice` and `MockChatApi.sendAs`, once other users' messages arrive from the server |
