package CDC.consumer;

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
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
        ,classes = {BookRabbitmqController.class, BookService.class}
)
@PactConsumerTest
@PactTestFor(providerName = "book_event-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
public class BooksCDCDefinitionTest {

  @MockBean
  BookService bookService;

  @Autowired
  BookRabbitmqController listener;

  @Pact(consumer = "book_created-consumer")
  V4Pact createBookCreatedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody();
    body.stringType("isbn", "6475803429671");
    body.stringType("title", "title");
    body.stringType("description", "description");
    body.stringType("genre", "Infantil");
    body.array("authorIds")
            .integerType(1)
            .closeArray();
    body.stringMatcher("version", "[0-9]+", "1");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("a book created event").withMetadata(metadata).withContent(body).toPact();
  }

  @Pact(consumer = "book_updated-consumer")
  V4Pact createBookUpdatedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
            .stringType("isbn", "6475803429671")
            .stringType("title", "updated title")
            .stringType("description", "description")
            .stringType("genre", "Infantil");
        body.array("authorIds")
            .integerType(1)
            .closeArray();

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("a book updated event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
  }

  @Pact(consumer = "book_deleted-consumer")
  V4Pact createBookDeletedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
            .stringType("isbn", "6475803429671")
            .stringType("title", "deleted book")
            .stringType("genre", "Infantil")
            .stringMatcher("version", "[0-9]+", "1");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("a book deleted event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
  }

  @Pact(consumer = "author_created-consumer")
  V4Pact createAuthorCreatedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
            .integerType("authorId", 1)
            .stringType("name", "João Silva")
            .stringType("bio", "An experienced author with multiple publications")
            .stringMatcher("version", "[0-9]+", "1");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("an author created event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
  }

  @Pact(consumer = "author_pending_created-consumer")
  V4Pact createAuthorPendingCreatedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
            .stringType("name", "Maria Santos")
            .stringType("bio", "New author pending approval")
            .stringType("requestId", "req-12345");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("an author pending created event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
  }

  @Pact(consumer = "genre_created-consumer")
  V4Pact createGenreCreatedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
            .stringType("genre", "Science Fiction")
            .stringMatcher("version", "[0-9]+", "1");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("a genre created event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
  }

  @Pact(consumer = "book_requested-consumer")
  V4Pact createBookRequestedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
            .stringType("isbn", "9780140449136")
            .stringType("title", "The Odyssey")
            .stringType("description", "An epic poem by Homer")
            .stringType("genre", "Classic Literature");
    body.array("authorIds")
            .integerType(5)
            .closeArray();
    body.stringType("requestId", "req-98765");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("a book requested event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
  }

  @Pact(consumer = "book_finalized-consumer")
  V4Pact createBookFinalizedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
            .stringType("isbn", "9780140449136")
            .stringType("status", "APPROVED")
            .stringType("requestId", "req-98765");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("a book finalized event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
  }

  @Pact(consumer = "author_creation_failed-consumer")
  V4Pact createAuthorCreationFailedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
            .stringType("name", "Invalid Author")
            .stringType("reason", "Author name already exists")
            .stringType("requestId", "req-failed-001");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/json");

    return builder.expectsToReceive("an author creation failed event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
  }
//
// The following tests are now defined as IT tests, so that the definition of contract and the tests are decoupled.
// Yet, while the body of the tests can be elsewhere, the method signature must be defined here so the contract is generated.
//
  @Test
  @PactTestFor(pactMethod = "createBookCreatedPact")
  void testBookCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
//
//  // Convert the Pact message to a String (JSON payload)
//    String jsonReceived = messages.get(0).contentsAsString();
//
//    // Create a Spring AMQP Message with the JSON payload and optional headers
//    MessageProperties messageProperties = new MessageProperties();
//    messageProperties.setContentType("application/json");
//    Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);
//
//    // Simulate receiving the message in the listener
//    assertDoesNotThrow(() -> {
//      listener.receiveBookCreatedMsg(message);
//    });
//
//    // Verify interactions with the mocked service
//    verify(bookService, times(1)).create(any(BookViewAMQP.class));
  }

  @Test
  @PactTestFor(pactMethod = "createBookUpdatedPact")
  void testBookUpdated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
//    String jsonReceived = messages.get(0).contentsAsString();
//    MessageProperties messageProperties = new MessageProperties();
//    messageProperties.setContentType("application/json");
//    Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);
//
//    assertDoesNotThrow(() -> {
//      listener.receiveBookUpdated(message);
//    });
//
//    // Verify interactions with the mocked service
//    verify(bookService, times(1)).update(any(BookViewAMQP.class));
  }

  @Test
  @PactTestFor(pactMethod = "createBookDeletedPact")
  void testBookDeleted(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
    // Verify the contract for book deletion events
  }

  @Test
  @PactTestFor(pactMethod = "createAuthorCreatedPact")
  void testAuthorCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
    // Verify the contract for author created events
  }

  @Test
  @PactTestFor(pactMethod = "createAuthorPendingCreatedPact")
  void testAuthorPendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
    // Verify the contract for author pending created events
  }

  @Test
  @PactTestFor(pactMethod = "createGenreCreatedPact")
  void testGenreCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
    // Verify the contract for genre creation events
  }

  @Test
  @PactTestFor(pactMethod = "createBookRequestedPact")
  void testBookRequested(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
    // Verify the contract for book request events
    // This supports the book request and approval workflow
  }

  @Test
  @PactTestFor(pactMethod = "createBookFinalizedPact")
  void testBookFinalized(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
    // Verify the contract for book finalization events
    // Handles the completion of book approval process
  }

  @Test
  @PactTestFor(pactMethod = "createAuthorCreationFailedPact")
  void testAuthorCreationFailed(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
    // Verify the contract for author creation failure events
  }
}