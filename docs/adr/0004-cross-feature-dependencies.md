# ADR-0004: Features depend one way, on each other's domain only

## Status
Accepted.

## Context
The current user's identity came from `Users.ME`, a global constant in `chat/domain/models` read by `SendMessageUseCase` and `ChatItemMapper`. Moving it behind a `UserRepository` adds a second feature, `user`, and `chat` needs it to know who is sending and which messages are outgoing.

The dependency rules covered the layers inside `chat` only. Nothing said whether one feature may import another, or which of its layers.

## Decision

### A feature imports another feature's `domain` only
`chat` may import `user/domain`, the `User` model and the `UserRepository` interface. It never imports `user/data`, `user/ui` or `user/di`. Each feature's Koin module binds its own implementations, and `MuzzChatApplication` starts both modules, so `chat` receives a `UserRepository` without knowing `UserRepositoryImpl`.

### Features depend one way
`chat` depends on `user`, and `user` imports nothing from `chat`. The mock current user, `MockUserRemoteDataSource`, lives in `user` for this reason, even though the seed messages in `chat` use its id.

### `ArchitectureTest` lists the features
The test lists each feature and the features it may depend on. It runs the layer rules for every feature, checks that a feature imports only the `domain` of the features it depends on, and fails when a feature package is missing from the list, so a new feature has to declare its dependencies.

## Consequences
- `User` moves to `user/domain`, and `Users` is deleted. Sarah's name and id reach the chat screen as navigation values, like the `chatId`.
- A change to `user/data` cannot break `chat`, and a cycle between the two would fail the test.
- The rule is checked by a test, not the compiler. Separate Gradle modules per feature would make it a compile error, as described in accepted deviation 2 in [`architecture.md`](../architecture.md#accepted-deviations).
