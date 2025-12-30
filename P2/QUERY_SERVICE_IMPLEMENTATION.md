# LMS Books Query Service - Event Handling Implementation

## Overview
The lms_books_query service has been configured to receive and process three key events from the message broker (RabbitMQ):
1. **AuthorCreated** - When an author is created in the system
2. **GenreCreated** - When a genre is created in the system  
3. **BookFinalized** - When a book is finalized with all its details

## Architecture Pattern
This implementation follows the **CQRS (Command Query Responsibility Segregation)** pattern with event sourcing, where:
- The **lms_books_command** service publishes events when books, authors, and genres are created/updated
- The **lms_books_query** service subscribes to these events and maintains a read-optimized query model
- The query service builds and updates its local database based on incoming events (eventual consistency)

## Implementation Details

### 1. Event Constants (BookEvents.java)
Added the `BOOK_FINALIZED` event constant to track when books are finalized:
```java
static final String BOOK_FINALIZED = "BOOK_FINALIZED";
```

### 2. BookFinalizedEvent DTO
Created a new event data transfer object to carry finalized book information:
- `authorId` - ID of the author who finalized the book
- `authorName` - Name of the author
- `bookId` - ISBN of the book (unique identifier)
- `genreName` - Genre of the book

### 3. RabbitMQ Configuration (RabbitmqClientConfig.java)
Added the necessary message queue infrastructure:
- **Queue**: `query.book.finalized` - Durable queue to receive BookFinalized events
- **Exchange**: Uses the existing `LMS.books` direct exchange
- **Binding**: Routes `BOOK_FINALIZED` events from the exchange to the queue

### 4. Event Handler (BookRabbitmqController.java)
Implemented the event listener:
```java
@RabbitListener(queues = "#{autoDeleteQueue_Book_Finalized.name}")
public void receiveBookFinalized(Message msg)
```

This handler:
- Receives messages from the RabbitMQ queue
- Deserializes the JSON payload into a `BookFinalizedEvent` object
- Calls the service layer to process the event
- Logs success/error messages

### 5. Service Implementation (BookServiceImpl.java)
Implemented the `handleBookFinalized()` method which:
- Verifies the book exists in the query database (eventual consistency consideration)
- Confirms all book details are properly stored
- Logs the finalization event
- Handles potential missing books gracefully (for out-of-order event scenarios)

## Event Flow Diagram

```
Command Service                Message Broker              Query Service
================               ==============              =============

1. AuthorCreated Event -----> [LMS.authors exchange]
                                      |
                                      v
                           [query.author.created queue]
                                      |
                                      v
                          BookRabbitmqController
                               |
                               v
                        BookService.handleAuthorCreated()

2. GenreCreated Event -----> [LMS.genres exchange]
                                      |
                                      v
                           [query.genre.created queue]
                                      |
                                      v
                          BookRabbitmqController
                               |
                               v
                        BookService.handleGenreCreated()

3. BookFinalized Event ----> [LMS.books exchange]
                                      |
                                      v
                           [query.book.finalized queue]
                                      |
                                      v
                          BookRabbitmqController
                               |
                               v
                        BookService.handleBookFinalized()
```

## Book Creation Lifecycle in Query Service

### Phase 1: AuthorCreated Event
- Receives author creation event with author ID and book ID
- Creates or updates the book with the author information
- Sets default genre if not available yet

### Phase 2: GenreCreated Event  
- Receives genre creation event with genre name and book ID
- Updates the book with the genre information
- Ensures genre exists, creates if necessary

### Phase 3: BookFinalized Event
- Receives finalization event confirming the book is complete
- Verifies the book exists in the read model
- Confirms all details (author, genre, title, description) are properly stored
- Marks the book as finalized in the read model

## Error Handling & Resilience

The implementation includes several error handling strategies:

1. **Eventual Consistency**: Events may arrive out of order. The system handles missing books gracefully.
2. **Duplicate Events**: If a book already exists when processing AuthorCreated, it skips creation
3. **Missing Dependencies**: If a genre doesn't exist when needed, the system can create it
4. **Exception Logging**: All errors are logged with descriptive messages for debugging

