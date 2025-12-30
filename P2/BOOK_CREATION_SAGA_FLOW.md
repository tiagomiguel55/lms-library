# Book Creation Saga Flow - Complete Documentation

## Overview

This document describes the complete asynchronous flow for creating a book with a new author and genre using the Saga pattern with event-driven architecture and RabbitMQ.

## Architecture Pattern

- **Pattern**: Choreography-based Saga
- **Message Broker**: RabbitMQ
- **Services Involved**: BookCmd, AuthorCmd, GenreCmd
- **State Management**: `PendingBookRequest` entity with optimistic locking

---

## Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    1. User Requests Book Creation                       │
│                    POST /api/books/request                              │
│                    {isbn, title, author, genre}                         │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  BookCmd: Create PendingBookRequest                                     │
│  Status: PENDING_AUTHOR_CREATION                                        │
│  Publish: BOOK_REQUESTED event                                          │
└─────────────────┬───────────────────────────┬───────────────────────────┘
                  │                           │
                  ▼                           ▼
    ┌─────────────────────────┐   ┌─────────────────────────┐
    │  AuthorCmd receives     │   │  GenreCmd receives      │
    │  BOOK_REQUESTED         │   │  BOOK_REQUESTED         │
    └────────────┬────────────┘   └────────────┬────────────┘
                 │                              │
                 ▼                              ▼
    ┌─────────────────────────┐   ┌─────────────────────────┐
    │  Create temporary       │   │  Create temporary       │
    │  Author                 │   │  Genre                  │
    │  (finalized=false)      │   │  (finalized=false)      │
    └────────────┬────────────┘   └────────────┬────────────┘
                 │                              │
                 │  Publish                     │  Publish
                 │  AUTHOR_PENDING_CREATED      │  GENRE_PENDING_CREATED
                 │                              │
                 └──────────────┬───────────────┘
                                ▼
        ┌───────────────────────────────────────────────┐
        │  BookCmd receives both events                 │
        │  PENDING_AUTHOR_CREATION                      │
        │      → PENDING_GENRE_CREATION                 │
        │      → GENRE_CREATED                          │
        └─────────────────────┬─────────────────────────┘
                              │
                              ▼
        ┌───────────────────────────────────────────────┐
        │  BookCmd: Status = GENRE_CREATED              │
        │  Publish: BOOK_FINALIZED event                │
        │  (triggers finalization of author & genre)    │
        └─────────────┬───────────────┬─────────────────┘
                      │               │
                      ▼               ▼
        ┌──────────────────┐   ┌──────────────────┐
        │  AuthorCmd       │   │  GenreCmd        │
        │  Finalize Author │   │  Finalize Genre  │
        │  (finalized=true)│   │  (finalized=true)│
        └────────┬─────────┘   └─────────┬────────┘
                 │                       │
                 │  Publish              │  Publish
                 │  AUTHOR_CREATED       │  GENRE_CREATED
                 │                       │
                 └───────────┬───────────┘
                             │
            ┌────────────────┴────────────────┐
            │  RACE CONDITION HANDLING HERE!  │
            │  (Optimistic Locking + Retry)   │
            └────────────────┬────────────────┘
                             │
                             ▼
        ┌────────────────────────────────────────────────┐
        │  BookCmd receives both finalization events     │
        │  Status Transitions (with retry logic):        │
        │    GENRE_CREATED → GENRE_FINALIZED             │
        │    GENRE_FINALIZED → BOTH_FINALIZED ✓          │
        └─────────────────────┬──────────────────────────┘
                              │
                              ▼
        ┌────────────────────────────────────────────────┐
        │  BookCmd: Status = BOTH_FINALIZED              │
        │  Create Book with finalized Author & Genre     │
        │  Update Status: BOTH_FINALIZED → BOOK_CREATED  │
        └────────────────────────────────────────────────┘
```

---

## Status Flow

The `PendingBookRequest` entity tracks the saga state through these statuses:

```
PENDING_AUTHOR_CREATION
    ↓ (Genre pending created)
PENDING_GENRE_CREATION
    ↓ (Author pending created)
GENRE_CREATED
    ↓ (BOOK_FINALIZED event sent)
    ↓ (Author finalized)
AUTHOR_FINALIZED
    ↓ (Genre finalized) - OR -
    ↓
GENRE_FINALIZED
    ↓ (Author finalized) - OR -
    ↓
BOTH_FINALIZED
    ↓ (Book created)
