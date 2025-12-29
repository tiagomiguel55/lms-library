package pt.psoft.g1.psoftg1.bookmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class BookRabbitmqController {

    private final BookService bookService;

    // ========== BOOK EVENTS ==========

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

            System.out.println(" [QUERY] 📥 Received Book Updated: " + bookViewAMQP.getIsbn());

            bookService.update(bookViewAMQP);

            System.out.println(" [QUERY] ✅ Read model updated for book: " + bookViewAMQP.getIsbn());
        } catch (Exception ex) {
            System.out.println(" [QUERY] ❌ Error receiving book updated: " + ex.getMessage());
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

            System.out.println(" [QUERY] 📥 Received Book Finalized: " + event.getBookId());

            bookService.handleBookFinalized(event);

            // Removed misleading success message - the service logs the actual result
        } catch (Exception ex) {
            System.out.println(" [QUERY] ❌ Error receiving book finalized: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
