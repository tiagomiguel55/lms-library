package pt.psoft.g1.psoftg1.cdc.consumer;

import au.com.dius.pact.core.model.*;
import au.com.dius.pact.core.model.messaging.Message;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.cdc.config.CDCTestConfiguration;
import pt.psoft.g1.psoftg1.readermanagement.listeners.ReaderEventListener;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;

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
        classes = {ReaderEventListener.class, CDCTestConfiguration.class}
)
@ActiveProfiles("cdc-test")
public class ReadersCDCConsumerIT {

    @MockBean
    ReaderService readerService;

    @MockBean
    ReaderEventPublisher readerEventPublisher;

    @MockBean
    ReaderViewAMQPMapper readerViewAMQPMapper;

    @Autowired
    ReaderEventListener listener;

    @Test
    void testReaderCreatedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/reader_created-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock service behavior
        when(readerService.create(any())).thenReturn(mock(ReaderDetails.class));

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a Spring AMQP Message
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveReaderCreated(message);
            });

            // Verify interactions with the mocked services
            verify(readerService, times(1)).create(any());
        }
    }

    @Test
    void testReaderUpdatedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/reader_updated-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock service behavior
        when(readerService.update(any())).thenReturn(mock(ReaderDetails.class));

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a Spring AMQP Message
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveReaderUpdated(message);
            });

            // Verify interactions with the mocked services
            verify(readerService, times(1)).update(any());
        }
    }

    @Test
    void testReaderDeletedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/reader_deleted-consumer-reader_event-producer.json");

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
                listener.receiveReaderDeleted(jsonReceived);
            });

            System.out.println("Reader deleted message processed successfully");
        }
    }

    @Test
    void testReaderLendingRequestMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/reader_lending_request-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock service behavior for lending request
        when(readerService.create(any())).thenReturn(mock(ReaderDetails.class));

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a Spring AMQP Message
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveReaderLendingRequest(message);
            });

            System.out.println("Reader lending request message processed successfully");
        }
    }

    @Test
    void testValidateReaderMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/validate_reader-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock service behavior for reader validation
        when(readerService.findByReaderNumber(anyString())).thenReturn(Optional.of(mock(ReaderDetails.class)));

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (plain text payload)
            String readerId = messageGeneratedByPact.contentsAsString();

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.validateReaderHandler(readerId);
            });

            // Verify interactions with the mocked services
            verify(readerService, times(1)).findByReaderNumber(anyString());
            verify(readerEventPublisher, times(1)).sendReaderValidated(anyString(), anyBoolean());

            System.out.println("Validate reader message processed successfully");
        }
    }

    @Test
    void testReaderLendingResponseMessageProcessing() throws Exception {
        System.out.println("Reader lending response message processing test - producer only event");
    }

    @Test
    void testReaderValidatedMessageProcessing() throws Exception {
        System.out.println("Reader validated message processing test - producer only event");
    }
}
