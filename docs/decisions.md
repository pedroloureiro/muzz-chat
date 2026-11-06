# Decisions Log

Smaller decisions and known limitations. Architectural decisions live in [`adr/`](adr/).

---

## Room schema is not exported

**Decision:** `MessageDatabase` sets `exportSchema = false`.

**Limitation:** The repo holds no record of past schemas, so Room cannot generate auto-migrations and migrations cannot be tested with `MigrationTestHelper`. There are also no migrations today: raising the database version without one crashes the app on upgrade, for anyone with existing data.

**Trade-off:** The app has a single schema version and its data is disposable, so we skip the Room Gradle plugin and generated schema files we would not yet use. The cost is that the first schema change has to set all of this up.

**To support schema changes:**
1. Apply the `androidx.room` Gradle plugin and set `room { schemaDirectory("$projectDir/schemas") }`.
2. Remove `exportSchema = false` and commit the generated version 1 schema, before changing any entity.
3. Make the entity change, raise `version`, and add an `AutoMigration` or a manual `Migration`.
4. Add instrumented test dependencies and cover the migration with `MigrationTestHelper`.

---

## Only the newest 500 messages are shown

**Decision:** `MessageDao.getMessages()` returns the newest 500 messages, in ascending timestamp order.

**Limitation:** Older messages stay in Room but cannot be reached from the UI. Nothing deletes them, so the table keeps growing until "Clear messages" is used.

**Trade-off:** A fixed limit keeps the query and the list simple, and bounds how many rows each Room emission has to load and map. A single local conversation is unlikely to reach the limit.

**To support full history:**
1. Add Paging 3 (`room-paging` and `paging-compose`) and return a `PagingSource` from the DAO.
2. Replace `List<ChatItem>` in `ChatUiState` with paged data, inserting `TimestampHeader` items and computing `isGrouped` across page boundaries.
3. Load older pages as the user scrolls up.

---

## `ChatUiState` has no loading or error state

**Decision:** `ChatUiState` contains only `messages` and `inputText`.

**Limitation:** The UI cannot show a loading indicator or report a failure. A failed send would have no visible feedback.

**Trade-off:** There is no network connection, only a mocked one that always succeeds, so there are no slow requests or failures to display. Modelling those states now would add UI paths that never run.

**To support a real backend:**
1. Add `FAILED` to `MessageDeliveryStatus` and handle the failure branch in the repository, as described in [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md).
2. Render a retry affordance on failed messages.
3. Add loading and connection error state to `ChatUiState`.
