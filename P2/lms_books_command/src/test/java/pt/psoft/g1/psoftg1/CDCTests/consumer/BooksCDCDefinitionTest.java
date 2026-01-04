package pt.psoft.g1.psoftg1.CDCTests.consumer;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRabbitmqController;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookRequestRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {BookRabbitmqController.class}
)
@PactConsumerTest
@PactTestFor(providerName = "book_event-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
public class BooksCDCDefinitionTest {

    @MockBean
    BookRepository bookRepository;

    @MockBean
    AuthorRepository authorRepository;

    @MockBean
    GenreRepository genreRepository;

    @MockBean
    PendingBookRequestRepository pendingBookRequestRepository;

    @MockBean
    BookEventsPublisher bookEventsPublisher;

    @MockBean
    BookService bookService;

    @Autowired
    BookRabbitmqController listener;

    // Eventos consumidos pelo Books Command

    @Pact(consumer = "book_created-consumer")
    V4Pact createBookCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("isbn", "9780140449136");
        body.stringType("title", "The Odyssey");
        body.stringType("description", "An epic poem by Homer");
        body.stringType("genre", "Classic Literature");
        body.array("authorIds")
                .integerType(1)
                .closeArray();
        body.stringMatcher("version", "[0-9]+", "1");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a book created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "book_requested-consumer")
    V4Pact createBookRequestedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("bookId", "9780140449136");
        body.stringType("title", "The Odyssey");
        body.stringType("authorName", "Homer");
        body.stringType("genreName", "Classic Literature");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a book requested event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "book_updated-consumer")
    V4Pact createBookUpdatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("isbn", "9780140449136");
        body.stringType("title", "The Odyssey - Updated");
        body.stringType("description", "An epic poem by Homer - Updated edition");
        body.stringType("genre", "Classic Literature");
        body.array("authorIds")
                .integerType(1)
                .closeArray();
        body.stringMatcher("version", "[0-9]+", "2");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a book updated event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "author_pending_created-consumer")
    V4Pact createAuthorPendingCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("name", "John Doe");
        body.stringType("bio", "An experienced writer");
        body.stringType("requestId", "req-12345");
        body.stringMatcher("timestamp", "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "2026-01-03T10:30:00");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("an author pending created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "genre_pending_created-consumer")
    V4Pact createGenrePendingCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("genre", "Science Fiction");
        body.stringType("requestId", "genre-req-12345");
        body.stringMatcher("timestamp", "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "2026-01-03T10:30:00");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a genre pending created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "author_creation_failed-consumer")
    V4Pact createAuthorCreationFailedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("name", "Invalid Author");
        body.stringType("requestId", "req-fail-123");
        body.stringType("errorMessage", "Author name validation failed");
        body.stringMatcher("timestamp", "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "2026-01-03T10:45:00");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("an author creation failed event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "genre_creation_failed-consumer")
    V4Pact createGenreCreationFailedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("genre", "Invalid Genre");
        body.stringType("requestId", "genre-req-fail-123");
        body.stringType("errorMessage", "Genre validation failed");
        body.stringMatcher("timestamp", "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "2026-01-03T10:45:00");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a genre creation failed event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "author_created-consumer")
    V4Pact createAuthorCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.integerType("authorId", 1);
        body.stringType("name", "Jane Smith");
        body.stringType("bio", "Bestselling novelist");
        body.stringMatcher("version", "[0-9]+", "1");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("an author created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "genre_created-consumer")
    V4Pact createGenreCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("genre", "Mystery");
        body.stringMatcher("version", "[0-9]+", "1");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a genre created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "book_finalized-consumer")
    V4Pact createBookFinalizedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.integerType("authorId", 1);
        body.stringType("authorName", "Homer");
        body.stringType("bookId", "9780140449136");
        body.stringType("genreName", "Classic Literature");
        body.stringType("title", "The Odyssey");
        body.stringType("description", "An epic poem by Homer");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a book finalized event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "lending_validation_request-consumer")
    V4Pact createLendingValidationRequestPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingId", "lending-123");
        body.stringType("isbn", "9780140449136");
        body.stringType("readerNumber", "2024/1");
        body.stringMatcher("requestedAt", "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "2026-01-03T12:00:00");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a lending validation request event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    //
    // Test method signatures for contract generation
    //

    @Test
    @PactTestFor(pactMethod = "createBookCreatedPact")
    void testBookCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createBookRequestedPact")
    void testBookRequested(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createBookUpdatedPact")
    void testBookUpdated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createAuthorPendingCreatedPact")
    void testAuthorPendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createGenrePendingCreatedPact")
    void testGenrePendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createAuthorCreationFailedPact")
    void testAuthorCreationFailed(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createGenreCreationFailedPact")
    void testGenreCreationFailed(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createAuthorCreatedPact")
    void testAuthorCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createGenreCreatedPact")
    void testGenreCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createBookFinalizedPact")
    void testBookFinalized(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createLendingValidationRequestPact")
    void testLendingValidationRequest(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in BooksCDCConsumerIT
    }
}
