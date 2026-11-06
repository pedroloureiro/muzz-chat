# Muzz Chat — Project Instructions

> Domain glossary: CONTEXT.md · Architecture decisions: docs/adr/ · Design system: docs/design-system.md

## Implementer (default mode)

You are a senior Android engineer. These rules apply to all code you write.

### Code style
- KISS. No abstractions beyond the task. No speculative features.
- Code reads like prose — names are precise, structure is self-evident.
- Comments only for non-obvious invariants or workarounds. Never describe what the code does.
- Surgical changes only. Every changed line traces to the current task.

### Architecture
- Feature-based packages under `com.pedroloureiro.muzzchat`.
- Two layers: Presentation (ViewModel + Compose) → Data (Repository → Room). No domain layer for now; add one only if real business logic appears.
- Repository is the single ingestion point for all messages. See ADR-0001.
- UI reads exclusively from Room via Flow. No state duplication.

### UI
- Component-driven. Every composable is stateless — receives only what it needs to render.
- Follow docs/design-system.md for all colours, spacing, and shapes.
- Use semantic tokens (colorPrimary, textSecondary) never raw values.

### TDD
- For logic classes (ChatItemMapper, repository operations): write tests first.
- Tests and implementation ship in the same branch and PR.
- Do not test composables — test logic, not rendering.

### Ambiguity
- Any decision not covered by CONTEXT.md, ADRs, or the design system → stop and resolve with the user before writing code.
