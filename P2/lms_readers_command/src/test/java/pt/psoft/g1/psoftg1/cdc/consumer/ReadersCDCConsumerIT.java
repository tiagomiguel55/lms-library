package pt.psoft.g1.psoftg1.cdc.consumer;

import au.com.dius.pact.core.model.*;
import au.com.dius.pact.core.model.messaging.Message;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderRabbitmqController;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.repositories.PendingReaderUserRequestRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderMapper;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ReaderRabbitmqController.class}
)
@ActiveProfiles("cdc-test")
public class ReadersCDCConsumerIT {

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

    @Test
    void testReaderUserRequestedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/reader_user_requested-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock repository behavior
        when(readerRepository.findByReaderNumber(anyString())).thenReturn(Optional.empty());
        when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of());

        ReaderDetails mockReaderDetails = mock(ReaderDetails.class);
        Reader mockReader = mock(Reader.class);
        when(mockReader.getId()).thenReturn("123");
        when(mockReaderDetails.getReader()).thenReturn(mockReader);
        when(mockReaderDetails.getReaderNumber()).thenReturn("2024/1");

        when(readerMapper.createReaderDetails(anyInt(), any(), any(), anyString(), any())).thenReturn(mockReaderDetails);
        when(readerRepository.save(any(ReaderDetails.class))).thenReturn(mockReaderDetails);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (JSON payload)
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveReaderUserRequested(jsonReceived);
            });

            // Verify interactions with the mocked services
            verify(readerRepository, times(1)).findByReaderNumber(anyString());
            verify(forbiddenNameRepository, atLeastOnce()).findByForbiddenNameIsContained(anyString());
            verify(readerRepository, times(1)).save(any(ReaderDetails.class));
            verify(readerEventPublisher, times(1)).sendReaderPendingCreated(any());
        }
    }

    @Test
    void testUserPendingCreatedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/user_pending_created-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (JSON payload)
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveUserPendingCreated(jsonReceived);
            });

            System.out.println("User pending created message processed successfully");
        }
    }

    @Test
    void testReaderPendingCreatedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/reader_pending_created-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (JSON payload)
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveReaderPendingCreated(jsonReceived);
            });

            System.out.println("Reader pending created message processed successfully");
        }
    }

    @Test
    void testReaderCreatedMessageProcessing() throws Exception {
        System.out.println("Reader created message processing test - producer only event");
    }

    @Test
    void testReaderUpdatedMessageProcessing() throws Exception {
        System.out.println("Reader updated message processing test - producer only event");
    }

    @Test
    void testReaderDeletedMessageProcessing() throws Exception {
        System.out.println("Reader deleted message processing test - producer only event");
    }

    @Test
    void testUserCreatedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/user_created-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (JSON payload)
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            // Create AMQP message for testing
            org.springframework.amqp.core.Message amqpMessage = new org.springframework.amqp.core.Message(
                    jsonReceived.getBytes(StandardCharsets.UTF_8),
                    new MessageProperties()
            );

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveUserCreated(amqpMessage);
            });

            System.out.println("User created message processed successfully");
        }
    }

    @Test
    void testUserUpdatedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/user_updated-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (JSON payload)
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            // Create AMQP message for testing
            org.springframework.amqp.core.Message amqpMessage = new org.springframework.amqp.core.Message(
                    jsonReceived.getBytes(StandardCharsets.UTF_8),
                    new MessageProperties()
            );

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveUserUpdated(amqpMessage);
            });

            System.out.println("User updated message processed successfully");
        }
    }

    @Test
    void testUserDeletedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/user_deleted-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (JSON payload)
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveUserDeleted(jsonReceived);
            });

            System.out.println("User deleted message processed successfully");
        }
    }
}
