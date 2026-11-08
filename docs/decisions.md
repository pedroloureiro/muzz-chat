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

## Opening a Chat catches up forward from the newest gap-free message

**Decision:** REFRESH fetches the pages after the Chat sync state's `newestSeq` until the server reports `hasMore = false`. With no sync state, it fetches the newest page. An incoming message that skips a `seq` starts the same catch-up. See [ADR-0003](adr/0003-cursor-paging.md).

**Limitation:** After a long absence, the catch-up downloads every missed message, one page per request, before the Chat is up to date.

**Trade-off:** Stored history is never thrown away, and the history stays complete with no gaps. Dropping it and starting from the newest page would cost fewer requests after a long absence, but would download the dropped history again while scrolling.

**To support very long absences:**
1. Beyond a number of catch-up pages, delete the Chat's stored confirmed messages and its sync state in one transaction, and fetch the newest page.

---

## Paged history has no memory or storage cap

**Decision:** `PagingConfig` keeps its default `maxSize`, and nothing deletes old messages from Room. Paging replaced the window of the newest 500 messages.

**Limitation:** Every page the user scrolls past stays in memory until the list is recreated, and the `messages` table grows with every page fetched.

**Trade-off:** Paging already reads from Room only the pages near the screen, which was the reason for the old cap. The seed has 200 messages, so neither limit is reached, and dropping pages can make scrolling back less smooth.

**To support long histories:**
1. Set `PagingConfig.maxSize` so pages far from the screen are dropped. It must be at least `pageSize + 2 * prefetchDistance`.
2. Trim the oldest confirmed messages when storage runs low, resetting the Chat's `oldestSeq` and `reachedStart` in the same transaction. Scrolling back to them fetches them again through `MessageRemoteMediator`.

---

## Message timestamps are epoch millis

**Decision:** Messages carry `timestampMillis: Long`, milliseconds since 1970-01-01T00:00Z, in every layer, including the Room column and the JSON field. It is used for display and grouping, and never for order, which `seq` decides.

**Limitation:** The value cannot be read by eye in the database or in JSON.

**Trade-off:** Epoch millis are already UTC, convert exactly to any zone, and subtract as plain numbers. An ISO-8601 string would need parsing on every mapping and take more space. Tests stay readable by building values with `Instant.parse()`.

---

## The server orders messages by a per-Chat sequence number

**Decision:** The server gives each message of a Chat the next `seq`, starting at 1 with no holes, in the same transaction that stores it. `seq` is the only order of confirmed messages and the paging cursor. The server `id` is an opaque UUID used only for identity. Pending messages sort after confirmed ones by device timestamp. See [ADR-0001](adr/0001-repository-dual-datasource-echo-pattern.md) and [ADR-0003](adr/0003-cursor-paging.md).

**Limitation:**
- Every write to a Chat goes through one counter on the server, so writes within a Chat are serialised. Very busy group Chats would need sharded or batched numbering.
- A deleted message must keep its `seq`, or a hole would look like a missing message. See "Messages cannot be edited or deleted".
- Timestamps can go backwards between neighbours while a message is pending, because a pending message has the device time. The display rules treat a negative gap as no gap.

**Trade-off:** A sequence with no holes makes a missing message detectable and the start of the history known, and needs no tiebreaker between messages stored in the same millisecond. A time-ordered UUIDv7 needs no counter, but its values are not consecutive, so gaps could only be guessed.

---

## Room schema is not exported

**Decision:** `MessageDatabase` sets `exportSchema = false`, and the database stays at version 1. A schema change edits the entities in place, with no version bump and no migration.

**Limitation:** The repo holds no record of past schemas, so Room cannot generate auto-migrations. A build with a changed schema crashes on a device that has the old database, until the app is reinstalled or its data is cleared.

**Trade-off:** The app is not in production and is installed on no devices, so there is no data to keep. Local messages are a copy of the server's, so a real install would fetch them again. We skip version bumps, the Room Gradle plugin, generated schema files and migrations until real installs exist.

**Once real installs exist:**
1. Apply the `androidx.room` Gradle plugin and set `room { schemaDirectory("$projectDir/schemas") }`.
2. Remove `exportSchema = false` and commit the current schema, before changing any entity.
3. Make each entity change with a raised `version` and an `AutoMigration` or a manual `Migration`.
4. Add instrumented test dependencies and cover each migration with `MigrationTestHelper`.

---

## Message history pages from Room

**Decision:** The message list is paged with Paging 3. Room is the paging source, and `MessageRemoteMediator` fetches a Chat's history from the server by `seq`, following the server's `nextCursor` and `hasMore`. See [ADR-0003](adr/0003-cursor-paging.md).

**Limitation:** Nothing deletes old messages from Room, so the table grows with the history the user has scrolled through. Room's generated `PagingSource` uses `LIMIT/OFFSET`, which gets slower on very large local tables.

**Trade-off:** Keeping what was loaded makes scrolling back instant and works offline. A local offset over indexed rows stays fast at the sizes a single Chat reaches.

