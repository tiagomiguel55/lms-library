# MongoDB Migration Guide for LMS Books Query Service

## Overview
This guide documents the migration from PostgreSQL (JPA/Hibernate) to MongoDB for the query service in the CQRS pattern.

## Changes Made

### 1. Dependencies (pom.xml)
**Removed:**
- `spring-boot-starter-data-jpa`
- `postgresql` driver
- `h2` database

**Added:**
- `spring-boot-starter-data-mongodb`
- `testcontainers:mongodb` for testing

### 2. Entity Model Changes

All entity classes were converted from JPA entities to MongoDB documents:

#### Book.java
```java
// BEFORE (JPA)
@Entity
@Table(name = "Book", uniqueConstraints = {...})
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long pk;
    
    @Version
    private Long version;
    
    @Embedded
    Isbn isbn;
    
    @ManyToOne
    Genre genre;
    
    @ManyToMany
    private List<Author> authors;
}

// AFTER (MongoDB)
@Document(collection = "books")
public class Book {
    @Id
    @Getter
    private String id;  // MongoDB uses String IDs by default
    
    @Version
    @Getter
    private Long version;
    
    @Indexed(unique = true)
    private Isbn isbn;
    
    @DBRef  // MongoDB reference to Genre document
    @NotNull
    private Genre genre;
    
    @DBRef  // MongoDB reference to Author documents
    private List<Author> authors;
}
```

#### Author.java
```java
// MongoDB Document
@Document(collection = "authors")
public class Author {
    @Id
    @Getter
    private String id;
    
    @Getter
    private long authorNumber;  // Business key
    
    @Version
    private long version;
}
```

#### Genre.java
```java
@Document(collection = "genres")
public class Genre {
    @Id
    @Getter
    private String id;
    
    @Indexed(unique = true)
    @Getter
    String genre;
}
```

### 3. Value Objects

Value objects (Isbn, Title, Description, Name, Bio) no longer need `@Embeddable`:
- **Removed**: `@Embeddable`, `@Column` annotations
- **Changed**: `@Transient` now uses `org.springframework.data.annotation.Transient`
- MongoDB automatically embeds these as nested documents

### 4. Repository Changes

#### Before (JPA)
```java
public interface SpringDataBookRepository extends 
    BookRepository, 
    CrudRepository<Book, Isbn> {
    
    @Query("SELECT b FROM Book b WHERE b.isbn.isbn = :isbn")
    Optional<Book> findByIsbn(@Param("isbn") String isbn);
}
```

#### After (MongoDB)
```java
public interface SpringDataBookRepository extends 
    BookRepository, 
    BookRepoCustom,
    MongoRepository<Book, String> {  // String ID
    
    Optional<Book> findByIsbn(String isbn);  // Query method
}

// Custom queries using MongoTemplate
@RequiredArgsConstructor
class BookRepoCustomImpl implements BookRepoCustom {
    private final MongoTemplate mongoTemplate;
    
    @Override
    public Optional<Book> findByIsbnCustom(String isbn) {
        Query query = new Query();
        query.addCriteria(Criteria.where("isbn.isbn").is(isbn));
        return Optional.ofNullable(mongoTemplate.findOne(query, Book.class));
    }
    
    @Override
    public List<Book> searchBooks(Page page, SearchBooksQuery searchQuery) {
        Query query = new Query();
        
        if (StringUtils.hasText(searchQuery.getTitle())) {
            query.addCriteria(Criteria.where("title.title")
                .regex("^" + searchQuery.getTitle(), "i"));
        }
        
        query.skip((long) (page.getNumber() - 1) * page.getLimit());
        query.limit(page.getLimit());
        
        return mongoTemplate.find(query, Book.class);
    }
}
```

### 5. Configuration Changes

#### JpaConfig → MongoConfig
```java
// Renamed file to MongoConfig.java
@Configuration
@EnableTransactionManagement  // MongoDB supports transactions
public class MongoConfig {
    @Bean("auditorProvider")
    public AuditorAware<String> auditorProvider() {
        // Same as before
    }
}
```

#### Application Properties
```properties
# REMOVED PostgreSQL configuration:
# spring.datasource.url=jdbc:postgresql://...
# spring.jpa.hibernate.ddl-auto=update
# spring.jpa.generate-ddl=true

# ADDED MongoDB configuration:
spring.data.mongodb.host=mongodb_query_dev
spring.data.mongodb.port=27017
spring.data.mongodb.database=query_books_dev
spring.data.mongodb.username=lmsuser
spring.data.mongodb.password=lmspass
spring.data.mongodb.authentication-database=admin
```

