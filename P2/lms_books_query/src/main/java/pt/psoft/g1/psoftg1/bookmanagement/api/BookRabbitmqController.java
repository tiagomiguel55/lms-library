package pt.psoft.g1.psoftg1.bookmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class BookRabbitmqController {

    private final BookService bookService;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private DirectExchange directExchange;

    // Exchange dedicated to cross-service validations (shared with LendingsCmd)
    @Autowired
    @Qualifier("lmsDirectExchange")
    private DirectExchange lmsDirectExchange;

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Created.name}")
    public void receiveBookCreatedMsg(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);

            System.out.println(" [QUERY] 📥 Received Book Created: " + bookViewAMQP.getIsbn());

            bookService.create(bookViewAMQP);

            System.out.println(" [QUERY] ✅ Read model updated for book: " + bookViewAMQP.getIsbn());
        } catch (IllegalArgumentException e) {
            // Book já existe - normal em eventual consistency
            System.out.println(" [QUERY] ℹ️ Book already exists, skipping: " + e.getMessage());
        } catch (Exception ex) {
            System.out.println(" [QUERY] ❌ Error receiving book created: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Updated.name}")
    public void receiveBookUpdated(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);

            System.out.println("Received Book Updated: " + bookViewAMQP.getIsbn());

            bookService.update(bookViewAMQP);

            System.out.println("Read model updated for book: " + bookViewAMQP.getIsbn());
        } catch (Exception ex) {
            System.out.println("Error receiving book updated: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    // ========== BOOK FINALIZED EVENT ==========

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Finalized.name}")
    public void receiveBookFinalized(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            BookFinalizedEvent event = objectMapper.readValue(jsonReceived, BookFinalizedEvent.class);

            System.out.println(" [QUERY] 📥 Received Author Created: " + event.getName());

            // Se o evento tem bookId associado, atualiza o book
            if (event.getBookId() != null && !event.getBookId().isEmpty()) {
                bookService.handleAuthorCreated(event, event.getBookId());
                System.out.println(" [QUERY] ✅ Book updated with author: " + event.getName());
            } else {
                System.out.println(" [QUERY] ℹ️ Author created without associated book, skipping book update");
            }
        } catch (Exception ex) {
            System.out.println(" [QUERY] ❌ Error receiving author created: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    // ========== GENRE EVENTS ==========

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Created.name}")
    public void receiveGenreCreated(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
AuthorViewAMQP event = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

           System.out.println(" [QUERY] 📥 Received Genre Created: " + event.getGenre());

            // Se o evento tem bookId associado, atualiza o book
            if (event.getBookId() != null && !event.getBookId().isEmpty()) {
                bookService.handleGenreCreated(event, event.getBookId());
                System.out.println(" [QUERY] ✅ Book updated with genre: " + event.getGenre());
            } else {
                System.out.println(" [QUERY] ℹ️ Genre created without associated book, skipping book update");
            }
        } catch (Exception ex) {
            System.out.println(" [QUERY] ❌ Error receiving book finalized: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ========== VALIDATION FLOW (ValidateBook -> BookValidated) ==========
    @RabbitListener(queues = "#{validateBookQueue.name}")
    public void receiveValidateBook(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            ValidateBookRequest request = objectMapper.readValue(jsonReceived, ValidateBookRequest.class);

            boolean exists = false;
            try {
                // If the book exists, service returns it; otherwise it throws
                bookService.findByIsbn(request.getBookId());
                exists = true;
            } catch (Exception e) {
                exists = false;
            }

            BookValidatedResponse response = new BookValidatedResponse(
                request.getBookId(),
                exists,
                request.getCorrelationId()
            );

            String responseJson = new ObjectMapper().writeValueAsString(response);
            template.convertAndSend(lmsDirectExchange.getName(), "book.validated", responseJson);

            System.out.println(" [QUERY] 📤 Sent Book Validated: bookId=" + request.getBookId() +
                               ", exists=" + exists +
                               ", correlationId=" + request.getCorrelationId());
        } catch (Exception ex) {
            System.out.println(" [QUERY] ❌ Error processing ValidateBook: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
