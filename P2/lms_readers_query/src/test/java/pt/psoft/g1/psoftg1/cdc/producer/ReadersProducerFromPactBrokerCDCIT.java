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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.cdc.config.CDCTestConfiguration;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.api.SagaCreationResponse;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderSagaViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;

import java.util.HashMap;

@Import(CDCTestConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ReaderEventPublisher.class},
        properties = {
                "stubrunner.amqp.mockConnection=true",
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

    @PactVerifyProvider("a reader lending request event")
    public MessageAndMetadata readerLendingRequest() throws JsonProcessingException {
        ReaderSagaViewAMQP event = new ReaderSagaViewAMQP();
        event.setLendingNumber("L2024/001");
        event.setReaderNumber("2024/1");
        event.setFullName("John Doe");
        event.setPhoneNumber("+351912345678");

        Message<String> message = new ReaderMessageBuilder().withReaderSagaView(event).build();

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

    @PactVerifyProvider("a reader lending response event")
    public MessageAndMetadata readerLendingResponse() throws JsonProcessingException {
        SagaCreationResponse response = new SagaCreationResponse();
        response.setLendingNumber("L2024/001");
        response.setStatus("SUCCESS");
        response.setError("");

        Message<String> message = new ReaderMessageBuilder().withSagaCreationResponse(response).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a reader validated event")
    public MessageAndMetadata readerValidated() throws JsonProcessingException {
        String validationResponse = "{\"readerId\":\"2024/1\",\"exists\":true}";

        Message<String> message = MessageBuilder.withPayload(validationResponse)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .build();

        return generateMessageAndMetadata(message);
    }

    private MessageAndMetadata generateMessageAndMetadata(Message<String> message) {
        HashMap<String, Object> metadata = new HashMap<>();
        message.getHeaders().forEach((k, v) -> metadata.put(k, v));

        return new MessageAndMetadata(message.getPayload().getBytes(), metadata);
    }
}
