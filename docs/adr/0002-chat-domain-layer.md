# ADR-0002: Domain layer in the chat feature, with use cases only where there is logic

## Status
Accepted. Replaces the earlier two-layer decision, and changes where the send flow in [ADR-0001](0001-repository-dual-datasource-echo-pattern.md) starts.

## Context
The project started with two layers: presentation depending directly on data, with a domain layer to be added only if business logic appeared.

That design had three problems:
- `ChatRepository` lived in `data` and returned `MessageEntity`, so a Room class reached the UI.
- The UI layer depended on the data layer, so the dependency did not point towards the business rules.
- Rules for sending a message were split between `ChatViewModel` and `ChatRepositoryImpl`.

## Decision

### Add a domain layer
`chat/domain` holds the business models, the `ChatRepository` interface and use cases. `data` implements the interface and `ui` depends only on the domain, so both point inwards. The domain is free of Android and Room code.

### Keep the layers as packages
The layers stay in the single `app` module, and `ArchitectureTest` checks the dependency rules. Separate Gradle modules would make violations compile errors, but one feature does not justify the extra build setup.

### Add a use case only when an operation has logic of its own
`SendMessageUseCase` exists because it trims and validates text and builds the `Message`. Observing, loading and clearing messages have no logic, so `ChatViewModel` calls `ChatRepository` directly for those.

### Keep persistence separate from the domain
`MessageEntity` has its own `contentType` and `contentValue` columns instead of embedding the domain `Content`, so changing `Content` cannot change the database schema by accident.

## Consequences
- Room types cannot reach the UI through `ChatRepository`, and a test catches imports that break the dependency rules.
- There is one extra model, `Message`, and a mapping between it and `MessageEntity`.
- `ChatViewModel` uses both `SendMessageUseCase` and `ChatRepository`, which is deliberate and follows the rule above.
- Strict Clean Architecture is not fully met. The accepted deviations are listed in [`architecture.md`](../architecture.md#accepted-deviations).
