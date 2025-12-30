# Genre Creation Pattern Implementation

## Summary

Genre creation now follows the same SAGA pattern as Author creation. Both Author and Genre must be created/verified before a Book can be created. **Both AuthorCmd and GenreCmd listen to the same BOOK_REQUESTED event.**

## Changes Made

### 1. **Genre Model** (`Genre.java`)
- Added `finalized` boolean flag (default: false)
- Genre is created with `finalized=false` and marked as `finalized=true` when book creation succeeds

### 2. **Genre Events** (`GenreEvents.java`)
Added new event types:
- `GENRE_PENDING_CREATED` - Published when genre is created/found
- `GENRE_CREATION_FAILED` - Published when genre creation fails (SAGA compensation)

### 3. **Genre Service** (`GenreService.java` & `GenreServiceImpl.java`)
- Added `markGenreAsFinalized(String genreName)` method
- Marks genre as finalized after successful book creation

### 4. **Genre Events Publisher** (`GenreEventsPublisher.java`)
Added methods:
- `sendGenrePendingCreated(String genreName, String bookId, String authorName)`
- `sendGenreCreationFailed(String bookId, String authorName, String genreName, String errorMessage)`
- `sendGenreCreated(Genre genre, String bookId)`

### 5. **Genre Event DTOs**
Created:
- `GenrePendingCreated.java` - Event when genre is ready
- `GenreCreationFailed.java` - Event when genre creation fails

### 6. **GenreRabbitmqController** (NEW)
Handles Genre-related events:
- `receiveBookRequested()` - **Listens to BOOK_REQUESTED event**, creates/finds genre and publishes GENRE_PENDING_CREATED
- `receiveGenreFinalized()` - Marks genre as finalized after book creation

### 7. **BookServiceImpl**
- Removed local genre creation
- **Only publishes ONE event: BOOK_REQUESTED** (both AuthorCmd and GenreCmd listen to it)
- Waits for both AUTHOR_PENDING_CREATED and GENRE_PENDING_CREATED before creating book

### 8. **PendingBookRequest**
Added new statuses:
- `PENDING_GENRE_CREATION` - Waiting for genre
- `GENRE_CREATED` - Genre is ready

### 9. **BookRabbitmqController**
- Handles both `AUTHOR_PENDING_CREATED` and `GENRE_PENDING_CREATED` events
- `tryCreateBook()` method checks if both author and genre are ready
- Only creates book when both dependencies are satisfied
- Added `receiveGenreCreationFailed()` handler for SAGA compensation

### 10. **RabbitMQ Configuration**
Added queues and bindings:
- `autoDeleteQueue_Genre_Pending_Created`
- `autoDeleteQueue_Genre_Creation_Failed`
- `autoDeleteQueue_Genre_Finalized`

## Event Flow

```
1. POST /api/books → BookCmd receives request
2. BookCmd saves PendingBookRequest (status: PENDING_AUTHOR_CREATION)
3. BookCmd publishes BOOK_REQUESTED → LMS.books exchange
4. AuthorCmd receives BOOK_REQUESTED event
   - Creates/finds author with finalized=false
   - Publishes AUTHOR_PENDING_CREATED → BookCmd
5. GenreCmd receives BOOK_REQUESTED event (NEW!)
   - Creates/finds genre with finalized=false
   - Publishes GENRE_PENDING_CREATED → BookCmd
6. BookCmd receives AUTHOR_PENDING_CREATED (status: AUTHOR_CREATED)
7. BookCmd receives GENRE_PENDING_CREATED (status: GENRE_CREATED)
8. BookCmd checks if both are ready → Creates Book
9. BookCmd publishes BOOK_FINALIZED → AuthorCmd
10. AuthorCmd marks author as finalized=true
11. AuthorCmd publishes AUTHOR_CREATED → BookQuery
12. GenreCmd marks genre as finalized=true
13. GenreCmd publishes GENRE_CREATED → BookQuery
```

## Key Architecture Points

### Simplified Event Publishing
- **BookCmd only publishes BOOK_REQUESTED** - no separate Genre events needed
- Both AuthorCmd and GenreCmd are subscribers to the BOOK_REQUESTED event
- This follows the pub-sub pattern where multiple services can react to the same event

### Parallel Processing
- AuthorCmd and GenreCmd process the request in parallel
- Both send their respective PENDING_CREATED events independently
- BookCmd waits for both before creating the book

### No Direct Communication
- BookCmd doesn't need to know about AuthorCmd or GenreCmd
- It just publishes events and waits for responses
- Clean separation of concerns

## SAGA Compensation

If Genre creation fails:
1. GenreCmd publishes GENRE_CREATION_FAILED
2. BookCmd receives event and marks PendingBookRequest as FAILED
3. Book creation is aborted

If Author creation fails:
1. AuthorCmd publishes AUTHOR_CREATION_FAILED
2. BookCmd receives event and marks PendingBookRequest as FAILED
3. Book creation is aborted

## Testing with Postman

### Example 1: Create book with new author and new genre
```json
POST http://localhost:8080/api/books

{
  "bookId": "9780596009205",
  "authorName": "Joshua Bloch",
  "genreName": "Programming"
}
```

**Expected Flow:**
1. BOOK_REQUESTED event sent to LMS.books exchange
2. Both AuthorCmd and GenreCmd receive the event
3. Both process in parallel (with 10-second delays to check DB)
4. AUTHOR_PENDING_CREATED and GENRE_PENDING_CREATED sent to BookCmd
5. Book is created once both are received
6. BOOK_FINALIZED sent to both services
7. Both Author and Genre marked as finalized=true

### Example 2: Create book with existing author and new genre
```json
{
  "bookId": "9780134685991",
  "authorName": "José Saramago",
  "genreName": "Fiction"
}
```

### Example 3: Create book with new author and existing genre
```json
{
  "bookId": "9781449355739",
  "authorName": "Martin Fowler",
  "genreName": "Literatura"
}
```

## Database Verification

During the 10-second wait period, you can check:

**Author Table:**
```sql
SELECT author_number, name, finalized FROM author;
```

**Genre Table:**
```sql
SELECT pk, genre, finalized FROM genre;
```

Both should show `finalized=false` initially, then `finalized=true` after book creation completes.

## Benefits

1. **Consistency**: Genre follows the same pattern as Author
2. **Isolation**: GenreCmd is responsible for genre lifecycle
3. **Reliability**: SAGA pattern ensures proper compensation on failures
4. **Traceability**: All events are logged and can be monitored
5. **Scalability**: Services are decoupled and can scale independently
6. **Simplicity**: Only ONE event (BOOK_REQUESTED) needs to be published by BookCmd
7. **Flexibility**: Easy to add more services that need to react to book requests
