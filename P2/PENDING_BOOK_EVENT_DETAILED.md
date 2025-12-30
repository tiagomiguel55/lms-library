# PendingBookEvent - Detailed Explanation

## Overview
The `PendingBookEvent` is a crucial component of the query service's eventual consistency mechanism. It handles out-of-order event processing in a distributed system where events may arrive in different orders across different replicas.

## Problem It Solves

In a microservices architecture with event-driven communication:
1. **Book Finalized Event** arrives before the **Genre** is created
2. **Book Finalized Event** arrives before the **Author** is created
3. Events arrive at different replicas in different orders

Without PendingBookEvent, books would be lost or created incompletely.

## How It Works

### 1. **Entity Definition** (`PendingBookEvent.java`)
```
PendingBookEvent stores:
- bookId: The ISBN of the book being finalized
- genreName: The genre name that may not exist yet
- authorId: The author ID that may not exist yet
- authorName: The author name (cached in case author service is slow)
- title: The book title
- description: The book description
```

### 2. **Event Processing Flow**

#### Scenario: Genre arrives AFTER BookFinalized

```
Timeline:
1. BookFinalized Event arrives
   - Genre NOT found in database
   - BookFinalized event is stored as PendingBookEvent
   
2. GenreCreated Event arrives
   - Genre is created
   - processPendingBooksForGenre() is called
   - If author exists, book is created
   - If author still missing, PendingBookEvent stays for later
   
3. AuthorCreated Event arrives
   - Author is created
   - processPendingBooksForAuthor() is called
   - Genre exists now, so book can be fully created
   - PendingBookEvent is deleted
```

#### Scenario: Author arrives AFTER BookFinalized

```
Timeline:
1. BookFinalized Event arrives
   - Genre exists
   - Author NOT found in database
   - BookFinalized event is stored as PendingBookEvent (NEW FIX)
   
2. AuthorCreated Event arrives
   - Author is created
   - processPendingBooksForAuthor() is called
   - Genre exists, author exists, book can be created
   - PendingBookEvent is deleted
```

## Key Methods in BookServiceImpl

### 1. `handleBookFinalized(BookFinalizedEvent event)`
**Purpose**: Handle incoming BookFinalized events

**Logic**:
```
if book already exists:
  ✅ Log it and return
else:
  if genre NOT found:
    📝 Save as pending (waiting for genre)
    return
  if author NOT found:
    📝 Save as pending (waiting for author) [NEW FIX]
    return
  if both exist:
    ✅ Create the book immediately
```

**Important Fix**: Previously, when the author was missing, we would just log a warning and return WITHOUT storing it as pending. This meant the book would be lost forever. Now we properly store it as pending.

### 2. `processPendingBooksForGenre(String genreName)`
**Purpose**: Called when a new genre is created

**Logic**:
```
Get all pending books waiting for this genre
for each pending book:
  if author still not available:
    ⏳ Skip (keep pending for later)
  if author is available:
    ✅ Create the book
    Delete pending event
```

### 3. `processPendingBooksForAuthor(Long authorId)`
**Purpose**: Called when a new author is created

**Logic**:
```
Get all pending books waiting for this author
for each pending book:
  if genre still not available:
    ⏳ Skip (keep pending for later)
  if genre is available:
    ✅ Create the book
    Delete pending event
```

## Multiple Replicas Scenario

When you have multiple replicas (replica1, replica2) with load-balanced RabbitMQ queues:

```
Command Service                 RabbitMQ                 Query Replicas
                                                        
AuthorCreated ──┐
                ├─→ authors.exchange ──→ replica1: AuthorRabbitmqController
                │                    └─→ replica2: AuthorRabbitmqController
GenreCreated ──┤
                └─→ genres.exchange ──→ replica1: GenreRabbitmqController
                                    └─→ replica2: GenreRabbitmqController

BookFinalized ──→ books.exchange ──→ replica1: BookRabbitmqController
                                  └─→ replica2: BookRabbitmqController
```

**Key Point**: Each replica has its own database. When replica1 receives BookFinalized but Author hasn't been saved yet:
1. BookFinalized stored as PendingBookEvent in replica1's DB
2. AuthorCreated arrives at replica1's AuthorRabbitmqController
3. Author is saved to replica1's DB
4. `processPendingBooksForAuthor()` is called
5. Book is created in replica1's DB using the newly available author

## Database Schema

```sql
CREATE TABLE PendingBookEvent (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bookId VARCHAR(255) UNIQUE NOT NULL,
  genreName VARCHAR(255) NOT NULL,
  authorId BIGINT NOT NULL,
  authorName VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(1000)
);
```

The `UNIQUE` constraint on `bookId` prevents duplicate pending events in concurrent scenarios.

## Transaction Management

**Important**: The AuthorRabbitmqController uses `@Transactional`:

```java
@RabbitListener(queues = "#{autoDeleteQueue_Author_Created.name}")
@Transactional
public void receiveAuthorCreatedMsg(Message msg) {
    // ... save author ...
    // ... process pending books ...
}
```

This ensures:
1. Author is saved to the database
2. Transaction is committed (visible to other transactions)
3. `processPendingBooksForAuthor()` can then find the author

## Error Handling

```java
catch (DataIntegrityViolationException e) {
    // Duplicate pending event - normal in concurrent scenarios
    // One thread wins, others silently fail
    System.out.println("Pending book event already stored");
}
```

This gracefully handles race conditions where multiple message handlers try to store the same pending event.

## Advantages

✅ **Handles Out-of-Order Events**: Works regardless of event arrival order
✅ **Distributed System Safe**: Works across multiple replicas with separate databases
✅ **Fault Tolerant**: Lost messages can be recovered by periodic cleanup jobs
✅ **No Lost Data**: Books won't disappear due to timing issues
✅ **Transactional**: Uses database constraints to prevent duplicates

## Potential Improvements

1. **Cleanup Job**: Add a scheduled task to clean up very old pending events
2. **Retry Logic**: Add exponential backoff for persistent failures
3. **Dead Letter Queue**: Store events that can't be processed after N retries
4. **Monitoring**: Alert when pending events stay pending too long

## Testing Scenario

```
1. Start replica1 and replica2
2. Create an author (ID: 1, Name: "John Doe")
3. Create a genre ("Science Fiction")
4. Finalize a book (ISBN: "123-456", Author ID: 1, Genre: "Science Fiction")

Expected logs on replica1:
[QUERY] 📥 Received Book Finalized: 123-456
[QUERY] ⚠️ Author not found for finalized book (ID: 1)
[QUERY] 📝 Stored pending book event, waiting for author: John Doe
...
[QUERY] 📥 Received Author Created: John Doe (ID: 1)
[QUERY] ✅ Author saved in query model: John Doe (ID: 1)
[QUERY] 🔍 Checking for pending books waiting for author ID: 1
[QUERY] 🔄 Processing 1 pending book events for author ID: 1
[QUERY] ✅ Pending book finalized and created: 123-456 with author: John Doe and genre: Science Fiction
```

