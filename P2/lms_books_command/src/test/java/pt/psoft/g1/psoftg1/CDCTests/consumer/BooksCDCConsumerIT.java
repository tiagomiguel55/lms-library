package pt.psoft.g1.psoftg1.CDCTests.consumer;

import au.com.dius.pact.core.model.*;
import au.com.dius.pact.core.model.messaging.Message;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.CDCTests.config.CDCTestConfiguration;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRabbitmqController;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookRequestRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {BookRabbitmqController.class, CDCTestConfiguration.class}
)
@ActiveProfiles("cdc-test")
public class BooksCDCConsumerIT {

    @MockBean
    BookRepository bookRepository;

    @MockBean
    AuthorRepository authorRepository;

    @MockBean
    GenreRepository genreRepository;

    @MockBean
    PendingBookRequestRepository pendingBookRequestRepository;

    @MockBean
    BookEventsPublisher bookEventsPublisher;

    @MockBean
    BookService bookService;

    @Autowired
    BookRabbitmqController listener;

    @Test
    void testBookCreatedMessageProcessing() throws Exception {
        // Use PactReader to load the Pact file
        File pactFile = new File("target/pacts/book_created-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock repository behavior
        when(bookRepository.findByIsbn(anyString())).thenReturn(Optional.empty());
        when(authorRepository.findByAuthorNumber(anyLong())).thenReturn(Optional.of(mock(Author.class)));
        when(genreRepository.findByString(anyString())).thenReturn(Optional.of(mock(Genre.class)));

        Book mockBook = mock(Book.class);
        when(mockBook.getIsbn()).thenReturn("9780140449136");
        when(bookService.create(any(BookViewAMQP.class))).thenReturn(mockBook);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            // Convert the Pact message to a String (JSON payload)
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            // Create a Spring AMQP Message with the JSON payload
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            // Simulate receiving the message in the RabbitMQ listener
            assertDoesNotThrow(() -> {
                listener.receiveBookCreatedMsg(message);
            });

            // Verify interactions with the mocked services
            verify(bookService, times(1)).create(any(BookViewAMQP.class));
        }
    }

    @Test
    void testBookUpdatedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/book_updated-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock repository behavior
        when(bookRepository.findByIsbn(anyString())).thenReturn(Optional.of(mock(Book.class)));
        when(authorRepository.findByAuthorNumber(anyLong())).thenReturn(Optional.of(mock(Author.class)));
        when(genreRepository.findByString(anyString())).thenReturn(Optional.of(mock(Genre.class)));

        Book mockBook = mock(Book.class);
        when(bookService.update(any(BookViewAMQP.class))).thenReturn(mockBook);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            assertDoesNotThrow(() -> {
                listener.receiveBookUpdated(message);
            });

            verify(bookService, times(1)).update(any(BookViewAMQP.class));
        }
    }

    @Test
    void testAuthorPendingCreatedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/author_pending_created-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            assertDoesNotThrow(() -> {
                listener.receiveAuthorPendingCreated(message);
            });
        }
    }

    @Test
    void testGenrePendingCreatedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/genre_pending_created-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            assertDoesNotThrow(() -> {
                listener.receiveGenrePendingCreated(message);
            });
        }
    }

    @Test
    void testAuthorCreationFailedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/author_creation_failed-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            assertDoesNotThrow(() -> {
                listener.receiveAuthorCreationFailed(message);
            });
        }
    }

    @Test
    void testGenreCreationFailedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/genre_creation_failed-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            assertDoesNotThrow(() -> {
                listener.receiveGenreCreationFailed(message);
            });
        }
    }

    @Test
    void testAuthorCreatedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/author_created-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            assertDoesNotThrow(() -> {
                listener.receiveAuthorCreated(message);
            });
        }
    }

    @Test
    void testGenreCreatedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/genre_created-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            assertDoesNotThrow(() -> {
                listener.receiveGenreCreated(message);
            });
        }
    }

    @Test
    void testBookFinalizedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/book_finalized-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

            assertDoesNotThrow(() -> {
                listener.receiveBookFinalized(message);
            });
        }
    }

    @Test
    void testLendingValidationRequestMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/lending_validation_request-consumer-book_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            assertDoesNotThrow(() -> {
                listener.handleBookValidationRequest(jsonReceived);
            });
        }
    }
}
