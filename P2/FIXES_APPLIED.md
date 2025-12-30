# Fixes Applied to LMS Books Event Flow

## Problem Summary

When calling `/books/create-complete` endpoint in the command service:
- ✅ The command service saga completed successfully
- ❌ The query service received NO logs and NO events
- ❌ Books were not appearing in the query service

## Root Causes Identified & Fixed

### 1. **Exchange Name Mismatch (CRITICAL)**
**Problem**: Command and Query services were using different RabbitMQ exchange names:
- **Command Service**: `books.exchange`, `authors.exchange`, `genres.exchange`
- **Query Service**: `LMS.books`, `LMS.authors`, `LMS.genres`

**Impact**: Events published to one exchange were never received by services listening on the other exchange.

**Solution Applied**: Updated query service's `RabbitmqClientConfig.java` to use the same exchange names as the command service:
```java
@Bean
public DirectExchange direct() {
    return new DirectExchange("books.exchange");  // ← Changed from "LMS.books"
}

@Bean
public DirectExchange directAuthors() {
    return new DirectExchange("authors.exchange");  // ← Changed from "LMS.authors"
}

@Bean
public DirectExchange directGenres() {
    return new DirectExchange("genres.exchange");  // ← Changed from "LMS.genres"
}
```

### 2. **GenreServiceImpl.markGenreAsFinalized() - Backwards Logic (BUG)**
**Problem**: Method had inverted if/else logic:
```java
// BEFORE (BROKEN):
Optional<Genre> genreOpt = genreRepository.findByString(genreName);
if (genreOpt.isPresent()) {  // If genre IS present
    Genre genre = genreOpt.get();
    if (genre.isFinalized()) {
        System.out.println(" [x] Genre already finalized: " + genreName);
        return;
    }
    System.out.println(" [x] ⚠️ Genre not found: " + genreName);  // ← WRONG MESSAGE
    throw new NotFoundException("Genre not found: " + genreName);  // ← Throws exception when genre EXISTS!
}
```

**Impact**: Every time a genre needed to be finalized, an exception was thrown, causing infinite retry loops.

**Solution Applied**: Fixed the logic and actually mark the genre as finalized:
```java
// AFTER (FIXED):
Optional<Genre> genreOpt = genreRepository.findByString(genreName);
if (genreOpt.isEmpty()) {  // ← Check for empty instead
    System.out.println(" [x] ❌ Genre not found: " + genreName);
    throw new NotFoundException("Genre not found: " + genreName);
}

Genre genre = genreOpt.get();
if (genre.isFinalized()) {
    System.out.println(" [x] ℹ️ Genre already finalized: " + genreName);
    return;
}

// Mark as finalized
genre.setFinalized(true);
genreRepository.save(genre);  // ← ACTUALLY SAVE IT!
System.out.println(" [x] ✅ Genre marked as finalized: " + genreName);
```

### 3. **AuthorServiceImpl.markAuthorAsFinalized() - Incomplete Implementation (BUG)**
**Problem**: Method checked if author was finalized but never actually marked it:
```java
// BEFORE (INCOMPLETE):
@Override
public void markAuthorAsFinalized(Long authorNumber) {
    Author author = authorRepository.findByAuthorNumber(authorNumber)
            .orElseThrow(() -> new NotFoundException("Author not found with ID: " + authorNumber));

    if (author.isFinalized()) {
        System.out.println(" [x] Author already finalized: " + author.getName() + " (ID: " + authorNumber + ")");
        return;
    }
    // ← METHOD ENDS HERE - NEVER MARKS AUTHOR AS FINALIZED!
}
```

**Impact**: Authors were never marked as finalized, blocking the book creation saga.

**Solution Applied**: Complete the implementation:
```java
// AFTER (FIXED):
@Override
public void markAuthorAsFinalized(Long authorNumber) {
    Author author = authorRepository.findByAuthorNumber(authorNumber)
            .orElseThrow(() -> new NotFoundException("Author not found with ID: " + authorNumber));

    if (author.isFinalized()) {
        System.out.println(" [x] ℹ️ Author already finalized: " + author.getName() + " (ID: " + authorNumber + ")");
        return;
    }
    
    // Mark as finalized
    author.setFinalized(true);
    authorRepository.save(author);  // ← ACTUALLY SAVE IT!
    System.out.println(" [x] ✅ Author marked as finalized: " + author.getName() + " (ID: " + authorNumber + ")");
}
```

## Files Modified

1. **Query Service**
   - `C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_query\src\main\java\pt\psoft\g1\psoftg1\configuration\RabbitmqClientConfig.java`
     - Changed exchange names from `LMS.*` to `*.exchange`

2. **Command Service**
   - `C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_command\src\main\java\pt\psoft\g1\psoftg1\genremanagement\services\GenreServiceImpl.java`
     - Fixed backwards logic in `markGenreAsFinalized()`
     - Added actual finalization save
   
   - `C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_command\src\main\java\pt\psoft\g1\psoftg1\authormanagement\services\AuthorServiceImpl.java`
     - Completed implementation of `markAuthorAsFinalized()`
     - Added actual finalization save

## Compilation Status

✅ **Query Service**: BUILD SUCCESS
✅ **Command Service**: BUILD SUCCESS

## Expected Behavior After Fix

When you call `/books/create-complete` endpoint:

1. Command service publishes `BookRequestedEvent`
2. AuthorCmd and GenreCmd create temporary author/genre
3. Command service publishes `BookFinalized` event
4. Both services' finalization methods now properly execute
5. **Query service receives events on correct `books.exchange`**
6. **Query service logs appear showing:**
   ```
   [QUERY] 📥 Received Book Created
   [QUERY] 📥 Received Author Created
   [QUERY] 📥 Received Genre Created
   [QUERY] 📥 Received Book Finalized
   [QUERY] ✅ Book finalized and saved
   ```
7. Books now appear in query service database

## Testing Steps

1. Rebuild both Docker images:
   ```bash
   cd lms_books_command && docker build -t lmsbooks:command -f Dockerfile .
   cd ../lms_books_query && docker build -t lmsbooks:query -f Dockerfile .
   ```

2. Redeploy services:
   ```bash
   docker stack rm lms_command lms_query
   docker stack deploy -c docker-compose-swarm.yml lms_command
   docker stack deploy -c docker-compose-swarm.yml lms_query
   ```

3. Call the endpoint:
   ```bash
   curl -X POST http://localhost:8080/api/books/create-complete \
     -H "Content-Type: application/json" \
     -d '{
       "bookId": "978-0747532699",
       "authorName": "J.K. Rowling",
       "genreName": "Fantasy"
     }'
   ```

4. Check logs:
   ```bash
   docker service logs -f lms_command_lmsbooks_command
   docker service logs -f lms_query_lmsbooks_query
   ```

5. Verify book in query service:
   ```bash
   curl http://localhost:8085/api/v1/books/isbn/978-0747532699
   ```

## Summary

**Three critical issues fixed:**
1. ✅ Exchange names now match between command and query services
2. ✅ Genre finalization now actually saves to database
3. ✅ Author finalization now actually saves to database

**Result**: Query service will now receive and process all events from the command service, and books will properly appear in the query database with the three-event flow (AuthorCreated → GenreCreated → BookFinalized).