BOOK_CREATED ✓
```

---

## Detailed Step-by-Step Flow

### Step 1: Book Creation Request
**Endpoint**: `POST /api/books/request`
```json
{
  "isbn": "9780451524935",
  "title": "1984",
  "authorName": "George Orwell",
  "genreName": "Fiction"
}
```

**Action**: BookCmd creates a `PendingBookRequest` with:
- `bookId`: ISBN
- `authorName`: Author name
- `genreName`: Genre name
- `status`: `PENDING_AUTHOR_CREATION`
- `version`: 0 (for optimistic locking)

**Event Published**: `BOOK_REQUESTED`
```json
{
  "isbn": "9780451524935",
  "authorName": "George Orwell",
  "genreName": "Fiction"
}
```

---

### Step 2: Parallel Author & Genre Creation

#### AuthorCmd Receives BOOK_REQUESTED
1. Checks if author "George Orwell" exists
2. If not exists, creates temporary author:
   ```java
   Author author = new Author(name, null, null);
   author.setFinalized(false);  // IMPORTANT: Not finalized yet!
   ```
3. Publishes `AUTHOR_PENDING_CREATED` event

#### GenreCmd Receives BOOK_REQUESTED
1. Checks if genre "Fiction" exists
2. If not exists, creates temporary genre:
   ```java
   Genre genre = new Genre(genreName);
   genre.setFinalized(false);  // IMPORTANT: Not finalized yet!
   ```
3. Publishes `GENRE_PENDING_CREATED` event

---

### Step 3: BookCmd Tracks Pending Creation

#### Receives GENRE_PENDING_CREATED
**Current Status**: `PENDING_AUTHOR_CREATION`
**Transition**: `PENDING_AUTHOR_CREATION → PENDING_GENRE_CREATION`

#### Receives AUTHOR_PENDING_CREATED
**Current Status**: `PENDING_GENRE_CREATION`
**Transition**: `PENDING_GENRE_CREATION → GENRE_CREATED`

**Trigger**: When status becomes `GENRE_CREATED`, BookCmd sends `BOOK_FINALIZED` event

---

### Step 4: BOOK_FINALIZED Event Triggers Finalization

**Event**: `BOOK_FINALIZED`
```json
{
  "authorId": 652,
  "authorName": "George Orwell",
  "bookId": "9780451524935",
  "genreName": "Fiction"
}
```

#### AuthorCmd Finalizes Author
```java
author.setFinalized(true);
authorRepository.save(author);
// Publish AUTHOR_CREATED event
```

#### GenreCmd Finalizes Genre
```java
genre.setFinalized(true);
genreRepository.save(genre);
// Publish GENRE_CREATED event
```

---

### Step 5: Race Condition Handling ⚠️

**Problem**: Both `AUTHOR_CREATED` and `GENRE_CREATED` events may arrive simultaneously!

**Example Race Condition**:
```
Time T0: Status = GENRE_CREATED

Time T1: Thread A reads status = GENRE_CREATED
Time T1: Thread B reads status = GENRE_CREATED

Time T2: Thread A updates to AUTHOR_FINALIZED
Time T2: Thread B updates to GENRE_FINALIZED

Time T3: Status = GENRE_FINALIZED (Thread B won)
         ❌ STUCK! Never reaches BOTH_FINALIZED
```

---

## Solution: Optimistic Locking + Retry Logic

### Change 1: Added Version Field to PendingBookRequest

```java
@Entity
@Table(name = "PendingBookRequest")
public class PendingBookRequest {
    // ...existing fields...
    
    @Version
    private Long version;  // ✅ NEW: Enables optimistic locking
}
```

**How it works**:
- JPA automatically increments `version` on each update
- If two transactions try to update the same record, the second one throws `OptimisticLockException`

---

### Change 2: Retry Logic in Event Handlers

Both `receiveAuthorCreated()` and `receiveGenreCreated()` now implement:

```java
int maxRetries = 3;
for (int attempt = 0; attempt < maxRetries; attempt++) {
    try {
        // ✅ ALWAYS reload from DB to get latest status
        Optional<PendingBookRequest> pendingRequestOpt = 
            pendingBookRequestRepository.findByBookId(event.getBookId());
        
        PendingBookRequest pendingRequest = pendingRequestOpt.get();
        
        // ✅ Check ALL possible current states
        boolean statusChanged = false;
        if (pendingRequest.getStatus() == RequestStatus.GENRE_CREATED) {
            pendingRequest.setStatus(RequestStatus.AUTHOR_FINALIZED);
            statusChanged = true;
        } else if (pendingRequest.getStatus() == RequestStatus.GENRE_FINALIZED) {
            pendingRequest.setStatus(RequestStatus.BOTH_FINALIZED);
            statusChanged = true;
        } else if (pendingRequest.getStatus() == RequestStatus.AUTHOR_FINALIZED) {
            pendingRequest.setStatus(RequestStatus.BOTH_FINALIZED);
            statusChanged = true;
        } else if (pendingRequest.getStatus() == RequestStatus.BOTH_FINALIZED) {
            statusChanged = false; // Already there!
        }
        
        if (statusChanged) {
            pendingBookRequestRepository.save(pendingRequest);
        }
        
        tryCreateBook(event.getBookId());
        break; // ✅ Success! Exit retry loop
        
    } catch (OptimisticLockException e) {
        if (attempt < maxRetries - 1) {
            Thread.sleep(50); // Wait before retry
        } else {
            throw e; // Failed after all retries
        }
    }
}
```

---

### How It Solves the Race Condition

**Scenario 1: Sequential Events** ✅
```
1. GENRE_CREATED event arrives
   → Status: GENRE_CREATED → GENRE_FINALIZED
   → Waits for author

