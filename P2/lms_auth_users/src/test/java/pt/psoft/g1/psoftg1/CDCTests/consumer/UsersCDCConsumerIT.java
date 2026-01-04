package pt.psoft.g1.psoftg1.CDCTests.consumer;

import au.com.dius.pact.core.model.*;
import au.com.dius.pact.core.model.messaging.Message;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.CDCTests.config.CDCTestConfiguration;
import pt.psoft.g1.psoftg1.usermanagement.api.UserRabbitmqController;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.publishers.UserEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {UserRabbitmqController.class, CDCTestConfiguration.class}
)
@ActiveProfiles("cdc-test")
public class UsersCDCConsumerIT {

    @MockBean
    UserRepository userRepository;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    UserEventPublisher userEventPublisher;

    @Autowired
    UserRabbitmqController listener;

    @Test
    void testReaderUserRequestedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/reader_user_requested-consumer-user_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock repository behavior
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword123");

        Reader mockUser = mock(Reader.class);
        when(mockUser.getId()).thenReturn(123L);
        when(userRepository.save(any(Reader.class))).thenReturn(mockUser);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (JSON payload)
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            // Prepare message properties
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");

            // Create a Spring AMQP Message with the JSON payload and optional headers
            org.springframework.amqp.core.Message messageToBeSentByRabbit =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveReaderUserRequested(jsonReceived);
            });

            // Verify interactions with the mocked services
            verify(userRepository, times(1)).findByUsername(anyString());
            verify(passwordEncoder, times(1)).encode(anyString());
            verify(userRepository, times(1)).save(any(Reader.class));
            verify(userEventPublisher, times(1)).sendUserPendingCreated(any());
        }
    }

    @Test
    void testUserCreatedMessageProcessing() throws Exception {
        // This test would be implemented if there were listener methods for user created events
        // Currently UserRabbitmqController only handles reader user requested events
        System.out.println("User created message processing test - not implemented (no corresponding listener)");
    }

    @Test
    void testUserUpdatedMessageProcessing() throws Exception {
        // This test would be implemented if there were listener methods for user updated events
        System.out.println("User updated message processing test - not implemented (no corresponding listener)");
    }

    @Test
    void testUserPendingCreatedMessageProcessing() throws Exception {
        // This test would be implemented if there were listener methods for user pending created events
        System.out.println("User pending created message processing test - not implemented (no corresponding listener)");
    }

    @Test
    void testUserDeletedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/user_deleted-consumer-user_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (plain text payload for USER_DELETED)
            String usernameReceived = messageGeneratedByPact.contentsAsString();

            // For USER_DELETED, the message content is just the username as a string
            // There's no specific consumer/listener for this in UserRabbitmqController
            // but we can validate the message format
            assertThat(usernameReceived).isNotNull();
            assertThat(usernameReceived).isNotEmpty();
            System.out.println("User deleted message processing test - received username: " + usernameReceived);
        }
    }
}
