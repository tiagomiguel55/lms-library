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
import pt.psoft.g1.psoftg1.bookmanagement.api.BookSagaViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingDetailsView;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQPMapper;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.LendingNumber;
import pt.psoft.g1.psoftg1.lendingmanagement.publishers.LendingEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderSagaViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.lendingmanagement.api.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {LendingEventPublisher.class , LendingViewAMQPMapperImpl.class},
        properties = {
                "stubrunner.amqp.mockConnection=true",
                "spring.profiles.active=test"
        }
)
@Provider("lending_event-producer")
@PactFolder("target/pacts")
public class LendingEventPublisherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LendingEventPublisherTest.class);

    @Autowired
    private LendingEventPublisher lendingEventPublisher;

    @Autowired
    private LendingViewAMQPMapperImpl lendingViewAMQPMapper;

    @MockBean
    private RabbitTemplate template;

    @MockBean
    private DirectExchange direct;



    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(Pact pact, Interaction interaction, PactVerificationContext context) {
        if (!"a reader lending response event".equals(interaction.getDescription()) && !"a book lending response event".equals(interaction.getDescription())) {
            context.verifyInteraction();
        } else {
            LOGGER.warn("Ignoring unrelated interaction: {}", interaction.getDescription());
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @PactVerifyProvider("a lending created event")
    public MessageAndMetadata lendingCreated() throws JsonProcessingException {

        Book book = new Book( "9783161484100","title","description",null);

        Reader reader = Reader.newReader("john.doe@example.com", "Xuba438976", "John Doe");

        ReaderDetails readerDetails = new ReaderDetails(1, reader);

        LocalDate s = LocalDate.of(2024, 1,31-1);
        LocalDate r = LocalDate.of(2024,2,15+1);

        LendingNumber lendingNumber = new LendingNumber(1);

        Lending lending = new Lending(1,book,readerDetails,lendingNumber,s,r,r,200,"gen-11",true,true,"VAlIDATED",1L,"");

        lendingEventPublisher.sendLendingCreated(lending);

        LendingViewAMQP lendingViewAMQP = new LendingViewAMQP();

        lendingViewAMQP.setLendingNumber(lending.getLendingNumber());
        lendingViewAMQP.setIsbn(lending.getBook().getIsbn());
        lendingViewAMQP.setReaderNumber(lending.getReaderDetails().getReaderNumber());
        lendingViewAMQP.setStartDate(lending.getStartDate().toString());
        lendingViewAMQP.setReturnedDate(lending.getReturnedDate().toString());
        lendingViewAMQP.setLimitDate(lending.getLimitDate().toString());
        lendingViewAMQP.setGenId(lending.getGenId());
        lendingViewAMQP.setDaysOverdue(0);
        lendingViewAMQP.setDaysUntilReturn(14);
        lendingViewAMQP.setFineValueInCents(0);
        lendingViewAMQP.setVersion("1");


        System.out.println(lendingViewAMQP.getLendingNumber());

        Message<String> message = new LendingMessageBuilder().withLending(lendingViewAMQP).build();
        System.out.println(message.getPayload());
        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a lending updated event")
    public MessageAndMetadata lendingUpdated() throws JsonProcessingException {

        System.out.println("lendingUpdated");
        Book book = new Book( "9783161484100","title","description",null);

        Reader reader = Reader.newReader("john.doe@example.com", "Xuba438976", "John Doe");

        ReaderDetails readerDetails = new ReaderDetails(1, reader);

        LocalDate s = LocalDate.of(2024, 1,31-1);
        LocalDate r = LocalDate.of(2024,2,15+1);

        LendingNumber lendingNumber = new LendingNumber(1);

        Lending lending = new Lending(1,book,readerDetails,lendingNumber,s,r,r,200,"gen-11",true,true,"VAlIDATED",1L,"");

        lendingEventPublisher.sendLendingUpdated(lending, lending.getVersion());

        LendingViewAMQP lendingViewAMQP = new LendingViewAMQP();

        lendingViewAMQP.setLendingNumber(lending.getLendingNumber());
        lendingViewAMQP.setIsbn(lending.getBook().getIsbn());
        lendingViewAMQP.setReaderNumber(lending.getReaderDetails().getReaderNumber());
        lendingViewAMQP.setStartDate(lending.getStartDate().toString());
        lendingViewAMQP.setReturnedDate(lending.getReturnedDate().toString());
        lendingViewAMQP.setLimitDate(lending.getLimitDate().toString());
        lendingViewAMQP.setVersion("1");
        lendingViewAMQP.setGenId(lending.getGenId());
        lendingViewAMQP.setDaysOverdue(0);
        lendingViewAMQP.setDaysUntilReturn(14);
        lendingViewAMQP.setFineValueInCents(0);



        Message<String> message = new LendingMessageBuilder().withLending(lendingViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a lending deleted event")
    public MessageAndMetadata lendingDeleted() throws JsonProcessingException {
        System.out.println("lendingDeleted");
        Book book = new Book( "9783161484100","title","description",null);

        Reader reader = Reader.newReader("john.doe@example.com", "Xuba438976", "John Doe");

        ReaderDetails readerDetails = new ReaderDetails(1, reader);

        LocalDate s = LocalDate.of(2024, 1,31-1);
        LocalDate r = LocalDate.of(2024,2,15+1);

        LendingNumber lendingNumber = new LendingNumber(1);

        Lending lending = new Lending(1,book,readerDetails,lendingNumber,s,r,r,200,"gen-11",true,true,"VAlIDATED",1L,"");

        lendingEventPublisher.sendLendingDeleted(lending, lending.getVersion());

        LendingViewAMQP lendingViewAMQP = new LendingViewAMQP();

        lendingViewAMQP.setLendingNumber(lending.getLendingNumber());
        lendingViewAMQP.setIsbn(lending.getBook().getIsbn());
        lendingViewAMQP.setReaderNumber(lending.getReaderDetails().getReaderNumber());
        lendingViewAMQP.setStartDate(lending.getStartDate().toString());
        lendingViewAMQP.setReturnedDate(lending.getReturnedDate().toString());
        lendingViewAMQP.setLimitDate(lending.getLimitDate().toString());
        lendingViewAMQP.setVersion("1");
        lendingViewAMQP.setGenId(lending.getGenId());
        lendingViewAMQP.setDaysOverdue(0);
        lendingViewAMQP.setDaysUntilReturn(14);
        lendingViewAMQP.setFineValueInCents(0);


        Message<String> message = new LendingMessageBuilder().withLending(lendingViewAMQP).build();

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a book lending request event")
    public MessageAndMetadata bookCreatedInLending() throws JsonProcessingException {
        LendingDetailsView details = new LendingDetailsView();


        lendingEventPublisher.sendBookCreatedInLending(details, "2025/1");

        BookSagaViewAMQP bookSagaViewAMQP = new BookSagaViewAMQP();

        bookSagaViewAMQP.setLendingNumber("2025/1");
        bookSagaViewAMQP.setIsbn("912965338");
        bookSagaViewAMQP.setTitle("title");
        bookSagaViewAMQP.setDescription("description");
        bookSagaViewAMQP.setAuthorIds(List.of("1"));
        bookSagaViewAMQP.setGenre("Infantil");
        bookSagaViewAMQP.setVersion("1");


        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(bookSagaViewAMQP);

        Message<String> message = new GenericMessage<>(jsonString);

        return generateMessageAndMetadata(message);
    }

    @PactVerifyProvider("a reader lending request event")
    public MessageAndMetadata readerCreatedInLending() throws JsonProcessingException {
        LendingDetailsView details = new LendingDetailsView();


        lendingEventPublisher.sendReaderCreatedInLending(details, "2025/1");

        ReaderSagaViewAMQP readerSagaViewAMQP = new ReaderSagaViewAMQP();
        readerSagaViewAMQP.setLendingNumber("2025/1");
        readerSagaViewAMQP.setUsername("john.doe@example.com");
        readerSagaViewAMQP.setPassword("Xuba438976");
        readerSagaViewAMQP.setFullName("John Doe");
        readerSagaViewAMQP.setReaderNumber("2025/1");
        readerSagaViewAMQP.setBirthDate("2000-01-01");
        readerSagaViewAMQP.setPhoneNumber("919191919");
        readerSagaViewAMQP.setPhotoUrl("readerPhotoTest.jpg");
        readerSagaViewAMQP.setGdpr(true);
        readerSagaViewAMQP.setMarketing(true);
        readerSagaViewAMQP.setThirdParty(true);
        readerSagaViewAMQP.setInterestList(List.of("Infantil"));



        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(readerSagaViewAMQP);

        Message<String> message = new GenericMessage<>(jsonString);

        return generateMessageAndMetadata(message);
    }



    private MessageAndMetadata generateMessageAndMetadata(Message<String> message) {
        HashMap<String, Object> metadata = new HashMap<>();
        message.getHeaders().forEach((k, v) -> metadata.put(k, v));

        return new MessageAndMetadata(message.getPayload().getBytes(), metadata);
    }
}
