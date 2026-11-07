# Decisions Log

Smaller decisions and known limitations. Architectural decisions live in [`adr/`](adr/).

---

## Minimum SDK is 26

**Decision:** `minSdk` is 26 (Android 8.0), raised from the template's 24.

**Limitation:** The app does not install on Android 7.0 and 7.1 (API 24–25).

**Trade-off:** Nothing requires the app to support API 24 or 25. API 26 includes `java.time`, the standard library for dates and times, so `utils/DateTimeUtils` uses it with no core library desugaring and no legacy `SimpleDateFormat`.

**To support API 24–25:**
1. Set `minSdk = 24` and enable `isCoreLibraryDesugaringEnabled` in `compileOptions`.
2. Add the `desugar_jdk_libs` dependency with `coreLibraryDesugaring(...)`.

---

## Message timestamps are epoch millis

**Decision:** Messages carry `timestampMillis: Long`, milliseconds since 1970-01-01T00:00Z, in every layer. The Room column and the JSON field keep the name `timestamp`.

**Limitation:** The value cannot be read by eye in the database or in JSON.

**Trade-off:** Epoch millis are already UTC, convert exactly to any zone, and sort and subtract as plain numbers. An ISO-8601 string would need parsing on every mapping, take more space, and only sort correctly if every value used the same offset and precision. Tests stay readable by building values with `Instant.parse()`. Keeping the stored names avoids a Room migration and a change to the API contract.

---

## Room orders messages by timestamp

**Decision:** The server owns message order through each message's `timestampMillis`. `MessageDao` sorts by it, and the order in which messages arrive from `ChatRemoteDataSource` or `MockData` does not matter.

**Limitation:**
- For sent messages, `timestampMillis` comes from the device clock in `SendMessageUseCase`, and the mock echoes it back unchanged, so the server never sets their order.
- Messages with the same `timestampMillis` have no defined order between them.
- A `SENDING` message has no server order yet. It sorts by its device time until the echo arrives.

**Trade-off:** Room has to sort, because SQLite returns rows in no guaranteed order, and messages are not inserted in the order the server holds them. Older pages arrive after newer messages, and an echo written with `REPLACE` is inserted again. With a mock backend and one device, the device clock is close enough to a server order.

**To support a real backend:**
1. Have the server assign each message an ordering key, either a server timestamp or a sequence number, and return it on the echo.
2. Store it when the echo arrives, replacing the device time, and have `MessageDao` sort by it.
3. Add `messageId` as a tie-breaker, or rely on the sequence number being unique.
4. With Paging 3, have the `RemoteMediator` write each page into Room, and let the `PagingSource` query sort by the same key.

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

## Timestamp headers update only when messages change

**Decision:** `ChatItemMapper.map()` reads the current time, time zone and locale on each call, and `ChatViewModel` calls it when Room emits.

**Limitation:** Header labels are not recomputed while the message list is unchanged. A header showing `11:59` still shows it after midnight, when it should read `Thursday 11:59`. A change of time zone or locale also shows only after the next message.

**Trade-off:** The labels catch up at the next send or receive, and a chat is rarely left open across these changes. Remapping on a timer or on system broadcasts would add work to the ViewModel for a rare case.

**To keep labels current:**
1. Combine the Room Flow with a ticker that emits at each midnight, and with `ACTION_TIMEZONE_CHANGED` and locale changes, and map again on each emission.

---

## Leaving the chat can leave a send stuck

**Decision:** `ChatViewModel` runs sends in `viewModelScope`.

**Limitation:** If the ViewModel is cleared after the optimistic insert and before the send finishes, the send is cancelled and the message stays `SENDING` for good. Nothing retries it.

**Trade-off:** The window is only ~200ms with the mock, and retrying a send needs the `FAILED` handling that is not built yet.

**To support a real backend:**
1. Run sends where they outlive the screen, in an application scope or WorkManager.
2. When the app starts, resend or fail any message still marked `SENDING`.

---

## "Clear messages" only clears the device

**Decision:** `clearMessages()` deletes from Room and leaves the mock backend untouched. `MockData` keeps sent messages in memory, so `fetchMessages()` returns them after the seed messages.

**Limitation:** Sent messages come back when "Load sample data" runs after "Clear messages", as they would from a real server. The mock forgets them when the process dies, so after a restart they survive only in Room, and clearing then removes them for good.

**Trade-off:** This makes the mock behave like a server without adding storage to a fake one.

**To support a real backend:**
1. Replace `mockBackendModule`. The server then owns history and it survives restarts.
2. Decide whether clearing should also delete on the server, which ties in with clearing during a send above.
