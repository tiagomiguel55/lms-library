package pt.psoft.g1.psoftg1.CDCTests.producer;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;

import java.util.HashMap;

import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderPendingCreated;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderUserRequestedEvent;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.api.UserPendingCreated;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;

/**
 * CDC Producer Test for lms_readers_command
 *
 * Verifies that this service produces messages that match the Pact contracts
 * defined by consumer services.
 *
 * Producer Events (what this service publishes):
 * 1. reader_user_requested - SAGA initiation
 * 2. user_pending_created - SAGA coordination
 * 3. reader_pending_created - SAGA coordination
 * 4. reader_created - CQRS sync to lms_readers_query
 * 5. reader_updated - CQRS sync to lms_readers_query
 * 6. reader_deleted - CQRS sync to lms_readers_query
 *
 * Note: User events (user_created, user_updated, user_deleted) are actually
 * produced by lms_auth_users, not by this service. They are included here
 * for contract verification purposes only.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ReaderEventPublisher.class}
)
@Provider("reader_event-producer")
@PactFolder("target/pacts")
public class ReadersProducerCDCIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadersProducerCDCIT.class);

    @Autowired
    ReaderEventPublisher readerEventPublisher;

    @MockBean
    RabbitTemplate template;

    @MockBean
    DirectExchange direct;

    @MockBean
    ReaderViewAMQPMapper readerViewAMQPMapper;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(Pact pact, Interaction interaction, PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    // ========================================
    // SAGA EVENTS (coordination between services)
    // ========================================

    @PactVerifyProvider("a reader user requested event")
    public MessageAndMetadata readerUserRequested() throws JsonProcessingException {
        ReaderUserRequestedEvent event = new ReaderUserRequestedEvent();
        event.setReaderNumber("2024/1");
        event.setUsername("john.doe");
        event.setPassword("Test123!");
        event.setFullName("John Doe");
        event.setBirthDate("1990-01-01");
        event.setPhoneNumber("+351912345678");
        event.setPhotoURI("http://example.com/photo.jpg");
        event.setGdpr(true);
        event.setMarketing(false);
        event.setThirdParty(false);

        Message<String> message = new ReaderMessageBuilder().withReaderUserRequested(event).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a user pending created event")
    public MessageAndMetadata userPendingCreated() throws JsonProcessingException {
        UserPendingCreated event = new UserPendingCreated();
        event.setReaderNumber("2024/1");
        event.setUserId("123");
        event.setUsername("john.doe");
        event.setFinalized(false);

        Message<String> message = new ReaderMessageBuilder().withUserPendingCreated(event).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a reader pending created event")
    public MessageAndMetadata readerPendingCreated() throws JsonProcessingException {
        ReaderPendingCreated event = new ReaderPendingCreated();
        event.setReaderNumber("2024/1");
        event.setReaderId("456");
        event.setUsername("john.doe");
        event.setFinalized(false);

        Message<String> message = new ReaderMessageBuilder().withReaderPendingCreated(event).build();

        return generateMessageAndMetadata(message);
    }

    // ========================================
    // CQRS SYNC EVENTS (command -> query)
    // These are consumed by lms_readers_query
    // ========================================

    @PactVerifyProvider("a reader created event")
    public MessageAndMetadata readerCreated() throws JsonProcessingException {
        ReaderViewAMQP readerViewAMQP = new ReaderViewAMQP();
        readerViewAMQP.setReaderNumber("2024/1");
        readerViewAMQP.setFullName("John Doe");
        readerViewAMQP.setPhoneNumber("+351912345678");
        readerViewAMQP.setVersion("1");

        Message<String> message = new ReaderMessageBuilder().withReader(readerViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a reader updated event")
    public MessageAndMetadata readerUpdated() throws JsonProcessingException {
        ReaderViewAMQP readerViewAMQP = new ReaderViewAMQP();
        readerViewAMQP.setReaderNumber("2024/1");
        readerViewAMQP.setFullName("John Doe Updated");
        readerViewAMQP.setPhoneNumber("+351987654321");
        readerViewAMQP.setVersion("2");

        Message<String> message = new ReaderMessageBuilder().withReader(readerViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a reader deleted event")
    public MessageAndMetadata readerDeleted() throws JsonProcessingException {
        ReaderViewAMQP readerViewAMQP = new ReaderViewAMQP();
        readerViewAMQP.setReaderNumber("2024/1");
        readerViewAMQP.setFullName("John Doe");
        readerViewAMQP.setPhoneNumber("+351912345678");
        readerViewAMQP.setVersion("3");

        Message<String> message = new ReaderMessageBuilder().withReader(readerViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    // ========================================
    // USER EVENTS (for reference only)
    // These are actually produced by lms_auth_users,
    // not by this service. Included for contract verification.
    // ========================================

    @PactVerifyProvider("a user created event")
    public MessageAndMetadata userCreated() throws JsonProcessingException {
        UserViewAMQP userViewAMQP = new UserViewAMQP();
        userViewAMQP.setUsername("john.doe");
        userViewAMQP.setFullName("John Doe");
        userViewAMQP.setPassword("Test123!");
        userViewAMQP.setVersion("1");

        Message<String> message = new ReaderMessageBuilder().withUser(userViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a user updated event")
    public MessageAndMetadata userUpdated() throws JsonProcessingException {
        UserViewAMQP userViewAMQP = new UserViewAMQP();
        userViewAMQP.setUsername("john.doe");
        userViewAMQP.setFullName("John Doe Updated");
        userViewAMQP.setPassword("UpdatedPassword123!");
        userViewAMQP.setVersion("2");

        Message<String> message = new ReaderMessageBuilder().withUser(userViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a user deleted event")
    public MessageAndMetadata userDeleted() throws JsonProcessingException {
        UserViewAMQP userViewAMQP = new UserViewAMQP();
        userViewAMQP.setUsername("john.doe");
        userViewAMQP.setFullName("John Doe");
        userViewAMQP.setPassword("Test123!");
        userViewAMQP.setVersion("3");

        Message<String> message = new ReaderMessageBuilder().withUser(userViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    private MessageAndMetadata generateMessageAndMetadata(Message<String> message) {
        HashMap<String, Object> metadata = new HashMap<>();
        message.getHeaders().forEach((k, v) -> metadata.put(k, v));

        return new MessageAndMetadata(message.getPayload().getBytes(), metadata);
    }
}
