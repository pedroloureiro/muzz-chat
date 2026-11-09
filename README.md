# Muzz Chat

A single-conversation Android chat screen built with Jetpack Compose. Two fixed users (`ME` and `SARAH`) exchange text messages; everything runs locally with no backend or authentication.

When you send a message it appears immediately with a *sending* indicator, then is confirmed after a short simulated server echo (~200ms) — mirroring how a production chat app behaves.

## Architecture

Clean Architecture inside the chat feature, with dependencies pointing towards the domain. The full rules and accepted deviations are in [`docs/architecture.md`](docs/architecture.md).

```
UI (Compose + ViewModel)  →  Domain (models, ChatRepository, use cases)  ←  Data (ChatRepositoryImpl, Room, remote)
```

- **UI observes Room through `ChatRepository`** via `Flow`. There is no duplicated state.
- **The repository is the single ingestion point for all messages.** It coordinates a `ChatLocalDataSource` (Room, source of truth) and a `ChatRemoteDataSource` (the messaging service).
- **Mock echo pattern:** sent messages are inserted into Room as `SENDING`, posted to the remote data source, and flipped to `SENT` when the echo arrives on `incomingMessages()`. `MockChatRemoteDataSource` provides the echo, and can be swapped for a real WebSocket implementation without touching anything above the repository. See [ADR-0001](docs/adr/0001-repository-dual-datasource-echo-pattern.md).
- **`ChatItemMapper`** (pure Kotlin) turns messages into renderable rows, inserting timestamp headers and computing message grouping.

### Package layout

Under `com.pedroloureiro.muzzchat`:

| Package | Responsibility |
|---|---|
| `chat/domain` | Business models, repository interface, use cases |
| `chat/data` | Repository implementation, data sources, Room, mock data |
| `chat/ui` | Chat screen, ViewModel, UI state |
| `chat/ui/components` | Chat-specific stateless composables |
| `chat/di` | Dependency wiring |
| `user/domain` | `User` and the repository interface for the current user, which `chat` imports |
| `user/data` | Repository implementation and the mock user data source |
| `user/di` | Dependency wiring |
| `ui/components` | Generic stateless composables |
| `ui/theme` | Colours, typography, theme |

## Documentation

- [`CONTEXT.md`](CONTEXT.md) — domain glossary
- [`docs/architecture.md`](docs/architecture.md) — architecture rules and accepted deviations
- [`docs/adr/`](docs/adr/) — architecture decision records
- [`docs/decisions.md`](docs/decisions.md) — smaller decisions and known limitations
- [`docs/api.md`](docs/api.md) — what the app assumes of the backend, and how the mock backend works
- [`docs/design-system.md`](docs/design-system.md) — colours, spacing, shapes (visual reference: [`design-system.html`](docs/design-system.html))
- [`CLAUDE.md`](CLAUDE.md) — project conventions

## Build and run

Requirements: Android Studio (latest stable) or JDK 11+, and the Android SDK with API 36.

```bash
./gradlew assembleDebug      # build
./gradlew installDebug       # install on a connected device or emulator
./gradlew test               # unit tests
```

Or open the project in Android Studio and run the `app` configuration. Minimum SDK is 24.