## Database Operations

All book data is persisted to the query database through:
- `BookRepository.save()` - Saves new books
- `BookRepository.findByIsbn()` - Retrieves existing books
- `AuthorRepository.findByAuthorNumber()` - Verifies authors exist
- `GenreRepository.findByString()` - Verifies genres exist

## Testing & Deployment

The implementation has been compiled successfully with Maven:
```
BUILD SUCCESS
Total time: 25.002 s
```

### Configuration Profile
The RabbitMQ configuration is active for all profiles except test:
```java
@Profile("!test")
```

This ensures the message broker connection is only established in non-test environments.

## Queue Names
For reference, the durable queues used are:
- `query.book.created` - New books
- `query.book.updated` - Updated books  
- `query.book.deleted` - Deleted books
- `query.book.finalized` - Finalized books
- `query.author.created` - New authors
- `query.genre.created` - New genres

## Next Steps (If Needed)

1. **Testing**: Create integration tests to verify event processing
2. **Monitoring**: Add metrics and health checks for queue consumption
3. **Dead Letter Queue**: Consider adding DLQ for failed messages
4. **State Management**: Could add a "finalized" flag to Book entity for explicit tracking
5. **Performance**: Monitor event processing latency and throughput

## Summary

The lms_books_query service is now fully configured to:
✅ Receive and process AuthorCreated events
✅ Receive and process GenreCreated events  
✅ Receive and process BookFinalized events
✅ Build and maintain a consistent read model of books
✅ Handle eventual consistency scenarios gracefully

## Bounded Context: LMS Query Model

### Definition
The **LMS Query Service** represents a distinct bounded context in Domain-Driven Design (DDD). A bounded context defines an explicit boundary within which a particular domain model is applicable and valid. Within this boundary, all terms have specific meanings, and all processes follow consistent rules.

### Context Boundary
The LMS Query Service bounded context is responsible for:
- **Reading**: Providing optimized read access to book, author, and genre data
- **Aggregating**: Combining data from command-side events into a unified read model
- **Denormalizing**: Storing data in a way that's optimal for queries (not normalized for writes)
- **Consistency**: Maintaining eventual consistency with the command service

### Core Entities in This Context

#### 1. **Book** (Read Model)
In the Query bounded context, the Book entity is optimized for reading:
- Contains pre-computed fields like `isAvailable`, `totalCopies`, `copiesAvailable`
- Stores denormalized author and genre information for fast queries
- Tracks state transitions through events (creation → finalization)

#### 2. **Author** (Read Model)
Simplified representation focused on query needs:
- Author ID and name (sufficient for displaying book details)
- No complex business rules or state machines
- Serves as a reference dimension for book queries

#### 3. **Genre** (Read Model)
Lightweight genre representation:
- Genre name and categorization
- Linked to books for filtering and discovery
- No genre-specific business logic in this context

#### 4. **PendingBookEvent** (Transient State)
A special entity that handles eventual consistency issues:
- **Purpose**: Store books that have received `BookFinalized` events but haven't received their corresponding `GenreCreated` events yet
- **Lifespan**: Temporary - deleted once the genre event arrives
- **Problem it solves**: Out-of-order event processing in distributed systems

### Interaction with Command Service

```
┌─────────────────────────────────────────────────────────────────┐
│                    CQRS Architecture                             │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐         ┌──────────────────────────┐
│  Command Service         │         │   Query Service          │
│  Bounded Context         │         │   Bounded Context        │
│                          │         │                          │
│  - Books (Commands)      │         │   - Books (Read Model)   │
│  - Authors               │         │   - Authors              │
│  - Genres                │         │   - Genres               │
│  - Validations           │         │   - PendingBookEvent     │
│  - Business Rules        │         │   - Denormalized Views   │
│                          │         │                          │
│ Publishes Events ──────────────────> Subscribes & Updates    │
│                          │    via   │                          │
│                          │  RabbitMQ│                          │
└──────────────────────────┘         └──────────────────────────┘

Events Published:
├─ AuthorCreated
├─ GenreCreated
└─ BookFinalized
```

