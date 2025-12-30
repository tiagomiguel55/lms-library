package pt.psoft.g1.psoftg1.producerTests;

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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.messaging.support.GenericMessage;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.api.*;
import pt.psoft.g1.psoftg1.readermanagement.api.SagaCreationResponse;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.util.HashMap;
import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ReaderEventPublisher.class, ReaderViewAMQPMapperImpl.class},
        properties = {
                "stubrunner.amqp.mockConnection=true",
                "spring.profiles.active=test"
        }
)
@Provider("reader_event-producer")
@PactFolder("target/pacts")
public class ReaderEventPublisherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReaderEventPublisherTest.class);

    @Autowired
    private ReaderEventPublisher readerEventPublisher;

    @Autowired
    private ReaderViewAMQPMapperImpl readerViewAMQPMapper;

    @MockBean
    private RabbitTemplate template;

    @MockBean
    private DirectExchange direct;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(Pact pact, Interaction interaction, PactVerificationContext context) {
        if (!"a reader lending request event".equals(interaction.getDescription())) {
            context.verifyInteraction();
        } else {
            LOGGER.warn("Ignoring unrelated interaction: {}", interaction.getDescription());
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @PactVerifyProvider("a reader created event")
    public MessageAndMetadata readerCreated() throws JsonProcessingException {

        List<Genre> interestList = List.of(new Genre("Infantil"));

        Reader reader = Reader.newReader("john.doe@example.com", "Xuba438976", "John Doe");
        ReaderDetails readerDetails = new ReaderDetails(1, reader, "2000-01-01", "919191919", true,
                true,
                true,
                "readerPhotoTest.jpg",
                interestList);
        readerEventPublisher.sendReaderCreated(readerDetails);

        List<String> interestListS = List.of("Infantil");

        ReaderViewAMQP readerViewAMQP = new ReaderViewAMQP();
        readerViewAMQP.setUsername(readerDetails.getReader().getUsername());
        readerViewAMQP.setPassword(readerDetails.getReader().getPassword());
        readerViewAMQP.setFullName(readerDetails.getReader().getName().getName());
        readerViewAMQP.setReaderNumber(readerDetails.getReaderNumber());
        readerViewAMQP.setBirthDate(readerDetails.getBirthDate().toString());
        readerViewAMQP.setPhoneNumber(readerDetails.getPhoneNumber());
        readerViewAMQP.setPhotoUrl(readerDetails.getPhoto().getPhotoFile());
        readerViewAMQP.setGdpr(true);
        readerViewAMQP.setMarketing(true);
        readerViewAMQP.setThirdParty(true);
        readerViewAMQP.setInterestList(interestListS);

        Message<String> message = new ReaderMessageBuilder().withReader(readerViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a reader updated event")
    public MessageAndMetadata readerUpdated() throws JsonProcessingException {
        List<Genre> interestList = List.of(new Genre("Infantil"));

        Reader reader = Reader.newReader("john.doe@example.com", "Xuba438976", "John Doe");
        ReaderDetails readerDetails = new ReaderDetails(1, reader, "2000-01-01", "919191919", true,
                true,
                true,
                "readerPhotoTest.jpg",
                interestList);
        readerEventPublisher.sendReaderUpdated(readerDetails,1L);

        List<String> interestListS = List.of("Infantil");

        ReaderViewAMQP readerViewAMQP = new ReaderViewAMQP();
        readerViewAMQP.setUsername(readerDetails.getReader().getUsername());
        readerViewAMQP.setPassword(readerDetails.getReader().getPassword());
        readerViewAMQP.setFullName(readerDetails.getReader().getName().getName());
        readerViewAMQP.setReaderNumber(readerDetails.getReaderNumber());
        readerViewAMQP.setBirthDate(readerDetails.getBirthDate().toString());
        readerViewAMQP.setPhoneNumber(readerDetails.getPhoneNumber());
        readerViewAMQP.setPhotoUrl(readerDetails.getPhoto().getPhotoFile());
        readerViewAMQP.setGdpr(true);
        readerViewAMQP.setMarketing(true);
        readerViewAMQP.setThirdParty(true);
        readerViewAMQP.setInterestList(interestListS);

        Message<String> message = new ReaderMessageBuilder().withReader(readerViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a reader deleted event")
    public MessageAndMetadata readerDeleted() throws JsonProcessingException {
        List<Genre> interestList = List.of(new Genre("Infantil"));

        Reader reader = Reader.newReader("john.doe@example.com", "Xuba438976", "John Doe");
        ReaderDetails readerDetails = new ReaderDetails(1, reader, "2000-01-01", "919191919", true,
                true,
                true,
                "readerPhotoTest.jpg",
                interestList);
        readerEventPublisher.sendReaderDeleted(readerDetails,1L);

        List<String> interestListS = List.of("Infantil");

        ReaderViewAMQP readerViewAMQP = new ReaderViewAMQP();
        readerViewAMQP.setUsername(readerDetails.getReader().getUsername());
        readerViewAMQP.setPassword(readerDetails.getReader().getPassword());
        readerViewAMQP.setFullName(readerDetails.getReader().getName().getName());
        readerViewAMQP.setReaderNumber(readerDetails.getReaderNumber());
        readerViewAMQP.setBirthDate(readerDetails.getBirthDate().toString());
        readerViewAMQP.setPhoneNumber(readerDetails.getPhoneNumber());
        readerViewAMQP.setPhotoUrl(readerDetails.getPhoto().getPhotoFile());
        readerViewAMQP.setGdpr(true);
        readerViewAMQP.setMarketing(true);
        readerViewAMQP.setThirdParty(true);
        readerViewAMQP.setInterestList(interestListS);

        Message<String> message = new ReaderMessageBuilder().withReader(readerViewAMQP).build();

        return generateMessageAndMetadata(message);
    }


    @PactVerifyProvider("a reader lending response event")
    public MessageAndMetadata readerLendingResponse() throws JsonProcessingException {
        SagaCreationResponse response = new SagaCreationResponse();

        response.setLendingNumber("2025/1");
        response.setStatus("SUCCESS");
        response.setError(null);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(response);

        // Simula o envio de mensagem pelo publisher
        readerEventPublisher.sendReaderLendingResponse(response);

        // Constrói a mensagem simulada para Pact
        Message<String> message = new GenericMessage<>(jsonString);

        return generateMessageAndMetadata(message);
    }

    private MessageAndMetadata generateMessageAndMetadata(Message<String> message) {
        HashMap<String, Object> metadata = new HashMap<>();
        message.getHeaders().forEach((k, v) -> metadata.put(k, v));

        return new MessageAndMetadata(message.getPayload().getBytes(), metadata);
    }
}