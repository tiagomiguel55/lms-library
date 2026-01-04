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

/**
 * CDC Pact Definition Test for lms_readers_command
 *
 * Defines the message contracts (Pacts) that this service expects to consume.
 * Only includes contracts for messages that are actually consumed by ReaderRabbitmqController.
 *
 * Consumer Events (what this service receives):
 * 1. reader_user_requested - SAGA Step 1: Initiates Reader-User creation
 * 2. user_pending_created - SAGA Step 2: User creation confirmation from lms_auth_users
 * 3. reader_pending_created - SAGA Step 3: Reader creation self-confirmation
 *
 * Note: reader_created, reader_updated, reader_deleted are PRODUCER-ONLY events
 * (consumed by lms_readers_query, not by this service)
 */
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

    // ========================================
    // CONSUMER PACTS (Events we consume)
    // ========================================

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

    // ========================================
    // TEST METHODS (Contract Generation)
    // ========================================

    @Test
    @PactTestFor(pactMethod = "createReaderUserRequestedPact")
    void testReaderUserRequested(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createUserPendingCreatedPact")
    void testUserPendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation in ReadersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createReaderPendingCreatedPact")
    void testReaderPendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation in ReadersCDCConsumerIT
    }
}