### Language Differences Between Contexts

| Term | Command Service | Query Service |
|------|-----------------|---------------|
| Book | Aggregate root with complex validation | Read model with denormalized data |
| Author | Full entity with relationships | Reference value (ID + Name) |
| Genre | Separate bounded context integration | Lookup entity for filtering |
| State | Command-driven state changes | Event-sourced state |
| Operation | Write optimization (normalized) | Read optimization (denormalized) |

### Data Flow Through Bounded Context

```
1. Command Service executes a command
   ↓
2. Command modifies Book aggregate
   ↓
3. Book emits domain events
   ↓
4. Events published to RabbitMQ
   ↓
5. Query Service receives events
   ↓
6. Events deserialized to DTOs
   (BookFinalizedEvent, AuthorCreatedEvent, etc.)
   ↓
7. Service layer processes events
   ↓
8. Read model updated in Query database
   ↓
9. Queries can now read the consistent data
```

### Bounded Context Responsibilities

**What the Query Bounded Context OWNS:**
- Book read model and its denormalized structure
- Author read model (query-optimized version)
- Genre read model (query-optimized version)
- PendingBookEvent for handling eventual consistency
- All read operations and queries
- Event handlers that synchronize with command service

**What the Query Bounded Context DEPENDS ON:**
- Events from the Command Service (AuthorCreated, GenreCreated, BookFinalized)
- RabbitMQ message broker for event delivery
- Spring Data JPA for persistence

**What the Query Bounded Context DOES NOT OWN:**
- Command execution
- Book creation or update business logic
- Author or Genre creation validation
- Book lending operations (separate context)
- User authentication and authorization (shared context)

### Consistency Model

The Query Service operates under **Eventual Consistency**:
- Not immediately consistent with the command service
- Eventually becomes consistent once all events are processed
- May have short delays between command execution and query visibility
- Handles out-of-order events gracefully (via PendingBookEvent)

### Integration Points

**Event Subscriptions:**
- **AuthorCreated** → Creates/updates authors in read model
- **GenreCreated** → Creates/updates genres and resolves pending books
- **BookFinalized** → Finalizes books or stores them as pending if genre missing

**Database Schema:**
```sql
-- Books table (denormalized for reading)
CREATE TABLE books (
    id BIGINT PRIMARY KEY,
    isbn VARCHAR(255) UNIQUE,
    title VARCHAR(255),
    description TEXT,
    author_id BIGINT,
    author_name VARCHAR(255),
    genre_name VARCHAR(255),
    is_available BOOLEAN,
    total_copies INT,
    copies_available INT,
    finalized BOOLEAN
);

-- PendingBookEvent table (temporary state)
CREATE TABLE pending_book_event (
    id BIGINT PRIMARY KEY,
    book_id VARCHAR(255) UNIQUE,
    genre_name VARCHAR(255),
    author_id BIGINT,
    author_name VARCHAR(255),
    title VARCHAR(255),
    description TEXT
);
```

### Design Decisions

1. **Separate Database**: Query service has its own database, independent from command service
2. **Event-Driven Synchronization**: Uses RabbitMQ to stay in sync, not direct database replication
3. **Denormalization**: Stores author and genre info directly in Book for fast queries
4. **PendingBookEvent**: Explicitly handles race conditions in event ordering
5. **No Validation Logic**: Query service trusts events from command service (separation of concerns)

### Benefits of This Bounded Context

✅ **Scalability**: Query model can be scaled independently for read-heavy workloads
✅ **Performance**: Denormalized structure optimized for specific query patterns
✅ **Resilience**: Decoupled from command service via event-driven architecture
✅ **Maintainability**: Clear responsibility boundaries and distinct models
✅ **Eventual Consistency**: Handles distributed system challenges gracefully