### 6. Exception Handling

**Removed:**
- `org.hibernate.exception.ConstraintViolationException`
- `org.hibernate.StaleObjectStateException`

**Replaced with:**
- `org.springframework.dao.DuplicateKeyException` (for unique constraint violations)
- `ConflictException` (for optimistic locking)

### 7. Docker Configuration

Created `docker-compose-rabbitmq+mongodb.yml`:
```yaml
services:
  mongodb_query_dev:
    container_name: mongodb_query_dev
    image: mongo:7.0
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin
      MONGO_INITDB_DATABASE: query_books_dev
    volumes:
      - mongodb_query_volume:/data/db
      - ./init-mongo.js:/docker-entrypoint-initdb.d/init-mongo.js:ro
```

Created `init-mongo.js` for database initialization:
```javascript
db = db.getSiblingDB('query_books_dev');

db.createUser({
    user: 'lmsuser',
    pwd: 'lmspass',
    roles: [{ role: 'readWrite', db: 'query_books_dev' }]
});

// Create indexes
db.books.createIndex({ "isbn.isbn": 1 }, { unique: true });
db.books.createIndex({ "title.title": 1 });
db.books.createIndex({ "genre.genre": 1 });
db.books.createIndex({ "authors.authorNumber": 1 });

db.authors.createIndex({ "authorNumber": 1 }, { unique: true });
db.genres.createIndex({ "genre": 1 }, { unique: true });
```

## Key Differences: JPA vs MongoDB

| Feature | JPA/PostgreSQL | MongoDB |
|---------|---------------|---------|
| **Entity Annotation** | `@Entity` | `@Document(collection="...")` |
| **Primary Key** | `@Id @GeneratedValue` (Long) | `@Id` (String, auto-generated) |
| **Embedded Objects** | `@Embeddable` | Automatic embedding (no annotation needed) |
| **Relationships** | `@ManyToOne`, `@ManyToMany` | `@DBRef` (or embedded documents) |
| **Unique Constraints** | `@Column(unique=true)` | `@Indexed(unique=true)` |
| **Transient Fields** | `jakarta.persistence.Transient` | `org.springframework.data.annotation.Transient` |
| **Queries** | JPQL, `@Query` | MongoTemplate, Query DSL |
| **Schema Generation** | `ddl-auto` | No schema (schemaless) |

## Benefits of MongoDB for Query Service

1. **Flexible Schema**: Better for denormalized read models
2. **JSON-like Documents**: Natural fit for API responses
3. **Horizontal Scaling**: Better for read-heavy query patterns
4. **Embedded Documents**: Reduces joins needed for complex queries
5. **Rich Query Language**: Aggregation pipeline for complex analytics

## Remaining Work

Due to the large number of interconnected files, the following still need attention:

1. **Add missing @Getter annotations** to all fields that need public getters
2. **Update all DTO classes** (BookFinalizedEvent, PendingBookEvent, etc.) with proper Lombok annotations
3. **Fix SpringDataGenreRepository** - remove `@Override` from default method
4. **Update all controllers and mappers** to use the correct getter methods
5. **Fix constructor issues** in RabbitMQ controllers
6. **Complete the migration** of BookServiceImpl with correct method calls

## Next Steps

1. Run: `mvn clean install -DskipTests` to rebuild with new dependencies
2. Start MongoDB: `docker-compose -f docker-compose-rabbitmq+mongodb.yml up -d`
3. Run the application and test CQRS event flow
4. Verify data synchronization between command (PostgreSQL) and query (MongoDB) services

## Testing

```bash
# Start infrastructure
docker-compose -f docker-compose-rabbitmq+mongodb.yml up -d

# Check MongoDB is running
docker exec -it mongodb_query_dev mongosh -u admin -p admin

# Use the query database
use query_books_dev

# Verify collections were created
show collections

# Check indexes
db.books.getIndexes()
```

## Rollback Plan

If issues arise, you can roll back by:
1. Revert pom.xml to use JPA dependencies
2. Revert entity annotations to JPA annotations
3. Revert repositories to use CrudRepository
4. Update application properties to use PostgreSQL again