**To support very large histories:**
1. Keep the newest N messages per Chat, delete older ones, and in the same transaction set the Chat's `oldestSeq` to the oldest kept `seq` and `reachedStart` to false, so scrolling back fetches them again.
2. Replace the generated `PagingSource` with one keyed by `seq`.

---

## The UI shows no loading or error state

**Decision:** `ChatUiState` contains only `inputText`, and the screen does not render the paged list's `LoadState`.

**Limitation:** The UI cannot show a loading indicator while a page is fetched, or report a failure. A failed send or page load would have no visible feedback.

**Trade-off:** There is no network connection, only a mocked one that always succeeds after a fixed delay of a few hundred milliseconds, so there are no failures to display and no slow requests. Modelling those states now would add UI paths that never run.

**To support a real backend:**
1. Render the paged list's `LoadState` for page loading and errors, and add connection state to `ChatUiState`.
2. Show failed sends, as described in "Sends are not retried and cannot fail".

---

## `ChatViewModel` runs the incoming message sync, for the open Chat only

**Decision:** `ChatViewModel` calls `ChatRepository.syncIncomingMessages(chatId)` in `viewModelScope`. It stores the open Chat's messages and ignores the others.

**Limitation:** Incoming messages are stored only while the ViewModel is alive, and only for the open Chat. Other Chats' messages reach Room when that Chat is opened and catches up. The sync also keeps running when the app is in the background, and it stops when the user leaves the Chat.

**Trade-off:** The app has one screen and no chat list, so nothing would read other Chats' messages. The catch-up on opening a Chat fills whatever the stream missed. In exchange, the repository needs no injected scope and starts no work when it is created.

**To support a chat list:**
1. Run one app-level handler for the stream, using `ProcessLifecycleOwner` and `repeatOnLifecycle(STARTED)` so it follows the foreground, and store every Chat's messages, each moving its own sync state.
2. Store Chats in a `chats` table with the last message preview, last activity time and unread count, updated with each message.
3. Catch up every Chat with a sync state after each reconnect.
4. Add push notifications for messages that arrive while the app is in the background. Opening a Chat from one catches up as usual.

---

## Timestamp headers update only when messages change

**Decision:** `ChatItemMapper` reads the current time, time zone and locale each time it maps, and paging maps again when Room is invalidated.

**Limitation:** Header labels are not recomputed while the messages are unchanged. A header showing `11:59` still shows it after midnight, when it should read `Thursday 11:59`. A change of time zone or locale also shows only after the next message.

**Trade-off:** The labels catch up at the next send or receive, and a chat is rarely left open across these changes. Refreshing on a timer or on system broadcasts would add work to the ViewModel for a rare case.

**To keep labels current:**
1. Invalidate the paged list at each midnight, on `ACTION_TIMEZONE_CHANGED` and on locale changes.

---

## Sends are not retried and cannot fail

**Decision:** `ChatViewModel` runs sends in `viewModelScope`, and a failed send leaves the message pending. A message is confirmed by whichever arrives first, the send response or its echo on the stream, and the second changes nothing.

**Limitation:**
- If the ViewModel is cleared after the pending row is inserted and before the send finishes, the send is cancelled and the message stays pending for good. Nothing resends it.
- A send that fails stays `SENDING`, and the user cannot retry it.

**Trade-off:** The mock never fails and answers in ~200ms. Retries, failure states and an outbox would add paths that never run here.

**To support a real backend:**
1. Keep pending messages as an outbox in the same table, and send each from a WorkManager job with a network constraint and exponential backoff, so a send outlives the screen and the process.
2. Retry network errors and 5xx responses with the same `localId`. The server keeps a unique key on `(senderId, localId)` and answers a repeat with the message it already stored, so a retry after an ambiguous timeout cannot store it twice.
3. Mark a 4xx response (invalid, blocked, Chat gone) `FAILED` at once, with its reason, since retrying cannot help. Mark it `FAILED` too when retries run out.
4. Add a `status` column for `FAILED`, indexed with `chatId`, and a tap-to-retry action that enqueues the job again with the same `localId`.

---

## Messages cannot be edited or deleted

**Decision:** A confirmed message never changes after the server stores it. There are no edit or delete operations, and only `TEXT` content.

**Limitation:** The user cannot correct or remove a message.

**Trade-off:** With no changes after confirmation, a late or repeated echo cannot overwrite newer state, so writes need no version check. Out of scope for this exercise.

**To support edits and deletes:**
1. Delete as a tombstone: the message keeps its `seq` and loses its content, with a `deleted` flag, so its `seq` never becomes a hole.
2. Add `editedAt`, and a server `changeSeq` that rises with every change to a message. Apply a change only when its `changeSeq` is newer than the stored one, so a late event cannot overwrite a newer one.
3. Deliver edits and deletes as their own events on the stream, such as `message.updated`.
4. Add content types, such as images, as new `ContentType` values.

---

## The mock backend keeps sent messages only in memory

**Decision:** `MockData` keeps sent messages in memory, after the seed messages.

**Limitation:** The mock forgets them when the process dies, so after a restart they survive only in Room.

**Trade-off:** This makes the mock behave like a server without adding storage to a fake one.

**To support a real backend:**
1. Replace `mockBackendModule`. The server then owns history and it survives restarts.
