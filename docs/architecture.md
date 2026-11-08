# Architecture

The rules every change must follow. The reasoning is in [ADR-0002](adr/0002-chat-domain-layer.md), the message flow in [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md), paging in [ADR-0003](adr/0003-cursor-paging.md), and vocabulary in [`CONTEXT.md`](../CONTEXT.md).

---

## Overview

The app follows Clean Architecture inside each feature, using packages in a single Gradle module. Chat is the only feature.

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

---

## Package structure

```text
com.pedroloureiro.muzzchat
├── MainActivity (and MOCK_SARAH_CHAT_ID), MuzzChatApplication
├── ui
│   ├── theme                 Colours, typography, spacing, dimens, shapes, alpha, scale, theme
│   └── components            Generic composables: Button, TextInput, TopBar, Avatar
├── utils                     Static utility functions: DateTimeUtils
└── chat
    ├── di                    ChatModule
    ├── domain                ChatRepository (interface), SendMessageUseCase
    │   └── models            Message (Pending, Confirmed), MessageWithNext, Content, ContentType,
    │                         User + Users, MessageDeliveryStatus
    ├── data                  ChatRepositoryImpl, MessageMappers, MessageRemoteMediator, MockData
    │   ├── local             ChatLocalDataSource, RoomChatLocalDataSource
    │   │   └── db            MessageDatabase, Converters, MessageDao
    │   │       └── entities  MessageEntity, ChatSyncEntity, MessageWithNextRow
    │   └── remote            ChatRemoteDataSource, MockChatRemoteDataSource
    │       └── models        MessageDto, MessagePageDto, ContentDto, OutgoingMessageDto
    └── ui                    ChatScreen, ChatViewModel, ChatItemMapper
        ├── models            ChatItem, ChatUiState, MessageDirection
        └── components        MessageBubble, MessageItem, SendingIndicator, SectionHeader, ChatInput
```

A new feature gets its own package beside `chat`, with the same layers. Code moves to the top level only when it knows nothing about any feature. Inside a layer, packages follow rule 22.

---

## Rules

### Dependencies
1. `chat/domain` imports no Android, Room, serialization, `chat/data` or `chat/ui` code. The one `androidx` package it may import is `androidx.paging`, from the plain Kotlin `paging-common` library.
2. `chat/ui` imports nothing from `chat/data`.
3. `chat/data` imports nothing from `chat/ui`.
4. Only `chat/di` may see every layer. The top-level `ui` and `utils` packages import nothing from `chat`.

### Data
5. `MessageEntity` and `MessageDto` stay inside `chat/data`. `ChatRepository` exposes only domain models.
6. `ChatRepositoryImpl` is the single ingestion point for all messages.
7. Room is the source of truth. The UI observes messages through `ChatRepository`, and message state is never duplicated.
8. Nothing outside `chat/data/local` references `MessageDao` or `MessageDatabase`, except `chat/di` to wire them.

### Use cases
9. Add a use case only when an operation has logic of its own, such as validation or building a domain object.
10. `ChatViewModel` calls `ChatRepository` directly for operations with no logic. Forwarding-only use cases are not added.
11. Use cases read the clock and generate IDs directly. Inject them only when logic depends on their values.

### Presentation
12. Composables are stateless and receive only what they need to render. A composable that reacts to press or focus receives a `MutableInteractionSource` from its caller, and the screen remembers it.
13. Display rules, such as grouping and timestamp headers, belong to `ChatItemMapper`.
14. `ChatItemMapper` has no Android dependencies, so it can run on the JVM.

### Dependency injection
15. Koin wiring lives in `chat/di`. Classes receive dependencies through their constructors and never call Koin themselves.
16. Android types stay out of `chat/domain` contracts. A `data` class that needs `Context` receives it through Koin.
17. Long-running work in `chat/data` is a `suspend` function whose caller owns its lifetime through a scope. Nothing uses `GlobalScope`.

### Ordering and paging
18. Every message belongs to one Chat, and every remote call and Room query is limited to one `chatId`. Confirmed messages are ordered by their server-assigned `seq`, and pending messages after them by device timestamp. `timestampMillis` is used only for display and grouping.
19. A message is identified by its `senderId` and `clientMessageId` together: the Room primary key and the UI key. An echo confirms its pending message in place, by one upsert on that key. Every write of server messages, whether a history page or an incoming message, goes through the same insert, in the same transaction as the Chat's sync state it moves.
20. The message list is paged from Room. Display rules look at one message and one neighbour, so they work on `PagingData` through `map` and `insertSeparators`. Data a rule needs from a neighbour that `map` cannot see is supplied by the paged query, as `MessageWithNext`.