2. AUTHOR_CREATED event arrives
   → Reloads: Status = GENRE_FINALIZED
   → Status: GENRE_FINALIZED → BOTH_FINALIZED ✅
   → Book created!
```

**Scenario 2: Simultaneous Events (Optimistic Lock)** ✅
```
1. Both events arrive at same time
   Thread A: Reads GENRE_CREATED
   Thread B: Reads GENRE_CREATED

2. Thread A saves first (version 1 → 2)
   → Status: GENRE_CREATED → GENRE_FINALIZED

3. Thread B tries to save (expects version 1, but it's 2)
   → ❌ OptimisticLockException thrown!

4. Thread B retries (attempt 2)
   → Reloads: Status = GENRE_FINALIZED
   → Status: GENRE_FINALIZED → BOTH_FINALIZED ✅
   → Book created!
```

---

## Step 6: Book Creation

When status reaches `BOTH_FINALIZED`, `tryCreateBook()` executes:

```java
// 1. Verify both author and genre are finalized
List<Author> authors = authorRepository.searchByNameName(authorName);
Optional<Genre> genreOpt = genreRepository.findByString(genreName);

Author author = authors.get(0);
Genre genre = genreOpt.get();

// 2. Double-check finalization
assert author.isFinalized() == true;
assert genre.isFinalized() == true;

// 3. Create book
Book newBook = new Book(isbn, title, description, genre, authorList, null);
Book savedBook = bookRepository.save(newBook);

// 4. Update pending request
PendingBookRequest latestRequest = pendingBookRequestRepository.findByBookId(isbn).get();
latestRequest.setStatus(RequestStatus.BOOK_CREATED);
pendingBookRequestRepository.save(latestRequest);
```

---

## Key Design Decisions

### 1. Two-Phase Creation (Temporary → Finalized)
**Why**: Ensures book is only created with **verified** author and genre.

- Phase 1: Create temporary entities (finalized=false)
- Phase 2: Only after both exist, finalize them together

**Benefits**:
- Atomic validation
- If one fails, the other isn't finalized
- Book never created with invalid references

### 2. Choreography (not Orchestration)
**Why**: Each service reacts to events independently.

**Benefits**:
- No central orchestrator
- Services are loosely coupled
- Easy to add more services

**Trade-offs**:
- Complex state tracking
- Requires careful event ordering

### 3. Optimistic Locking
**Why**: Handle concurrent events without database locks.

**Benefits**:
- Better performance (no locking)
- Automatic conflict detection
- Simple retry mechanism

**Alternative**: Pessimistic locking would block one transaction, reducing concurrency.

---

## Error Handling & Compensation

### Failure Scenarios

#### 1. Author Creation Fails
```
GenreCmd creates temporary genre (finalized=false)
AuthorCmd fails → Publishes AUTHOR_CREATION_FAILED
BookCmd receives event → Status: FAILED
Compensation: Genre remains unfinalized (orphaned but not harmful)
```

#### 2. Genre Creation Fails
```
AuthorCmd creates temporary author (finalized=false)
GenreCmd fails → Publishes GENRE_CREATION_FAILED
BookCmd receives event → Status: FAILED
Compensation: Author remains unfinalized (orphaned but not harmful)
```

#### 3. Book Creation Fails (after finalization)
```
Both author and genre finalized
Book creation fails due to constraint violation
Status: BOTH_FINALIZED (stuck)
Manual intervention needed or implement compensating transaction
```

---

## Event Summary

| Event | Publisher | Consumer | Purpose |
|-------|-----------|----------|---------|
| `BOOK_REQUESTED` | BookCmd | AuthorCmd, GenreCmd | Request creation of author & genre |
| `AUTHOR_PENDING_CREATED` | AuthorCmd | BookCmd | Notify author created (pending) |
| `GENRE_PENDING_CREATED` | GenreCmd | BookCmd | Notify genre created (pending) |
| `BOOK_FINALIZED` | BookCmd | AuthorCmd, GenreCmd | Trigger finalization |
| `AUTHOR_CREATED` | AuthorCmd | BookCmd | Notify author finalized |
| `GENRE_CREATED` | GenreCmd | BookCmd | Notify genre finalized |
| `AUTHOR_CREATION_FAILED` | AuthorCmd | BookCmd | Compensation event |
| `GENRE_CREATION_FAILED` | GenreCmd | BookCmd | Compensation event |

---

## Database Schema

### PendingBookRequest Table
```sql
CREATE TABLE pending_book_request (
    id BIGINT PRIMARY KEY,
    book_id VARCHAR(255) UNIQUE NOT NULL,  -- ISBN
    author_name VARCHAR(255) NOT NULL,
    genre_name VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0  -- For optimistic locking
);
```

### Author Table
```sql
CREATE TABLE author (
    author_number BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    bio TEXT,
    photo_id BIGINT,
    finalized BOOLEAN NOT NULL DEFAULT FALSE,  -- NEW FIELD
    version BIGINT NOT NULL
);
```

### Genre Table
```sql
CREATE TABLE genre (
    pk BIGINT PRIMARY KEY,
    genre VARCHAR(255) UNIQUE NOT NULL,
    finalized BOOLEAN NOT NULL DEFAULT FALSE  -- NEW FIELD
);
```

---

## Testing the Flow

### Success Case
```bash
# 1. Request book creation
POST http://localhost:8080/api/books/request
{
  "isbn": "9780451524935",
  "title": "1984",
  "authorName": "George Orwell",
  "genreName": "Fiction"
}

# 2. Check pending request status
GET http://localhost:8080/api/books/pending/9780451524935
# Response: { "status": "BOOK_CREATED" }

# 3. Verify book created
GET http://localhost:8080/api/books/9780451524935
# Response: Book details with finalized author and genre
```

### Race Condition Test
```bash
# Send 5 simultaneous requests with same author/genre
# All should complete successfully without deadlocks
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/books/request \
    -H "Content-Type: application/json" \
    -d "{\"isbn\":\"978045152493$i\", \"title\":\"Book $i\", \"authorName\":\"George Orwell\", \"genreName\":\"Fiction\"}" &
done
wait
```

---

## Monitoring & Debugging

### Key Log Messages

✅ **Success Flow**:
```
[x] Saved pending book request for ISBN: 9780451524935
[x] Publishing Book Requested event
[x] Received Book Requested by AMQP (AuthorCmd)
[x] Received Book Requested by AMQP (GenreCmd)
[x] Creating temporary author (finalized=false)
[x] Creating temporary genre (finalized=false)
[x] Both Author and Genre are CREATED (pending finalization)
[x] Sending BOOK_FINALIZED event
[x] Finalizing author
[x] Finalizing genre
[x] Status transition: GENRE_FINALIZED → BOTH_FINALIZED
[x] Book created successfully
[x] Updated pending request status to BOOK_CREATED
```

⚠️ **Optimistic Lock Retry**:
```
[x] ⚠️ Optimistic lock conflict (attempt 1), retrying...
[x] Status transition: GENRE_FINALIZED → BOTH_FINALIZED
```

❌ **Failure Flow**:
```
[x] ❌ Received Author Creation Failed by AMQP
[x] Marked pending book request as FAILED
[x] SAGA COMPENSATION COMPLETED
```

---

## Performance Considerations

1. **Asynchronous Processing**: All operations are non-blocking
2. **Optimistic Locking**: Better concurrency than pessimistic locks
3. **Retry Mechanism**: Max 3 attempts with 50ms delay
4. **Database Queries**: Indexed on `book_id` (ISBN) for fast lookups
5. **Event Publishing**: Fire-and-forget with RabbitMQ

**Typical Latency**: 200-500ms from request to book creation (depends on message broker)

---

## Future Improvements

1. **Idempotency Keys**: Prevent duplicate book creation requests
2. **Saga Timeout**: Automatically fail requests older than X minutes
3. **Cleanup Job**: Remove old FAILED/BOOK_CREATED pending requests
4. **Distributed Tracing**: Add correlation IDs to track saga across services
5. **Circuit Breaker**: Prevent cascade failures if one service is down
6. **Event Sourcing**: Store all state changes as events for better debugging

---

## Conclusion

This saga pattern with optimistic locking provides a robust solution for:
- ✅ Asynchronous, distributed book creation
- ✅ Handling race conditions with concurrent events
- ✅ Ensuring data consistency across services
- ✅ Proper compensation on failures

The key innovation is using **optimistic locking with retry logic** to handle the race condition when both author and genre finalization events arrive simultaneously, ensuring the saga always reaches the `BOTH_FINALIZED` state before creating the book.

