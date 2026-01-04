package pt.psoft.g1.psoftg1.CDCTests.producer;

import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.CDCTests.config.CDCTestConfiguration;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.LendingValidationResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Provider("book_event-producer")
@PactFolder("target/pacts")
@SpringBootTest(classes = {CDCTestConfiguration.class})
@ActiveProfiles("cdc-test")
public class BooksProducerCDCIT {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void setupTest(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @PactVerifyProvider("a book created event")
    MessageAndMetadata bookCreated() throws JsonProcessingException {
        BookViewAMQP bookView = new BookViewAMQP();
        bookView.setIsbn("9780140449136");
        bookView.setTitle("The Odyssey");
        bookView.setDescription("An epic poem by Homer");
        bookView.setGenre("Classic Literature");
        bookView.setAuthorIds(List.of(1L));
        bookView.setVersion(1L);

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(bookView);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a book updated event")
    MessageAndMetadata bookUpdated() throws JsonProcessingException {
        BookViewAMQP bookView = new BookViewAMQP();
        bookView.setIsbn("9780140449136");
        bookView.setTitle("The Odyssey - Updated");
        bookView.setDescription("An epic poem by Homer - Updated edition");
        bookView.setGenre("Classic Literature");
        bookView.setAuthorIds(List.of(1L));
        bookView.setVersion(2L);

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(bookView);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a book deleted event")
    MessageAndMetadata bookDeleted() throws JsonProcessingException {
        Map<String, Object> deletedBookData = new HashMap<>();
        deletedBookData.put("isbn", "9780140449136");
        deletedBookData.put("deletedAt", "2026-01-03T10:30:00");

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(deletedBookData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a book requested event")
    MessageAndMetadata bookRequested() throws JsonProcessingException {
        BookRequestedEvent bookRequestedEvent = new BookRequestedEvent();
        bookRequestedEvent.setBookId("9780140449136");
        bookRequestedEvent.setTitle("The Odyssey");
        bookRequestedEvent.setAuthorName("Homer");
        bookRequestedEvent.setGenreName("Classic Literature");

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(bookRequestedEvent);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a book finalized event")
    MessageAndMetadata bookFinalized() throws JsonProcessingException {
        BookFinalizedEvent bookFinalizedEvent = new BookFinalizedEvent();
        bookFinalizedEvent.setAuthorId(1L);
        bookFinalizedEvent.setAuthorName("Homer");
        bookFinalizedEvent.setBookId("9780140449136");
        bookFinalizedEvent.setGenreName("Classic Literature");
        bookFinalizedEvent.setTitle("The Odyssey");
        bookFinalizedEvent.setDescription("An epic poem by Homer");

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(bookFinalizedEvent);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a lending validation response event")
    MessageAndMetadata lendingValidationResponse() throws JsonProcessingException {
        LendingValidationResponse response = new LendingValidationResponse();
        response.setRequestId("req-123");
        response.setLendingNumber("lending-123");
        response.setBookExists(true);
        response.setIsbn("9780140449136");
        response.setMessage("Book is available for lending");

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(response);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("an author created event")
    MessageAndMetadata authorCreated() throws JsonProcessingException {
        Map<String, Object> authorData = new HashMap<>();
        authorData.put("authorNumber", 1L);  // ← Corrigido de "authorId" para "authorNumber"
        authorData.put("name", "Jane Smith");
        authorData.put("bio", "Bestselling novelist");
        authorData.put("version", 1L);  // ← Corrigido de String "1" para Long

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(authorData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a genre created event")
    MessageAndMetadata genreCreated() throws JsonProcessingException {
        Map<String, Object> genreData = new HashMap<>();
        genreData.put("genre", "Mystery");
        genreData.put("version", 1L);  // ← Corrigido para Long

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(genreData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("an author pending created event")
    MessageAndMetadata authorPendingCreated() throws JsonProcessingException {
        Map<String, Object> authorPendingData = new HashMap<>();
        authorPendingData.put("name", "John Doe");
        authorPendingData.put("bio", "An experienced writer");
        authorPendingData.put("requestId", "req-12345");
        authorPendingData.put("timestamp", "2026-01-03T10:30:00");

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(authorPendingData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a genre pending created event")
    MessageAndMetadata genrePendingCreated() throws JsonProcessingException {
        Map<String, Object> genrePendingData = new HashMap<>();
        genrePendingData.put("genre", "Science Fiction");
        genrePendingData.put("requestId", "genre-req-12345");
        genrePendingData.put("timestamp", "2026-01-03T10:30:00");

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(genrePendingData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("an author creation failed event")
    MessageAndMetadata authorCreationFailed() throws JsonProcessingException {
        Map<String, Object> authorFailedData = new HashMap<>();
        authorFailedData.put("name", "Invalid Author");
        authorFailedData.put("requestId", "req-fail-123");
        authorFailedData.put("errorMessage", "Author name validation failed");
        authorFailedData.put("timestamp", "2026-01-03T10:45:00");

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(authorFailedData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a genre creation failed event")
    MessageAndMetadata genreCreationFailed() throws JsonProcessingException {
        Map<String, Object> genreFailedData = new HashMap<>();
        genreFailedData.put("genre", "Invalid Genre");
        genreFailedData.put("requestId", "genre-req-fail-123");
        genreFailedData.put("errorMessage", "Genre validation failed");
        genreFailedData.put("timestamp", "2026-01-03T10:45:00");

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(genreFailedData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }

    @PactVerifyProvider("a lending validation request event")
    MessageAndMetadata lendingValidationRequest() throws JsonProcessingException {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("requestId", "req-123");
        requestData.put("isbn", "9780140449136");
        requestData.put("lendingNumber", "2024/1");  // ← Corrigido de "lendingId" e "readerNumber"

        byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(requestData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return new MessageAndMetadata(payload, metadata);
    }
}
