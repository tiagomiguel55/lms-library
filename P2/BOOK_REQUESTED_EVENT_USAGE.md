# BookRequested Event - Implementation Guide

## Overview
The `BOOK_REQUESTED` event has been successfully implemented in your LMS library management system. This event is triggered when a book is requested and follows the same pattern as other book events (BOOK_CREATED, BOOK_UPDATED, BOOK_DELETED).

## What Was Implemented

### 1. Event Constant
Added `BOOK_REQUESTED` constant to `BookEvents` interface in all three projects:
- `src/main/java/pt/psoft/g1/psoftg1/shared/model/BookEvents.java`
- `P2/lms_books_command/src/main/java/pt/psoft/g1/psoftg1/shared/model/BookEvents.java`
- `P2/lms_books_query/src/main/java/pt/psoft/g1/psoftg1/shared/model/BookEvents.java`

```java
public interface BookEvents {
    static final String BOOK_CREATED = "BOOK_CREATED";
    static final String BOOK_UPDATED = "BOOK_UPDATED";
    static final String BOOK_DELETED = "BOOK_DELETED";
    static final String BOOK_REQUESTED = "BOOK_REQUESTED";
}
```

### 2. Publisher Interface Method
Added to `BookEventsPublisher` interface:
```java
BookViewAMQP sendBookRequested(Book book);
```

### 3. Publisher Implementation
Implemented in `BookEventsRabbitmqPublisherImpl`:
```java
@Override
public BookViewAMQP sendBookRequested(Book book) {
    return sendBookEvent(book, book.getVersion(), BookEvents.BOOK_REQUESTED);
}
```

### 4. RabbitMQ Configuration
Added queue and binding configuration in both command and query services:

**Queue Bean:**
```java
@Bean
public Queue autoDeleteQueue_Book_Requested() {
    return new AnonymousQueue();
}
```

**Binding Bean:**
```java
@Bean
public Binding binding4(DirectExchange direct, Queue autoDeleteQueue_Book_Requested){
    return BindingBuilder.bind(autoDeleteQueue_Book_Requested)
            .to(direct)
            .with(BookEvents.BOOK_REQUESTED);
}
```

### 5. Event Consumer/Listener
Added RabbitMQ listener in both command and query `BookRabbitmqController`:

```java
@RabbitListener(queues = "#{autoDeleteQueue_Book_Requested.name}")
public void receiveBookRequested(Message msg) {
    try {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
        BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);

        System.out.println(" [x] Received Book Requested by AMQP: " + 
                          bookViewAMQP.getIsbn() + " - " + bookViewAMQP.getTitle());
        
        // Log or process the book request
        System.out.println(" [x] Book request logged successfully.");
    }
    catch(Exception ex) {
        System.out.println(" [x] Exception receiving book requested event from AMQP: '" + 
                          ex.getMessage() + "'");
    }
}
```

## How to Use

### Publishing a Book Request Event

In your service layer (e.g., BookService or LendingService), inject the publisher and call:

```java
@Service
public class BookService {
    
    @Autowired
    private BookEventsPublisher bookEventsPublisher;
    
    @Autowired
    private BookRepository bookRepository;
    
    public void requestBook(String isbn) {
        // Find the book
        Book book = bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new NotFoundException("Book not found"));
        
        // Publish the book requested event
        bookEventsPublisher.sendBookRequested(book);
        
        // Additional business logic...
    }
}
```

### Example Use Cases

1. **Book Request Analytics**
   - Track which books are being requested most frequently
   - Identify popular books for purchasing decisions
   - Generate reports on book demand

2. **Recommendation System**
   - Use request data to recommend similar books
   - Build user preference profiles

3. **Inventory Management**
   - Alert when a book with low copies has high request count
   - Trigger purchase orders for popular books

4. **User Notifications**
   - Notify users when a requested book becomes available
   - Send reading suggestions based on requests

### Consuming the Event

The event is automatically consumed by the `BookRabbitmqController` in both command and query services. You can extend the `receiveBookRequested` method to add your custom business logic:

```java
@RabbitListener(queues = "#{autoDeleteQueue_Book_Requested.name}")
public void receiveBookRequested(Message msg) {
    try {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
        BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);

        // YOUR CUSTOM LOGIC HERE:
        
        // 1. Log to analytics database
        analyticsService.logBookRequest(bookViewAMQP);
        
        // 2. Update recommendation engine
        recommendationService.updateBookPopularity(bookViewAMQP.getIsbn());
        
        // 3. Check inventory and notify if low
        inventoryService.checkAndNotify(bookViewAMQP.getIsbn());
        
        // 4. Store for reporting
        reportingService.storeRequestEvent(bookViewAMQP);
        
    } catch(Exception ex) {
        logger.error("Error processing book requested event", ex);
    }
}
```

## Event Data Structure

The `BookViewAMQP` object contains:
- `isbn`: String - The book's ISBN
- `title`: String - The book's title
- `description`: String - The book's description
- `authorIds`: List<Long> - List of author IDs
- `genre`: String - The book's genre
- `version`: Long - The book's version for optimistic locking

## Testing

To test the BookRequested event:

1. **Start RabbitMQ** (if using Docker):
   ```bash
   docker-compose up rabbitmq
   ```

2. **Start the services**:
   ```bash
   # Terminal 1: Start command service
   cd P2/lms_books_command
   mvn spring-boot:run
   
   # Terminal 2: Start query service
   cd P2/lms_books_query
   mvn spring-boot:run
   ```

3. **Trigger a book request** through your API endpoint that calls:
   ```java
   bookEventsPublisher.sendBookRequested(book);
   ```

4. **Check the logs** - You should see:
   ```
   [x] Send Book event to AMQP Broker: <Book Title>
   [x] Received Book Requested by AMQP: <ISBN> - <Book Title>
   [x] Book request logged successfully.
   ```

## Architecture

The BookRequested event follows the CQRS (Command Query Responsibility Segregation) pattern:

1. **Command Service**: Publishes the BOOK_REQUESTED event when a book is requested
2. **Message Broker (RabbitMQ)**: Routes the event to subscribed services
3. **Query Service**: Consumes the event and can update read models or trigger analytics
4. **Both Services**: Can listen to the event and react independently

## Notes

- The event uses the book's current version when publishing
- Events are published asynchronously via RabbitMQ
- AnonymousQueue ensures each service instance gets its own queue
- The DirectExchange routes events based on the routing key (BOOK_REQUESTED)

## Next Steps

Consider implementing:
- Persistent queue configuration for production (replace AnonymousQueue)
- Dead letter queue for failed event processing
- Event replay mechanism for audit/debugging
- Metrics collection for event processing performance

