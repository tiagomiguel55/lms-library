package pt.psoft.g1.psoftg1.cdc.consumer;

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
import pt.psoft.g1.psoftg1.readermanagement.listeners.ReaderEventListener;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ReaderEventListener.class}
)
@PactConsumerTest
@PactTestFor(providerName = "reader_event-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
public class ReadersCDCDefinitionTest {

    @MockBean
    ReaderService readerService;

    @MockBean
    ReaderEventPublisher readerEventPublisher;

    @MockBean
    ReaderViewAMQPMapper readerViewAMQPMapper;

    @Autowired
    ReaderEventListener listener;

    @Pact(consumer = "reader_lending_request-consumer")
    V4Pact createReaderLendingRequestPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingNumber", "L2024/001");
        body.stringType("readerNumber", "2024/1");
        body.stringType("fullName", "John Doe");
        body.stringType("phoneNumber", "+351912345678");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader lending request event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "reader_created-consumer")
    V4Pact createReaderCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("readerNumber", "2024/1");
        body.stringType("fullName", "John Doe");
        body.stringType("phoneNumber", "+351912345678");
        body.stringMatcher("version", "[0-9]+", "1");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "reader_updated-consumer")
    V4Pact createReaderUpdatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("readerNumber", "2024/1");
        body.stringType("fullName", "John Doe Updated");
        body.stringType("phoneNumber", "+351987654321");
        body.stringMatcher("version", "[0-9]+", "2");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader updated event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "reader_deleted-consumer")
    V4Pact createReaderDeletedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("readerNumber", "2024/1");
        body.stringType("fullName", "John Doe");
        body.stringType("phoneNumber", "+351912345678");
        body.stringMatcher("version", "[0-9]+", "3");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader deleted event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "reader_lending_response-consumer")
    V4Pact createReaderLendingResponsePact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingNumber", "L2024/001");
        body.stringType("status", "SUCCESS");
        body.stringType("error", "");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader lending response event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "validate_reader-consumer")
    V4Pact createValidateReaderPact(MessagePactBuilder builder) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "text/plain; charset=utf-8");

        return builder.expectsToReceive("a validate reader event")
                .withMetadata(metadata)
                .withContent("2024/1")  // Just the reader ID as string
                .toPact();
    }

    @Pact(consumer = "reader_validated-consumer")
    V4Pact createReaderValidatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("readerId", "2024/1");
        body.booleanType("exists", true);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader validated event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    //
    // Test method signatures for contract generation
    //

    @Test
    @PactTestFor(pactMethod = "createReaderLendingRequestPact")
    void testReaderLendingRequest(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createReaderCreatedPact")
    void testReaderCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createReaderUpdatedPact")
    void testReaderUpdated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createReaderDeletedPact")
    void testReaderDeleted(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createReaderLendingResponsePact")
    void testReaderLendingResponse(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createValidateReaderPact")
    void testValidateReader(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createReaderValidatedPact")
    void testReaderValidated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }
}
