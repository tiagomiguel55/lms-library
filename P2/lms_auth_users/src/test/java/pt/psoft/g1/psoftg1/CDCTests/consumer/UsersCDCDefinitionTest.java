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
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.psoft.g1.psoftg1.usermanagement.api.UserRabbitmqController;
import pt.psoft.g1.psoftg1.usermanagement.publishers.UserEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {UserRabbitmqController.class}
)
@PactConsumerTest
@PactTestFor(providerName = "user_event-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
public class UsersCDCDefinitionTest {

    @MockBean
    UserRepository userRepository;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    UserEventPublisher userEventPublisher;

    @Autowired
    UserRabbitmqController listener;

    @Pact(consumer = "reader_user_requested-consumer")
    V4Pact createReaderUserRequestedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("readerNumber", "2024/1");
        body.stringType("username", "john.doe");
        body.stringType("password", "Test123!");
        body.stringType("fullName", "John Doe");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader user requested event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "user_created-consumer")
    V4Pact createUserCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("username", "john.doe");
        body.stringType("fullName", "John Doe");
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
        body.stringMatcher("version", "[0-9]+", "2");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a user updated event")
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

    @Pact(consumer = "user_deleted-consumer")
    V4Pact createUserDeletedPact(MessagePactBuilder builder) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "text/plain; charset=utf-8");

        return builder.expectsToReceive("a user deleted event")
                .withMetadata(metadata)
                .withContent("john.doe")  // USER_DELETED sends just the username as string
                .toPact();
    }

    //
    // The following tests are now defined as IT tests, so that the definition of contract and the tests are decoupled.
    // Yet, while the body of the tests can be elsewhere, the method signature must be defined here so the contract is generated.
    //

    @Test
    @PactTestFor(pactMethod = "createReaderUserRequestedPact")
    void testReaderUserRequested(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in UsersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createUserCreatedPact")
    void testUserCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in UsersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createUserUpdatedPact")
    void testUserUpdated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in UsersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createUserPendingCreatedPact")
    void testUserPendingCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in UsersCDCConsumerIT
    }

    @Test
    @PactTestFor(pactMethod = "createUserDeletedPact")
    void testUserDeleted(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // Test implementation will be in UsersCDCConsumerIT
    }
}