### Text
21. Text shown to the user, including content descriptions, comes from `res/values/strings.xml` through `stringResource`. Names and other data go in as format arguments, such as `reply_as` = "Reply as %1$s". `@Preview` functions may use literal sample text.

### Packages
22. Files of one type, such as models, entities or components, get a package of their own when there is more than one of them, named for the type in the plural: `models`, `entities`, `components`. A file that is the only one of its type sits in its parent package. An interface and its implementations count as one file. Packages that group by role, not by type, are kept whatever their size: the layers (`domain`, `data`, `ui`, `di`, `utils`), `theme`, and `local`, `remote` and `db` in `data`.

---

## Sending a message

```text
ChatViewModel
  → SendMessageUseCase          trims and validates text, builds Message.Pending(clientMessageId, device time)
  → ChatRepository.sendMessage
  → ChatRepositoryImpl          inserts the pending row into Room, then calls the remote data source
  → Echo or response received   upsert by (senderId, clientMessageId): the pending row is confirmed in place
  → Room PagingSource           invalidated, reloads PagingData<MessageWithNext>
  → ChatItemMapper              map → ChatItem.Message, insertSeparators → TimestampHeader
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
| Classes with logic: use cases, `ChatItemMapper`, `ChatViewModel`, `ChatRepositoryImpl`, `MessageRemoteMediator`, `Converters` | Unit tests, written first. `ChatItemMapper`'s rules are tested directly and its paging wiring with `asSnapshot()` |
| DAO queries, including the paged `MessageWithNext` query | `MessageDaoTest`, using Robolectric and an in-memory Room database, so no emulator is needed |
| Forwarding code | Not tested directly |
| Composables | Not tested |
| Dependency rules 1–4 | `ArchitectureTest` scans the imports of the Kotlin files under `chat`, `ui` and `utils` |

---

## Enforcement

`ArchitectureTest` fails the test run when a dependency rule is broken. Because the layers are packages, a bad import still compiles until the test runs. The other rules rely on code review.

---

## Accepted deviations

These are the places where the app knowingly departs from strict Clean Architecture.

| # | Deviation | Why we accept it | How to correct it |
|---|---|---|---|
| 1 | `ChatViewModel` calls `ChatRepository` directly to observe messages, with no use case in between | That use case would only forward the call | Add a use case when an operation gains logic, or add all of them if a uniform rule becomes worth the code |
| 2 | Layers are packages in one Gradle module, so boundaries are checked by a test and not the compiler | With one feature, separate modules would add build setup without enough benefit | Split into `:feature:chat:domain`, `:feature:chat:data` and `:feature:chat:presentation`, with `domain` as a plain Kotlin module and `:app` as the wiring point |
| 3 | `Users.ME` is a global constant, so `ChatItemMapper` has the current user hardcoded | The app has no authentication and its users are fixed by design | Add a `CurrentUser` abstraction when login exists, and inject it into the mapper |
| 4 | `chat/domain` has `MessageWithNext`, a row model shaped by the paged list's needs | Paging maps one row at a time, and the bubble tail needs the next message. See [ADR-0003](adr/0003-cursor-paging.md) | Move the type into `chat/data` and map it to a UI-facing row in the ViewModel, if the domain should stay free of display concerns |
| 5 | `MessageEntity` stores the domain `ContentType` enum by name, so renaming a value changes what is stored | A separate enum for storage would duplicate it for little benefit | Add a storage-only enum or stable codes, and map them in `chat/data` |
| 6 | `chat/domain` uses `Flow` from `kotlinx.coroutines` and `PagingData` from `paging-common` | Neither has an Android dependency, and they are how the UI observes and pages Room | Define domain stream and paging abstractions, which is not worth doing unless the libraries must go |
| 7 | The open Chat's id is `MOCK_SARAH_CHAT_ID`, a private constant in `MainActivity.kt` marked as mock data. `MainActivity`, the app's entry point, passes it to `ChatScreen` as its `chatId` parameter, where a navigation argument would come from, and `ChatScreen` passes it to `ChatViewModel` through `parametersOf` | The app has one screen and no chat list to navigate from, so it has no navigation library | Add navigation, open `ChatScreen` from a chat list with the selected `chatId` as its navigation argument, and delete the constant |
