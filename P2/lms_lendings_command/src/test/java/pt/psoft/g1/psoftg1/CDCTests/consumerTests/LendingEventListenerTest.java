package pt.psoft.g1.psoftg1.consumerTests;

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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.listeners.LendingEventListener;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = LendingEventListener.class)
@PactConsumerTest
@PactTestFor(providerName = "lending_event-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
public class LendingEventListenerTest {

    @MockBean
    LendingService lendingService;

    @Qualifier("lendingEventListener")
    @Autowired
    LendingEventListener listener;

    @Pact(consumer = "lending_created-consumer")
    V4Pact createLendingCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringType("lendingNumber", "2025/1")
                .stringType("genId", "gen-123")
                .stringType("isbn", "9783161484100")
                .stringType("readerNumber", "2025/1")
                .stringType("startDate", "2025-01-01")
                .stringType("limitDate", "2025-01-15")
                .stringType("returnedDate", "2025-01-15")
                .integerType("daysUntilReturn", 14)
                .integerType("daysOverdue", 0)
                .integerType("fineValueInCents", 0)
                .stringType("version", "1");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a lending created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createLendingCreatedPact")
    void testLendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        String jsonReceived = messages.get(0).contentsAsString();

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

        assertDoesNotThrow(() -> {
            listener.receiveBookCreated(message);
        });

        verify(lendingService, times(1)).create((LendingViewAMQP) any());
    }

    @Pact(consumer = "lending_updated-consumer")
    V4Pact createLendingUpdatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringType("lendingNumber", "2025/1")
                .stringType("genId", "gen-123")
                .stringType("isbn", "book-123")
                .stringType("readerNumber", "2025/1")
                .stringType("startDate", "2025-01-01")
                .stringType("limitDate", "2025-01-15")
                .stringType("returnedDate", "2025-01-15")
                .integerType("daysUntilReturn", 14)
                .integerType("daysOverdue", 0)
                .integerType("fineValueInCents", 0)
                .stringType("version", "1");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a lending updated event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createLendingUpdatedPact")
    void testLendingUpdated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        String jsonReceived = messages.get(0).contentsAsString();

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

        assertDoesNotThrow(() -> {
            listener.receiveBookUpdated(message);
        });

        verify(lendingService, times(1)).update((LendingViewAMQP) any());
    }

    @Pact(consumer = "lending_deleted-consumer")
    V4Pact createLendingDeletedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringType("lendingNumber", "2025/1")
                .stringType("genId", "gen-123")
                .stringType("isbn", "book-123")
                .stringType("readerNumber", "2025/1")
                .stringType("startDate", "2025-01-01")
                .stringType("limitDate", "2025-01-15")
                .stringType("returnedDate", "2025-01-15")
                .integerType("daysUntilReturn", 14)
                .integerType("daysOverdue", 0)
                .integerType("fineValueInCents", 0)
                .stringType("version", "1");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a lending deleted event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createLendingDeletedPact")
    void testLendingDeleted(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        String jsonReceived = messages.get(0).contentsAsString();

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

        assertDoesNotThrow(() -> {
            listener.receiveBookDeleted(jsonReceived);
        });

        verify(lendingService, times(1)).delete((LendingViewAMQP) any());
    }

    @Pact(consumer = "reader_lending_response-consumer")
    V4Pact createReaderLendingResponsePact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingNumber", "2025/1");
        body.stringType("status","SUCCESS");
        body.nullValue("error");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader lending response event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createReaderLendingResponsePact")
    void testReaderLendingResponse(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        String jsonReceived = messages.get(0).contentsAsString();

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

        assertDoesNotThrow(() -> {
            listener.receiveReaderLendingResponse(message);
        });

        verify(lendingService, times(1)).readerValidated(any());
    }

    @Pact(consumer = "book_lending_response-consumer")
    V4Pact createBookLendingResponsePact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingNumber", "2025/1");
        body.stringType("status","SUCCESS");
        body.nullValue("error");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a book lending response event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createBookLendingResponsePact")
    void testBookLendingResponse(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        String jsonReceived = messages.get(0).contentsAsString();

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

        assertDoesNotThrow(() -> {
            listener.receiveBookLendingResponse(message);
        });

        verify(lendingService, times(1)).bookValidated(any());
    }

    @Pact(consumer = "reader_lending_request-consumer")
    V4Pact createReaderLendingRequestPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingNumber", "2025/1");
        body.stringType("username", "john.doe@example.com");
        body.stringType("fullName", "John Doe");
        body.stringType("readerNumber", "2025/1");
        body.stringType("birthDate", "1990-01-01");
        body.stringType("phoneNumber", "912965338");
        body.stringType("photoUrl", "http://example.com/photo.jpg");
        body.booleanType("gdpr", true);
        body.booleanType("marketing", false);
        body.booleanType("thirdParty", true);
        body.array("interestList")
                .stringType("fiction")
                .closeArray();


        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader lending request event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createReaderLendingRequestPact")
    void testReaderLendingRequest(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        //criar o pact
    }

    @Pact(consumer = "book_lending_request-consumer")
    V4Pact createBookLendingRequestPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingNumber", "2025/1");
        body.stringType("title", "title");
        body.stringType("genre", "Infantil");
        body.stringType("description", "description");
        body.stringType("isbn", "912965338");
        body.stringType("version", "1");
        body.array("authorIds")
                .stringType("1")
                .closeArray();


        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a book lending request event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createBookLendingRequestPact")
    void testBookLendingRequest(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        //criar o pact
    }


}
