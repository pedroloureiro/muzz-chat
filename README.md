# Muzz Chat

An Android chat screen built with Jetpack Compose, for the Muzz Android exercise. You chat with Sarah: messages you send appear at once and are confirmed by a mock server, and a "Reply as Sarah" button sends the typed text as her, so both sides of the conversation can be tried on one device. Messages are stored in Room and survive restarts. The history is paged, and older pages are fetched from the mock server as you scroll up.

The decisions, limitations and what I would do with more time are also in [`docs/implementation-decisions.pdf`](docs/implementation-decisions.pdf).

## Approach

I built this as an isolated feature meant to be integrated into Muzz, and made it as production-ready as the exercise allowed. A real chat has to cope with messages that arrive late, twice, out of order or not at all, and with history longer than memory. So most of the time went into the system design (a server-authoritative message model with a stable identity and a gap-free order), pagination and sync, and a mock backend that behaves like a real one: latency, an echo on an incoming stream, idempotent sends and history pages. It is not plug and play, but the contract the app relies on is written down in [`docs/api.md`](docs/api.md), and the mock sits behind the interfaces a real data source would implement. Integration should mostly mean replacing the data source, not redesigning the feature.

I did ask myself whether this was over-engineering. I decided a chat that ignores these mechanisms would not work in production. What I left out is listed below as a limitation, with the design for adding it. I also built a small design system from the Muzz press kit, so the app is on brand.

## Build and run

Requirements: Android Studio (latest stable) or JDK 11+, and the Android SDK with API 36. Minimum SDK is 26 (Android 8.0).

```bash
./gradlew installDebug       # build and install on a connected device or emulator
./gradlew test               # unit tests, including Room tests on Robolectric, so no emulator is needed
```

Or open the project in Android Studio and run the `app` configuration.

Using the app:
- **Send** (pink button) sends the typed text as you. It shows faded with three pulsing dots while it is sending, then as sent ~250ms later.
- **Reply as Sarah** (the "S" button) sends the typed text as Sarah, as if from her phone.
- Scroll up to load older messages. A small spinner shows at the top while an older page comes from the server, and a "Start of conversation" pill marks the oldest message. **Jump to latest** (the arrow button) appears when the newest message is out of view.
- On a fresh install, a spinner shows until the first messages arrive.
- To start again from the seed conversation, clear the app's data.

## Assumptions

The brief leaves these open. Each one is a choice I made, and the linked doc gives the details.

