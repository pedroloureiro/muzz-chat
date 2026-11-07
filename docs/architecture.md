# Architecture

The rules every change must follow. The reasoning is in [ADR-0002](adr/0002-chat-domain-layer.md), the message flow in [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md), and vocabulary in [`CONTEXT.md`](../CONTEXT.md).

Issues #4–#11 build out the parts of this structure that do not exist yet.

---

## Overview

The app follows Clean Architecture inside each feature, using packages in a single Gradle module. Chat is the only feature.

```text
ui ──> domain <── data
di ──> ui, domain, data
```

| Layer | Responsibility | May depend on |
|---|---|---|
| `domain` | Business models, repository contracts, use cases | Kotlin, `kotlinx.coroutines`, `java.util` |
| `data` | Implements `domain` contracts using Room and the remote service | `domain`, Android, Room, serialization |
| `ui` | Compose screens, ViewModel, UI state, display rules | `domain`, Android, Compose, top-level `ui` |
| `di` | Creates objects and wires the layers together | All layers |
| `utils` (top-level) | Static utility functions that know nothing about the app. Any layer may use them | Kotlin, the JDK |

---

## Package structure

```text
com.pedroloureiro.muzzchat
├── MainActivity, MuzzChatApplication
├── ui
│   ├── theme                 Colours, typography, spacing, theme
│   └── components            Generic composables: Button, TextInput
├── utils                     Static utility functions: DateTimeUtils
└── chat
    ├── di                    ChatModule
    ├── domain
    │   ├── model             Message, Content, ContentType, User + Users, MessageDeliveryStatus
    │   ├── repository        ChatRepository (interface)
    │   └── usecase           SendMessageUseCase
    ├── data
    │   ├── ChatRepositoryImpl, MessageMappers
    │   ├── local
    │   │   ├── ChatLocalDataSource, RoomChatLocalDataSource
    │   │   └── db
    │   │       ├── MessageDatabase, Converters
    │   │       ├── dao       MessageDao
    │   │       └── entity    MessageEntity
    │   ├── remote            ChatRemoteDataSource, MockChatRemoteDataSource
    │   │   └── model         MessageDto, ContentDto
    │   └── mock              MockData
    └── ui
        ├── ChatScreen, ChatViewModel, ChatUiState, ChatItemMapper
        ├── model             ChatItem, MessageDirection
        └── components        MessageBubble, MessageItem, SectionHeader, ChatInput, TopBar, Avatar
```

A new feature gets its own package beside `chat`, with the same layers. Code moves to the top level only when it knows nothing about any feature.

---

## Rules

### Dependencies
1. `chat/domain` imports no Android, Room, serialization, `chat/data` or `chat/ui` code.
2. `chat/ui` imports nothing from `chat/data`.
3. `chat/data` imports nothing from `chat/ui`.
4. Only `chat/di` may see every layer.

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
12. Composables are stateless and receive only what they need to render.
13. Display rules, such as grouping and timestamp headers, belong to `ChatItemMapper`.
14. `ChatItemMapper` has no Android dependencies, so it can run on the JVM.

### Dependency injection
15. Koin wiring lives in `chat/di`. Classes receive dependencies through their constructors and never call Koin themselves.
16. Android types stay out of `chat/domain` contracts. A `data` class that needs `Context` receives it through Koin.
17. Long-running work in `chat/data` is a `suspend` function whose caller owns its lifetime through a scope. Nothing uses `GlobalScope`.

---

## Sending a message

```text
ChatViewModel
  → SendMessageUseCase          trims and validates text, builds Message(SENDING)
  → ChatRepository.sendMessage
  → ChatRepositoryImpl          inserts into Room, then calls the remote data source
  → Echo received               status becomes SENT in Room
  → Room Flow                   mapped to List<Message> by the repository
  → ChatItemMapper              mapped to List<ChatItem>
  → ChatUiState                 rendered by ChatScreen
```

---

## Testing

| Code | Approach |
|---|---|
| Classes with logic: use cases, `ChatItemMapper`, `ChatRepositoryImpl`, `Converters` | Unit tests, written first |
| DAO queries | `MessageDaoTest`, using Robolectric and an in-memory Room database, so no emulator is needed |
| Forwarding code | Not tested directly |
| Composables | Not tested |
| Architecture rules 1–3 | `ArchitectureTest` scans imports (#11) |

---

## Enforcement

`ArchitectureTest` fails the test run when a dependency rule is broken. Because the layers are packages, a bad import still compiles until the test runs. The other rules rely on code review.

---

## Accepted deviations

These are the places where the app knowingly departs from strict Clean Architecture.

| # | Deviation | Why we accept it | How to correct it |
|---|---|---|---|
| 1 | `ChatViewModel` calls `ChatRepository` directly for observe, load and clear, with no use case in between | Those use cases would only forward the call | Add a use case when an operation gains logic, or add all of them if a uniform rule becomes worth the code |
| 2 | Layers are packages in one Gradle module, so boundaries are checked by a test and not the compiler | With one feature, separate modules would add build setup without enough benefit | Split into `:feature:chat:domain`, `:feature:chat:data` and `:feature:chat:presentation`, with `domain` as a plain Kotlin module and `:app` as the wiring point |
| 3 | `Users.ME` is a global constant, so `ChatItemMapper` has the current user hardcoded | The app has no authentication and its users are fixed by design | Add a `CurrentUser` abstraction when login exists, and inject it into the mapper |
| 4 | The 500-message limit is a product rule that lives in `MessageDao`'s SQL | It keeps the query simple, and one screen uses it. See [`decisions.md`](decisions.md) | Pass the limit from the domain through `ChatRepository`, or replace it with paging |
| 5 | `MessageEntity` stores the domain `ContentType` and `MessageDeliveryStatus` enums by name, so renaming a value changes what is stored | A separate set of enums for storage would duplicate them for little benefit | Add storage-only enums or stable codes, and map them in `chat/data` |
| 6 | `chat/domain` uses `Flow` from `kotlinx.coroutines` | It has no Android dependency, and it is how the UI observes Room | Define a domain stream abstraction, which is not worth doing unless the library must go |
