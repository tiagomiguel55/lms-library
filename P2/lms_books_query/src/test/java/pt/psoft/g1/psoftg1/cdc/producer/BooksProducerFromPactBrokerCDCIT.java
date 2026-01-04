package pt.psoft.g1.psoftg1.cdc.producer;

import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.cdc.config.CDCTestConfiguration;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;

/**
 * Testes CDC do Produtor para Books Query usando Pact Broker
 * Verifica se o serviço consegue produzir mensagens que atendem aos contratos do broker
 */
@SpringBootTest(classes = {CDCTestConfiguration.class})
@ActiveProfiles("cdc-test")
@Provider("book_event-producer")
@PactBroker(url = "http://localhost:9292", authentication = @PactBrokerAuth(username = "", password = ""))
public class BooksProducerFromPactBrokerCDCIT {

    @MockBean
    BookService bookService;

    @MockBean
    BookEventsPublisher bookEventsPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    // Estados para os contratos de livros - same as local version

    @State("a book created event")
    public String bookCreatedEvent() throws Exception {
        var bookMessage = BookMessageBuilder.createSampleBookCreatedMessage();
        return objectMapper.writeValueAsString(bookMessage);
    }

    @State("a book updated event")
    public String bookUpdatedEvent() throws Exception {
        var bookMessage = BookMessageBuilder.createSampleBookUpdatedMessage();
        return objectMapper.writeValueAsString(bookMessage);
    }

    @State("a book deleted event")
    public String bookDeletedEvent() throws Exception {
        var bookMessage = BookMessageBuilder.createSampleBookDeletedMessage();
        return objectMapper.writeValueAsString(bookMessage);
    }

    @State("an author created event")
    public String authorCreatedEvent() throws Exception {
        var authorMessage = BookMessageBuilder.createSampleAuthorCreatedMessage();
        return objectMapper.writeValueAsString(authorMessage);
    }

    @State("a genre created event")
    public String genreCreatedEvent() throws Exception {
        var genreMessage = BookMessageBuilder.createSampleGenreCreatedMessage();
        return objectMapper.writeValueAsString(genreMessage);
    }

    @State("a book finalized event")
    public String bookFinalizedEvent() throws Exception {
        var bookFinalizedMessage = BookMessageBuilder.createSampleBookFinalizedMessage();
        return objectMapper.writeValueAsString(bookFinalizedMessage);
    }
}
