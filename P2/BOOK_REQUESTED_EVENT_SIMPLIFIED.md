# BookRequested Event - Simplified Implementation

## Summary
The `BOOK_REQUESTED` event has been modified to send only **bookId**, **authorName**, and **genreName** when a POST request creates a book in the command service.

## What Was Changed

### 1. New Event DTO Created
Created `BookRequestedEvent.java` in both command and query services with only 3 fields:
- `bookId` (String) - The ISBN of the book
- `authorName` (String) - The name of the first author
- `genreName` (String) - The name of the genre

**Location:**
- `P2/lms_books_command/src/main/java/pt/psoft/g1/psoftg1/bookmanagement/api/BookRequestedEvent.java`
- `P2/lms_books_query/src/main/java/pt/psoft/g1/psoftg1/bookmanagement/api/BookRequestedEvent.java`

### 2. Updated Publisher Interface
Modified `BookEventsPublisher.java` to return `BookRequestedEvent` instead of `BookViewAMQP`:
```java
BookRequestedEvent sendBookRequested(Book book);
```

### 3. Updated Publisher Implementation
Modified `BookEventsRabbitmqPublisherImpl.java` to:
- Create a simplified `BookRequestedEvent` with only the 3 required fields
- Extract the first author's name from the book's author list
- Publish this lightweight event to the message broker

### 4. Updated Event Consumers
Modified `BookRabbitmqController.java` in both command and query services to:
- Deserialize the incoming message as `BookRequestedEvent` instead of `BookViewAMQP`
- Log the 3 specific fields (bookId, authorName, genreName)

### 5. Trigger Event on Book Creation
Modified `BookServiceImpl.java` to publish the `BookRequested` event whenever a book is created via POST:
```java
if( savedBook!=null ) {
    bookEventsPublisher.sendBookCreated(savedBook);
    bookEventsPublisher.sendBookRequested(savedBook);  // NEW LINE
}
```

## Event Flow

1. **Client** sends POST request to `/api/books/{isbn}` with:
   - title
   - genre (genreName)
   - authors (list of author IDs)

2. **BookController** receives the request and calls `BookService.create()`

3. **BookService** creates the book and publishes TWO events:
   - `BOOK_CREATED` - Full book details (BookViewAMQP)
   - `BOOK_REQUESTED` - Simplified data (BookRequestedEvent with bookId, authorName, genreName)

4. **RabbitMQ** distributes the events to all listeners

5. **Consumers** (both command and query services) receive and log the simplified event

## JSON Structure

The `BOOK_REQUESTED` event now sends a minimal JSON payload:
```json
{
  "bookId": "978-0-123456-78-9",
  "authorName": "John Doe",
  "genreName": "Fiction"
}
```

Instead of the full BookViewAMQP structure with title, description, authorIds list, version, etc.

## Benefits

1. **Reduced Payload Size** - Only 3 fields instead of 7+ fields
2. **Clearer Intent** - Event name and data clearly indicate what was requested
3. **Performance** - Less data to serialize/deserialize and transmit
4. **Flexibility** - Can be used for analytics, recommendations, or tracking independently of the full book creation event

## Testing

To test, create a book using POST:
```bash
PUT /api/books/978-0-123456-78-9
Content-Type: application/json

{
  "title": "Sample Book",
  "genre": "Fiction",
  "authors": [1],
  "description": "A sample book"
}
```

You should see in the console logs:
```
Send Book Requested event to AMQP Broker: Sample Book
[x] Received Book Requested by AMQP:
    - Book ID: 978-0-123456-78-9
    - Author Name: John Doe
    - Genre Name: Fiction
[x] Book request logged successfully.
```

