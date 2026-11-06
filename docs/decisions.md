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

---

## Clearing messages does not cancel a send in flight

**Decision:** `ChatRepositoryImpl` inserts every message from `incomingMessages()` without checking whether it was cleared in the meantime.

**Limitation:** A message cleared during the ~200ms before its echo arrives comes back with status `SENT`, because the echo inserts it again.

**Trade-off:** The window is small and the backend is a mock. Telling a late echo apart from a new message would add tracking to the repository, and the right rule depends on how a real server treats deletes.

**To support a real backend:**
1. Define what clearing means on the server, and whether it applies to messages in flight.
2. Make the repository drop echoes of cleared messages, for example by remembering the time of the last clear and ignoring older messages.
3. Cover clearing during a send in `ChatRepositoryImplTest`.

---

## `ChatViewModel` runs the incoming message sync

**Decision:** `ChatViewModel` calls `ChatRepository.syncIncomingMessages()` in `viewModelScope`.

**Limitation:** Incoming messages are stored only while the ViewModel is alive. The sync also keeps running when the app is in the background, and it stops when the user leaves the chat.

**Trade-off:** The app has one screen and a mock that emits only after a send, so these gaps have no visible effect today. In exchange, the repository needs no injected scope and starts no work when it is created.

**To support a real backend:**
1. Call `syncIncomingMessages()` from an app-level owner, using `ProcessLifecycleOwner` and `repeatOnLifecycle(STARTED)` so it follows the foreground.
2. Add reconnection, and a catch-up sync for messages missed while disconnected.
3. Add push notifications for messages that arrive while the app is in the background.

---

## Leaving the chat can leave a send stuck

**Decision:** `ChatViewModel` runs sends in `viewModelScope`.

**Limitation:** If the ViewModel is cleared after the optimistic insert and before the send finishes, the send is cancelled and the message stays `SENDING` for good. Nothing retries it.

**Trade-off:** The window is only ~200ms with the mock, and retrying a send needs the `FAILED` handling that is not built yet.

**To support a real backend:**
1. Run sends where they outlive the screen, in an application scope or WorkManager.
2. When the app starts, resend or fail any message still marked `SENDING`.
