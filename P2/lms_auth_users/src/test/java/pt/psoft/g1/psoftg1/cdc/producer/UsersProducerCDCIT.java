package pt.psoft.g1.psoftg1.cdc.producer;

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
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;

import pt.psoft.g1.psoftg1.cdc.config.CDCTestConfiguration;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;
import pt.psoft.g1.psoftg1.usermanagement.api.UserPendingCreated;
import pt.psoft.g1.psoftg1.usermanagement.api.ReaderUserRequestedEvent;
import pt.psoft.g1.psoftg1.usermanagement.publishers.UserEventPublisher;

@Import(CDCTestConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {UserEventPublisher.class},
        properties = {
                "stubrunner.amqp.mockConnection=true",
                "spring.profiles.active=cdc-test"
        }
)
@ActiveProfiles("cdc-test")
@Provider("user_event-producer")
@PactFolder("target/pacts")
public class UsersProducerCDCIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersProducerCDCIT.class);

    @Autowired
    UserEventPublisher userEventPublisher;

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

        Message<String> message = new UserMessageBuilder().withReaderUserRequested(event).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a user created event")
    public MessageAndMetadata userCreated() throws JsonProcessingException {
        UserViewAMQP userViewAMQP = new UserViewAMQP();
        userViewAMQP.setUsername("john.doe");
        userViewAMQP.setFullName("John Doe");
        userViewAMQP.setVersion("1");

        Message<String> message = new UserMessageBuilder().withUser(userViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a user updated event")
    public MessageAndMetadata userUpdated() throws JsonProcessingException {
        UserViewAMQP userViewAMQP = new UserViewAMQP();
        userViewAMQP.setUsername("john.doe");
        userViewAMQP.setFullName("John Doe Updated");
        userViewAMQP.setVersion("2");

        Message<String> message = new UserMessageBuilder().withUser(userViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a user pending created event")
    public MessageAndMetadata userPendingCreated() throws JsonProcessingException {
        UserPendingCreated event = new UserPendingCreated();
        event.setReaderNumber("2024/1");
        event.setUserId("123");
        event.setUsername("john.doe");
        event.setFinalized(false);

        Message<String> message = new UserMessageBuilder().withUserPendingCreated(event).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a user deleted event")
    public MessageAndMetadata userDeleted() throws JsonProcessingException {
        String deletedUsername = "john.doe";

        Message<String> message = new UserMessageBuilder().withDeletedUsername(deletedUsername).build();

        return generateMessageAndMetadata(message);
    }

    private MessageAndMetadata generateMessageAndMetadata(Message<String> message) {
        HashMap<String, Object> metadata = new HashMap<>();
        message.getHeaders().forEach((k, v) -> metadata.put(k, v));

        return new MessageAndMetadata(message.getPayload().getBytes(), metadata);
    }
}