| Assumption | Why |
|---|---|
| **There is no backend.** A mock server in the app (`MockChatApi`) stores messages, assigns their order and echoes them back after ~250ms. It never fails. | The brief asks for a local app. The mock acts like a real server, so a real one can replace it without changes above the repository. See [`docs/api.md`](docs/api.md). |
| **The app opens a single Chat, with Sarah.** Its id, Sarah's name and her user id are mock constants in `MainActivity`, passed to the screen where a chat list would pass navigation arguments. | The brief shows one conversation. See accepted deviation 6 in [`docs/architecture.md`](docs/architecture.md#accepted-deviations). |
| **You are a fixed user, given by a mock user service.** There is no sign-in. | The brief has no accounts. `UserRepository.currentUser()` stands where a signed-in user would come from. See [ADR-0004](docs/adr/0004-cross-feature-dependencies.md). |
| **Sarah's messages are sent from this device**, through the mock server, and arrive on the incoming stream like messages from any other device. This device never writes them to Room directly. | The brief asks for a way to trigger the other user's messages. The "Reply as Sarah" path is mock-only and isolated, so it can be deleted. See accepted deviation 7 in [`docs/architecture.md`](docs/architecture.md#accepted-deviations). |
| **The server is the authority on every message.** It assigns the id, a per-Chat sequence number (`seq`) with no gaps, and the timestamp. It takes the sender from the authenticated call, and a resend of the same message is stored once. | This is how production chat backends work, and it is what lets the app order messages, detect missed ones and send safely. Every guarantee the app relies on is listed in [`docs/api.md`](docs/api.md). |
| **The app starts with a 200-message conversation**, from `assets/mock_messages.json`, spread over several days. | Enough history to show paging, grouping and every header format. |
| **Text messages only.** | The brief shows only text. `Content` has a type, so other kinds can be added. See [`CONTEXT.md`](CONTEXT.md). |
| **Messages cannot be edited or deleted, and the chat cannot be cleared.** | Out of scope for the brief. See [`docs/decisions.md`](docs/decisions.md). |
| **The app is in English, and minimum SDK is 26.** | API 26 has `java.time` with no desugaring. See [`docs/decisions.md`](docs/decisions.md). |
| **The app has never been released**, so the Room schema changes in place with no migrations. | No installs exist with data to keep. See "Room schema is not exported" in [`docs/decisions.md`](docs/decisions.md). |

## How the brief's rules are applied

- **Newest messages at the bottom; sent on the right, received on the left, in bubbles.** The list is a `LazyColumn` with `reverseLayout`. Whether a message is sent or received comes from comparing its sender with the current user's id.
- **Small spacing between messages from the same user less than 20 seconds apart.** Such a message is *grouped*: 2dp below it instead of 8dp, and no tail on its bubble, so the tail marks the last bubble of a run.
- **A section header when the previous message is more than an hour older, or there is none.** I changed the label format to depend on how long ago the message was: today `11:59`, within the last 6 days `Thursday 11:59`, otherwise `12 Mar 2026 11:59`. "Thursday" alone would not say which Thursday for older messages.
- **A way to trigger the other user's messages.** The "Reply as Sarah" button, described above.
- **Bonus: a persistent database that the UI observes.** Room is the only source of truth. The UI never holds its own copy of the messages.

These rules live in `ChatItemMapper`, a pure Kotlin object written test-first.

## Main decisions

### Architecture
Clean Architecture inside each feature, with layers as packages in one Gradle module: `ui → domain ← data`, and `di` wiring them with Koin. There are two features: `chat`, and `user`, which gives `chat` the current user. A feature may import only another feature's `domain`. `ArchitectureTest` fails the build when an import breaks these rules, because the compiler would not catch it. A use case exists only where there is logic: `SendMessageUseCase` trims and validates the text and builds the message. Other ViewModel calls go to the repository directly. See [`docs/architecture.md`](docs/architecture.md), [ADR-0002](docs/adr/0002-chat-domain-layer.md) and [ADR-0004](docs/adr/0004-cross-feature-dependencies.md).

```text
UI (Compose, ChatViewModel, ChatItemMapper) → Domain (models, ChatRepository, UserRepository, SendMessageUseCase) ← Data (Room, mock remote)
```

### Sending: optimistic, then confirmed in place
`ChatRepositoryImpl` is the only place messages are written. A sent message is stored in Room at once as *pending* and shown as sending. The mock server stores it and returns it both as the send response and on the incoming stream. Whichever copy arrives first updates the pending row in place, with the server's id, `seq` and timestamp. The second copy changes nothing. Each message is keyed by its sender and a `clientMessageId` that the device generates, from the moment it is sent. The bubble therefore keeps its place in the list, and a retried send can't be stored twice. See [ADR-0001](docs/adr/0001-repository-dual-datasource-echo-pattern.md).

### How message identity evolved
The identity changed three times as I tested it against what a real server and a second device would do:
1. **A device-generated id, and a status flip on echo.** The device owned identity and order, so a wrong clock could reorder the chat.
2. **A server-generated, time-ordered UUIDv7 as order and cursor.** Its values are not consecutive, so a missing message could not be detected. Pending messages also needed a local id and a delete-and-insert swap.
3. **A per-Chat `seq` with no holes.** This made gaps detectable, with a sync state and a catch-up.
4. **The current design: `(senderId, clientMessageId)` as the one key, with `seq` as the order.** The first server copy confirms the pending row in place.

The trade-offs: the server must number each Chat's messages atomically, which serialises writes per Chat, and a deleted message must keep its `seq`. In return, every message has one key from the moment it is sent, confirming is one idempotent write, retries are safe, and missed messages are found exactly. The PDF has the full story.

### Order and paging
Confirmed messages are ordered by the server's `seq`, never by timestamp. Pending ones come after them. The list is paged with Paging 3, with Room as the paging source and a `RemoteMediator` that fetches older history by `seq`. Room is read 20 rows at a time, and each server request fetches 60 messages: more than half of Room's 40-row first read. That ratio works around a race in Paging that stopped older history loading after a page or two. A sync state is stored per Chat. When the Chat opens, or when a message arrives with a `seq` gap, the app fetches every missed message. The paged query also returns each message's next neighbour, because the bubble tail depends on it. See [ADR-0003](docs/adr/0003-cursor-paging.md).

### Scrolling
When you are at the bottom, new messages scroll into view, whoever sent them. When you are reading older messages, the list stays where it is and Jump to latest appears. Send first jumps to the newest message, so what you send always comes into view. Reply as Sarah stands in for a received message, so it does not jump. See [`docs/decisions.md`](docs/decisions.md).

### UI
Every composable is stateless. The screen holds the scroll state and the press and focus state of every control. Colours come from the Muzz press-kit palette, as two tiers of tokens: primitives, and semantic roles that components read. The launcher icon and splash screen are branded. The design departs from the brief's screenshots on purpose: the input field is filled instead of outlined, sending shows pulsing dots instead of ticks, and there is no overflow menu. See [`docs/design-system.md`](docs/design-system.md) and its visual reference [`design-system.html`](docs/design-system.html).

### Tests
Classes with logic are written test-first: `ChatItemMapper`, `SendMessageUseCase`, `ChatViewModel`, `ChatRepositoryImpl`, `MessageRemoteMediator`, `UserRepositoryImpl`, the mappers, converters and date utils. The DAO and the paged query run on an in-memory Room database with Robolectric. Composables are not tested. See the testing table in [`docs/architecture.md`](docs/architecture.md#testing).

### Tech stack

| Choice | Why | Trade-off |
|---|---|---|
| Kotlin 2.3, coroutines, Flow | The standard, and Room, Paging and Compose all speak `Flow` | — |
| Jetpack Compose, Material 3 as the base | Stateless custom components on Muzz tokens | More code than stock Material components |
| Room 2.8 (KSP) | Checked SQL, transactions, `Flow` queries, a ready `PagingSource` | `LIMIT/OFFSET` paging, and a first-load bug that needed a wrapper |
| Paging 3.5 + `RemoteMediator` | The standard for paging from a database while fetching from the network | Maps one row at a time, so the query supplies the next neighbour. `paging-common` reaches the domain |
| Koin 4.2 | Light, a Kotlin DSL, no annotation processing, ViewModel parameters | Bindings are checked at run time, not at compile time as with Hilt |
| kotlinx.serialization | Kotlin-first, no reflection | Differs from Moshi or Gson, if the main app uses one |
| `java.time`, minSdk 26 | Correct date arithmetic with no desugaring | No Android 7.x |
| JUnit 4, Robolectric, paging-testing | Every test, Room included, runs on the JVM with no emulator | No instrumented or UI tests |
| One module, layers as packages | Fast setup for two small features, with `ArchitectureTest` enforcing imports | The compiler does not enforce boundaries |

## Limitations and next steps

- **No failure handling.** The mock never fails, so a failed send would stay "sending", nothing retries it, and the UI shows no error or connection state.
- **Sends do not outlive the screen.** If the screen closes before the server answers, a message can stay pending for good.
- **Outgoing messages show only sending and sent.** There are no delivered or seen states and no sent or seen times.
- **Text only**, with no edit, delete, reply, or clearing the chat.
- **One Chat, with no chat list.** Incoming messages are stored only while the chat is open, and timestamp headers update only when messages change.
- **No sign of a new message while reading history**, beyond the Jump to latest button, which shows whenever the newest message is out of view.
- **Light theme and English only.**

With more time I would add, in this order: sent and seen times on outgoing messages, delivered and seen states, network errors and retries, delete, edit, simple account information for Sarah, replying to a specific message, dark mode, images, documents and videos, audio messages, and a "new message" pill when one arrives while you read older history. Then a chat list with app-level sync, a WorkManager outbox for sends, error states, Room migrations, more languages and separate Gradle modules. The PDF explains each one. [`docs/decisions.md`](docs/decisions.md) has, for each limitation, the steps to remove it.

## Documentation

| Doc | What it covers |
|---|---|
| [`docs/implementation-decisions.pdf`](docs/implementation-decisions.pdf) | Decisions, limitations and what I would do with more time |
| [`CONTEXT.md`](CONTEXT.md) | Domain glossary: the precise meaning of every term used in the code and docs |
| [`docs/architecture.md`](docs/architecture.md) | Architecture rules, package layout, data flows, testing approach and accepted deviations |
| [`docs/adr/`](docs/adr/) | Architecture decision records: message flow (0001), domain layer (0002), paging (0003), feature dependencies (0004) |
| [`docs/decisions.md`](docs/decisions.md) | Smaller decisions, each with its limitation, trade-off and how to remove it |
| [`docs/api.md`](docs/api.md) | What the app assumes of a real backend, and how the mock backend works |
| [`docs/design-system.md`](docs/design-system.md) | Colours, typography, spacing, shapes and every component's states ([visual reference](docs/design-system.html)) |
| [`CLAUDE.md`](CLAUDE.md) | Project conventions for writing code in this repo |
