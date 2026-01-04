package pt.psoft.g1.psoftg1.CDCTests.consumerTests;

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderSagaViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.listeners.ReaderEventListener;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = ReaderEventListener.class)
@PactConsumerTest
@PactTestFor(providerName = "reader_event-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
public class ReaderEventListenerTest {

    @MockBean
    ReaderService readerService;

    @MockBean
    ReaderEventPublisher readerEventPublisher;

    @MockBean
    ReaderViewAMQPMapper readerViewAMQPMapper;

    @Autowired
    ReaderEventListener listener;

    @BeforeEach
    void setup() {
        when(readerViewAMQPMapper.toReaderViewAMQP((ReaderSagaViewAMQP) any())).thenReturn(new ReaderViewAMQP());
    }


    // Contrato para evento "Reader Created"
    @Pact(consumer = "reader_created-consumer")
    V4Pact createReaderCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("username", "john.doe@example.com");
        body.stringType("fullName", "John Doe");
        body.stringType("readerNumber", "2025/1");
        body.stringType("birthDate", "1990-01-01");
        body.stringType("phoneNumber", "912965338");
        body.stringType("photoUrl", "http://example.com/photo.jpg");
        body.booleanType("gdpr", true);
        body.booleanType("marketing", false);
        body.booleanType("thirdParty", true);
        body.array("interestList")
                .stringType("fiction")
                .closeArray();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader created event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    // Contrato para evento "Reader Updated"
    @Pact(consumer = "reader_updated-consumer")
    V4Pact createReaderUpdatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("username", "john.doe@example.com");
        body.stringType("fullName", "John Doe");
        body.stringType("readerNumber", "2025/1");
        body.stringType("birthDate", "1990-01-01");
        body.stringType("phoneNumber", "912965338");
        body.stringType("photoUrl", "http://example.com/photo.jpg");
        body.booleanType("gdpr", true);
        body.booleanType("marketing", false);
        body.booleanType("thirdParty", true);
        body.array("interestList")
                .stringType("fiction")
                .closeArray();


        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader updated event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    // Contrato para evento "Reader Deleted"
    @Pact(consumer = "reader_deleted-consumer")
    V4Pact createReaderDeletedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("username", "john.doe@example.com");
        body.stringType("fullName", "John Doe");
        body.stringType("password", "password");
        body.stringType("readerNumber", "2025/1");
        body.stringType("birthDate", "1990-01-01");
        body.stringType("phoneNumber", "912965338");
        body.stringType("photoUrl", "http://example.com/photo.jpg");
        body.booleanType("gdpr", true);
        body.booleanType("marketing", false);
        body.booleanType("thirdParty", true);
        body.array("interestList")
                .stringType("fiction")
                .closeArray();



        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader deleted event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    // Contrato para evento "Reader Lending Request"
    @Pact(consumer = "reader_lending_request-consumer")
    V4Pact createReaderLendingRequestPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingNumber", "2025/1");
        body.stringType("username", "john.doe@example.com");
        body.stringType("fullName", "John Doe");
        body.stringType("readerNumber", "2025/1");
        body.stringType("birthDate", "1990-01-01");
        body.stringType("phoneNumber", "912965338");
        body.stringType("photoUrl", "http://example.com/photo.jpg");
        body.booleanType("gdpr", true);
        body.booleanType("marketing", false);
        body.booleanType("thirdParty", true);
        body.array("interestList")
                .stringType("fiction")
                .closeArray();


        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader lending request event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Pact(consumer = "reader_lending_response-consumer")
    V4Pact createReaderLendingResponsePact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("lendingNumber", "2025/1");
        body.stringType("status","SUCCESS");
        body.nullValue("error");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");

        return builder.expectsToReceive("a reader lending response event")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    // Teste do evento "Reader Created"
    @Test
    @PactTestFor(pactMethod = "createReaderCreatedPact")
    void testReaderCreated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
       String jsonReceived = messages.get(0).contentsAsString();
     
       MessageProperties messageProperties = new MessageProperties();
       messageProperties.setContentType("application/json");
       Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

       
       assertDoesNotThrow(() -> {
           listener.receiveReaderCreated(message);
       });
     
       verify(readerService, times(1)).create(any(ReaderViewAMQP.class));
    }

    // Teste do evento "Reader Updated"
    @Test
    @PactTestFor(pactMethod = "createReaderUpdatedPact")
    void testReaderUpdated(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        String jsonReceived = messages.get(0).contentsAsString();

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

        assertDoesNotThrow(() -> {
            listener.receiveReaderUpdated(message);
        });

        verify(readerService, times(1)).update(any(ReaderViewAMQP.class));
    }

    // Teste do evento "Reader Deleted"
    @Test
    @PactTestFor(pactMethod = "createReaderDeletedPact")
    void testReaderDeleted(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        String jsonReceived = messages.get(0).contentsAsString();

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);

        assertDoesNotThrow(() -> {
            listener.receiveReaderDeleted(jsonReceived);
        });

        verify(readerService, times(1)).delete(any(ReaderViewAMQP.class));
    }

    // Teste do evento "Reader Lending Request"
    @Test
    @PactTestFor(pactMethod = "createReaderLendingRequestPact")
    void testReaderLendingRequest(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
       String jsonReceived = messages.get(0).contentsAsString();

       MessageProperties messageProperties = new MessageProperties();
       messageProperties.setContentType("application/json");
       Message message = new Message(jsonReceived.getBytes(StandardCharsets.UTF_8), messageProperties);



       assertDoesNotThrow(() -> {
           listener.receiveReaderLendingRequest(message);
       });

       verify(readerService, times(1)).create(any(ReaderViewAMQP.class));
    }

    @Test
    @PactTestFor(pactMethod = "createReaderLendingResponsePact")
    void testReaderLendingResponse(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        // to create pact for producer test
    }
}
