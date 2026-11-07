# ADR-0003: Cursor-based paging of message history, with Room as the paging source

## Status
Accepted. Builds on [ADR-0001](0001-repository-dual-datasource-echo-pattern.md). Replaces the fixed window of the newest 500 messages.

## Context
A chat conversation has no limit on length. A production chat screen cannot load the whole history into memory, map all of it on every change, or fetch it from the server in one request. It loads the newest messages first and older ones as the user scrolls up.

Paging a chat by page number (offset) breaks as soon as new messages arrive: every message shifts down, so the next page repeats messages the user has already seen. It is also slow on large histories, because the database has to skip every row before the offset. The standard for chat is cursor paging: "the 50 messages before this one".

A cursor must be unique and strictly ordered. Timestamps are neither: two messages can share a millisecond, and a page boundary that falls between them skips or repeats one. `sequenceId` (see ADR-0001) is unique and increases by one with no gaps, so it is the cursor.

## Decision

### Remote contract
`ChatRemoteDataSource.fetchMessages(beforeSequenceId: Long?, limit: Int)` returns up to `limit` confirmed messages older than `beforeSequenceId`, newest first. `null` means "the newest messages". The mock answers it by filtering and slicing the list in `MockData`.

### Room is the paging source, with a `RemoteMediator`
Paging 3 reads from Room, and a `RemoteMediator` fetches from the server into Room when the user scrolls past what is stored. This keeps Room the single source of truth: the paged list, optimistic sends and echoes all go through the same table.

- **REFRESH** fetches the newest page.
- **APPEND** (scrolling up into older history) fetches the page before the smallest `sequenceId` stored in Room.
- **PREPEND** ends at once. New messages arrive through `syncIncomingMessages()`, not through paging.
- Sequences start at 1 with no gaps, so history is complete when the smallest stored `sequenceId` is 1. No separate table of paging keys is needed.

Each page is written in one transaction, using the same insert as incoming messages.

### The paged query carries each message's next neighbour
Room's `PagingSource` reads messages newest first: confirmed messages by `sequenceId`, then pending messages by device timestamp. Each row also returns the sender and timestamp of the **next** message in conversation order. This is computed by the query and not stored:

- For a confirmed message, the next message is the one with `sequenceId + 1`. For the newest confirmed message, it is the oldest pending message, if there is one.
- For a pending message, it is the next pending message by device timestamp.

Each row is a `MessageWithNext`. The query exists for the bubble tail, which is drawn on the last bubble of a run from one sender, so it depends on the message after it. `PagingData.map` sees one message at a time, so that information has to arrive on the row. The 20-second grouping rule itself stays in `ChatItemMapper`.

### Mapping a paged list
`ChatItemMapper` holds per-message rules, applied with Paging's two transformations:

- `map { toMessageItem(row) }` builds each `ChatItem.Message`, including `isGrouped` from the row's next-message data.
- `insertSeparators { newer, older -> headerAbove(newer, older) }` inserts a `TimestampHeader` above a message when there is more than an hour since the older one. Paging calls it with `older = null` only once the oldest message has loaded, so the first message of the conversation still gets a header.

### UI
`ChatViewModel` exposes `Flow<PagingData<ChatItem>>`, cached in `viewModelScope`, separately from `ChatUiState`. The screen collects it with `collectAsLazyPagingItems()` and shows it in a `LazyColumn(reverseLayout = true)`, keyed by each message's `localId`, or its server `id` when it has none.

## Consequences
- History of any length can be shown, and new messages arriving never cause repeated or skipped messages while scrolling.
- Server requests are indexed lookups by `sequenceId`, however long the history grows.
- Room's generated `PagingSource` uses `LIMIT/OFFSET` locally. Room invalidates it on every write, so it does not drift, and an offset over local indexed rows is fast. A custom `PagingSource` keyed by `sequenceId` can replace it if local histories become very large.
- `chat/domain` depends on `paging-common`, a plain Kotlin library, and has a `MessageWithNext` row model. Both are listed in the accepted deviations in [`architecture.md`](../architecture.md#accepted-deviations).
- `ChatUiState` no longer holds the message list.
- Tests cover `ChatItemMapper`'s rules directly and its paging wiring with `asSnapshot()`, along with the `RemoteMediator`, the paged query and the mock's paging.
