package pt.psoft.g1.psoftg1.cdc.producer;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
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
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderPendingCreated;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderUserRequestedEvent;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.api.UserPendingCreated;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;

import java.util.HashMap;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ReaderEventPublisher.class},
        properties = {
                "spring.profiles.active=cdc-test"
        }
)
@ActiveProfiles("cdc-test")
@Provider("reader_event-producer")
@PactBroker(
    url = "http://localhost:9292",
    authentication = @PactBrokerAuth(username = "pact_broker", password = "pact_broker")
)
public class ReadersProducerFromPactBrokerCDCIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadersProducerFromPactBrokerCDCIT.class);

    @Autowired
    ReaderEventPublisher readerEventPublisher;

    @MockBean
    RabbitTemplate template;

    @MockBean
    DirectExchange direct;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(Pact pact, Interaction interaction, PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

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
