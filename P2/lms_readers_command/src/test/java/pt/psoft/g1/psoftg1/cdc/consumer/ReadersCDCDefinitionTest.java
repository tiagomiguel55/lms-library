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
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderRabbitmqController;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.PendingReaderUserRequestRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderMapper;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ReaderRabbitmqController.class}
)
@PactConsumerTest
@PactTestFor(providerName = "reader_event-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
public class ReadersCDCDefinitionTest {

    @MockBean
    ReaderRepository readerRepository;

    @MockBean
    PendingReaderUserRequestRepository pendingReaderUserRequestRepository;

    @MockBean
    ReaderEventPublisher readerEventPublisher;

    @MockBean
    ReaderMapper readerMapper;

    @MockBean
    ForbiddenNameRepository forbiddenNameRepository;

    @MockBean
    ReaderService readerService;

    @MockBean
    ReaderViewAMQPMapper readerViewAMQPMapper;

    @MockBean
    pt.psoft.g1.psoftg1.usermanagement.services.UserService userService;

    @Autowired
    ReaderRabbitmqController listener;

    @Pact(consumer = "reader_user_requested-consumer")
    V4Pact createReaderUserRequestedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("readerNumber", "2024/1");
        body.stringType("username", "john.doe");
        body.stringType("password", "Test123!");
        body.stringType("fullName", "John Doe");
        body.stringType("birthDate", "1990-01-01");
        body.stringType("phoneNumber", "+351912345678");
        body.stringType("photoURI", "http://example.com/photo.jpg");
        body.booleanType("gdpr", true);
        body.booleanType("marketing", false);
        body.booleanType("thirdParty", false);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader user requested event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "user_pending_created-consumer")
    V4Pact createUserPendingCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("readerNumber", "2024/1");
        body.stringType("userId", "123");
        body.stringType("username", "john.doe");
        body.booleanType("finalized", false);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a user pending created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "reader_pending_created-consumer")
    V4Pact createReaderPendingCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("readerNumber", "2024/1");
        body.stringType("readerId", "456");
        body.stringType("username", "john.doe");
        body.booleanType("finalized", false);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader pending created event")
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

    @Pact(consumer = "user_created-consumer")
    V4Pact createUserCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("username", "john.doe");
        body.stringType("fullName", "John Doe");
        body.stringType("password", "Test123!");
        body.stringMatcher("version", "[0-9]+", "1");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a user created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "user_updated-consumer")
    V4Pact createUserUpdatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("username", "john.doe");
        body.stringType("fullName", "John Doe Updated");
        body.stringType("password", "UpdatedPassword123!");
        body.stringMatcher("version", "[0-9]+", "2");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a user updated event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "user_deleted-consumer")
    V4Pact createUserDeletedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("username", "john.doe");
        body.stringType("fullName", "John Doe");
        body.stringType("password", "Test123!");
        body.stringMatcher("version", "[0-9]+", "3");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a user deleted event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    //
    // Test method signatures for contract generation
    //

    @Test
    @PactTestFor(pactMethod = "createReaderUserRequestedPact")
    void testReaderUserRequested(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createUserPendingCreatedPact")
    void testUserPendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createReaderPendingCreatedPact")
    void testReaderPendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
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
    @PactTestFor(pactMethod = "createUserCreatedPact")
    void testUserCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createUserUpdatedPact")
    void testUserUpdated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createUserDeletedPact")
    void testUserDeleted(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in ReadersCDCConsumerIT
    }
}
