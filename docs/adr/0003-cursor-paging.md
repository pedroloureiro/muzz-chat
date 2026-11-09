# ADR-0003: Cursor-based paging of a Chat's history by sequence number, with Room as the paging source

## Status
Accepted, revised. The first version used the UUIDv7 message `id` as the order and the cursor, and found gaps by checking whether the newest page reached the stored messages. This version orders and pages each Chat by a server-assigned `seq` with no holes, follows the server's `nextCursor` and `hasMore`, keeps a sync state per Chat, and fills gaps by catching up forward. Builds on [ADR-0001](0001-repository-dual-datasource-echo-pattern.md). Replaces the fixed window of the newest 500 messages.

## Context
A Chat has no limit on length. A production chat screen cannot load the whole history into memory, map all of it on every change, or fetch it from the server in one request. It loads the newest messages first and older ones as the user scrolls up.

Paging by page number (offset) breaks as soon as new messages arrive: every message shifts down, so the next page repeats messages the user has already seen. The standard for chat is cursor paging: "the 50 messages before this one".

A cursor must be unique and strictly ordered within the Chat. Timestamps are neither. A time-ordered ID (UUIDv7) is, but its values are not consecutive, so the client cannot tell from two IDs whether a message between them is missing. A per-Chat sequence number can: the server gives each message of a Chat the next `seq`, starting at 1, with no holes. A missing number is a missing message, and `seq` 1 is the start of the history.

The device also loses messages: the incoming stream drops while the app is closed or reconnecting, so more than a page of messages can arrive unseen.

## Decision

### Messages belong to a Chat
Every message carries a `chatId`. Every remote call takes it, as part of the endpoint (`/chats/{chatId}/messages`). Room stores all Chats in one table, and every query is limited to one `chatId`. The `chatId` is a navigation argument of the chat screen, passed to its ViewModel, never injected. The app has no chat list, so `MainActivity` opens one Chat, whose id is mock data (see [`architecture.md`](../architecture.md#accepted-deviations)).

### Remote contract
- `fetchMessagesBefore(chatId, beforeSeq: Long?, limit)` returns up to `limit` confirmed messages older than `beforeSeq`, or the newest ones when it is `null`.
- `fetchMessagesAfter(chatId, afterSeq: Long, limit)` returns up to `limit` confirmed messages newer than `afterSeq`, oldest first, so a page continues the history with no gap.
- Both return a `MessagePageDto(messages, nextCursor, hasMore)`. `nextCursor` is the `seq` to continue from in the same direction, and `hasMore` says whether more messages remain in that direction.
- `incomingMessages()` carries every Chat's messages on one stream, and each message carries its `chatId` and `seq`.

The mock answers these by filtering and slicing the list in `MockChatApi`.

### Room is the paging source, with a `RemoteMediator`
Paging 3 reads from Room, and a `RemoteMediator` fetches from the server into Room. Room is the single source of truth: the paged list, optimistic sends and echoes all go through the same table.

Each Chat has a **sync state** row in `chat_sync`: `oldestSeq` (where older history continues, from the server's `nextCursor`), `newestSeq` (the newest `seq` up to which stored history has no gaps) and `reachedStart`. It is written in the same transaction as the messages that move it. It is stored, not computed from the messages, because an incoming message can be stored ahead of a gap: with 41 stored and 43 received, the newest stored `seq` is 43, but history is complete only up to 41.

- **REFRESH** runs when the Chat is opened. With no sync state, it fetches the newest page. Otherwise it runs a **catch-up**: it fetches pages after `newestSeq` until `hasMore` is false, so messages missed while the app was away are filled in and nothing stored is dropped.
- **APPEND** (scrolling up) fetches the page before `oldestSeq`. It ends at once when `reachedStart` is set, and a page with `hasMore = false` sets it.
- **PREPEND** ends at once. New messages arrive through `syncIncomingMessages(chatId)`, not through paging.
- The page size is `MESSAGE_PAGE_SIZE` in `chat/data`, beside the mediator, and is also the `limit` sent to the server. It is 20, so the seed's history spans several pages and paging can be seen while scrolling.

### Incoming messages and gaps
`syncIncomingMessages(chatId)` stores the open Chat's messages and ignores the others. A message whose `seq` is `newestSeq + 1` moves `newestSeq` on. A message that skips a `seq` is stored, so it shows at once in its place, and a catch-up runs straight away to fetch the missing ones. An echo of this device's own send follows the same rule.

### The paged query carries each message's next neighbour
Room's `PagingSource` reads one Chat's messages newest first: pending messages by device timestamp, then confirmed messages by `seq`. Each row also returns the sender and timestamp of the **next** message in Chat order. This is computed by the query and not stored:

- For a confirmed message, the next message is the stored confirmed message with the smallest `seq` greater than its own, found through the index on `(chatId, seq)`. For the newest confirmed message, it is the oldest pending message, if there is one.
- For a pending message, it is the next pending message by device timestamp.

Each row is a `MessageWithNext`. The query exists for the bubble tail, which is drawn on the last bubble of a run from one sender, so it depends on the message after it. `PagingData.map` sees one message at a time, so that information has to arrive on the row. The 20-second grouping rule itself stays in `ChatItemMapper`.

### Mapping a paged list
`ChatItemMapper` holds per-message rules, applied with Paging's two transformations:

- `map { toMessageItem(row) }` builds each `ChatItem.Message`, including `isGrouped` from the row's next-message data.
- `insertSeparators { newer, older -> headerAbove(newer, older) }` inserts a `TimestampHeader` above a message when there is more than an hour since the older one. Paging calls it with `older = null` only once the oldest message has loaded, so the first message of the Chat still gets a header.

### UI
`ChatViewModel` exposes `Flow<PagingData<ChatItem>>`, cached in `viewModelScope`, separately from `ChatUiState`. The screen collects it with `collectAsLazyPagingItems()` and shows it in a `LazyColumn(reverseLayout = true)`, keyed by each message's `senderId` and `clientMessageId` (see [ADR-0001](0001-repository-dual-datasource-echo-pattern.md)).

## Alternatives
- **UUIDv7 `id` as order and cursor** (the first version): no per-Chat counter on the server, but gaps can only be guessed, and two messages in one millisecond from different servers have an arbitrary order.
- **Cursor read from the stored messages** (`MIN`/`MAX(seq)`): no sync table, but a live message stored ahead of a gap moves the newest `seq` past it, so the catch-up would skip the missing messages.
- **Drop the stored history when the newest page does not reach it**: no forward endpoint, but history is downloaded again after every gap.

## Consequences
- History of any length can be shown, new messages never cause repeated or skipped messages while scrolling, and a gap is detected exactly and filled.
- The server has to assign `seq` atomically per Chat, which serialises writes within a Chat.
- Room's generated `PagingSource` uses `LIMIT/OFFSET` locally. Room invalidates it on every write, so it does not drift, and an offset over local indexed rows is fast.
- `chat/domain` depends on `paging-common`, a plain Kotlin library, and has a `MessageWithNext` row model. Both are listed in the accepted deviations in [`architecture.md`](../architecture.md#accepted-deviations).
- Tests cover `ChatItemMapper`'s rules directly and its paging wiring with `asSnapshot()`, along with the `RemoteMediator`, catch-up and gap handling in the repository, and the paged query and sync state. The mock is exercised through the repository tests, not tested on its own.
