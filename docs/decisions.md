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

**Decision:** Messages carry `timestampMillis: Long`, milliseconds since 1970-01-01T00:00Z, in every layer. The Room column and the JSON field keep the name `timestamp`. It is used for display and grouping, and never for order, which `sequenceId` decides.

**Limitation:** The value cannot be read by eye in the database or in JSON.

**Trade-off:** Epoch millis are already UTC, convert exactly to any zone, and subtract as plain numbers. An ISO-8601 string would need parsing on every mapping and take more space. Tests stay readable by building values with `Instant.parse()`.

---

## The server orders messages by `sequenceId`

**Decision:** The server assigns each confirmed message a `sequenceId`, starting at 1 and increasing by one with no gaps. Room orders confirmed messages by it, and pending messages after them by device timestamp. The order in which messages arrive from `ChatRemoteDataSource` does not matter. See [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md).

**Limitation:**
- A gap in the stored sequence, such as a message missed while the incoming stream was down, is not detected or fetched. The message after the gap is shown as the end of a run, with a tail.
- Timestamps can go backwards between neighbours while a message is pending, because a pending message has the device time. The display rules treat a negative gap as no gap.

**Trade-off:** A server sequence makes order independent of device clocks and gives paging a unique cursor. Gap detection needs a catch-up request that the mock has no reason to need, because its stream never drops.

**To support a real backend:**
1. Add `fetchMessages(afterSequenceId, limit)` to `ChatRemoteDataSource`.
2. When a stored `sequenceId` is not one more than the one before it, or after reconnecting, fetch the messages after the last one stored.

---

## Room schema is not exported

**Decision:** `MessageDatabase` sets `exportSchema = false`. A schema change raises the database version and uses a destructive fallback, with no migration.

**Limitation:** The repo holds no record of past schemas, so Room cannot generate auto-migrations, and a schema change deletes all local messages.

**Trade-off:** The app is not in production and is installed on no devices, so there is no data to keep. Local messages are a copy of the server's, so a real install would fetch them again. We skip the Room Gradle plugin, generated schema files and migrations until real installs exist.

**Once real installs exist:**
1. Apply the `androidx.room` Gradle plugin and set `room { schemaDirectory("$projectDir/schemas") }`.
2. Remove `exportSchema = false` and commit the current schema, before changing any entity.
3. Make each entity change with a raised `version` and an `AutoMigration` or a manual `Migration`.
4. Add instrumented test dependencies and cover each migration with `MigrationTestHelper`.

---

## Message history pages from Room

**Decision:** The message list is paged with Paging 3. Room is the paging source, and `MessageRemoteMediator` fetches older pages from the server by `sequenceId`. See [ADR-0003](adr/0003-cursor-paging.md).

**Limitation:** Nothing deletes old messages from Room, so the table grows with the history the user has scrolled through, until "Clear messages" is used. Room's generated `PagingSource` uses `LIMIT/OFFSET`, which gets slower on very large local tables.

**Trade-off:** Keeping what was loaded makes scrolling back instant and works offline. A local offset over indexed rows stays fast at the sizes a single conversation reaches.

**To support very large histories:**
1. Evict the oldest stored pages beyond a limit, and let the `RemoteMediator` fetch them again.
2. Replace the generated `PagingSource` with one keyed by `sequenceId`.

---

## The UI shows no loading or error state

**Decision:** `ChatUiState` contains only `inputText`, and the screen does not render the paged list's `LoadState`.

**Limitation:** The UI cannot show a loading indicator while a page is fetched, or report a failure. A failed send or page load would have no visible feedback.

**Trade-off:** There is no network connection, only a mocked one that always succeeds at once, so there are no slow requests or failures to display. Modelling those states now would add UI paths that never run.

**To support a real backend:**
1. Add `FAILED` as a state of a pending message and handle the failure branch in the repository, as described in [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md).
2. Render a retry affordance on failed messages.
3. Render the paged list's `LoadState` for page loading and errors, and add connection state to `ChatUiState`.

---

## Clearing messages does not cancel a send in flight

**Decision:** `ChatRepositoryImpl` inserts every echo from `incomingMessages()` without checking whether its message was cleared in the meantime.

**Limitation:** A message cleared during the ~200ms before its echo arrives comes back as confirmed, because the echo inserts it again.

**Trade-off:** The window is small and the backend is a mock. Telling a late echo apart from a new message would add tracking to the repository, and the right rule depends on how a real server treats deletes.

**To support a real backend:**
1. Define what clearing means on the server, and whether it applies to messages in flight.
2. Make the repository drop echoes of cleared messages, for example by remembering the `localId`s that were cleared.
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

**Decision:** `ChatItemMapper` reads the current time, time zone and locale each time it maps, and paging maps again when Room is invalidated.

**Limitation:** Header labels are not recomputed while the messages are unchanged. A header showing `11:59` still shows it after midnight, when it should read `Thursday 11:59`. A change of time zone or locale also shows only after the next message.

**Trade-off:** The labels catch up at the next send or receive, and a chat is rarely left open across these changes. Refreshing on a timer or on system broadcasts would add work to the ViewModel for a rare case.

**To keep labels current:**
1. Invalidate the paged list at each midnight, on `ACTION_TIMEZONE_CHANGED` and on locale changes.

---

## Leaving the chat can leave a send stuck

**Decision:** `ChatViewModel` runs sends in `viewModelScope`.

**Limitation:** If the ViewModel is cleared after the pending row is inserted and before the send finishes, the send is cancelled and the message stays pending for good. Nothing resends it.

**Trade-off:** The window is only ~200ms with the mock, and resending needs the failure handling that is not built yet.

**To support a real backend:**
1. Run sends where they outlive the screen, in an application scope or WorkManager.
2. When the app starts, resend every pending message with its `localId`. The server recognises the `localId`, so a message it already stored is returned, not stored twice.

---

## "Clear messages" only clears the device

**Decision:** `clearMessages()` deletes from Room and leaves the mock backend untouched. `MockData` keeps sent messages in memory, after the seed messages.

**Limitation:** Sent messages come back when "Load sample data" runs after "Clear messages", as they would from a real server. The mock forgets them when the process dies, so after a restart they survive only in Room, and clearing then removes them for good.

**Trade-off:** This makes the mock behave like a server without adding storage to a fake one.

**To support a real backend:**
1. Replace `mockBackendModule`. The server then owns history and it survives restarts.
2. Decide whether clearing should also delete on the server, which ties in with clearing during a send above.