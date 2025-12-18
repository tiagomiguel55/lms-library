package pt.psoft.g1.psoftg1.bookmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class BookRabbitmqController {

    @Autowired
    private final BookService bookService;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private DirectExchange directExchange;

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Created.name}")
    public void receiveBookCreatedMsg(Message msg) {

        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);

            System.out.println(" [x] Received Book Created by AMQP: " + msg + ".");
            try {
                bookService.create(bookViewAMQP);
                System.out.println(" [x] New book inserted from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] Book already exists. No need to store it.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving book event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Updated.name}")
    public void receiveBookUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);

            System.out.println(" [x] Received Book Updated by AMQP: " + msg + ".");
            try {
                bookService.update(bookViewAMQP);
                System.out.println(" [x] Book updated from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] Book does not exists or wrong version. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving book event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Requested.name}")
    public void receiveBookRequested(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            BookRequestedEvent event = objectMapper.readValue(jsonReceived, BookRequestedEvent.class);

            System.out.println(" [x] Received Book Requested by AMQP:");
            System.out.println("     - Book ID: " + event.getBookId());
            System.out.println("     - Author Name: " + event.getAuthorName());
            System.out.println("     - Genre Name: " + event.getGenreName());

            // Log or process the book request
            // This could trigger analytics, recommendations, or other business logic
            System.out.println(" [x] Book request logged successfully.");
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving book requested event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener
    public void receive(String payload) {
        System.out.println(" [x] Received '" + payload + "'");
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Validate_Book.name}")
    public void receiveValidateBook(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            ValidateBookRequest request = objectMapper.readValue(jsonReceived, ValidateBookRequest.class);

            System.out.println(" [x] Received ValidateBook request: bookId=" + request.getBookId() + 
                             ", correlationId=" + request.getCorrelationId());

            // Check if book exists
            boolean exists = bookService.findByIsbn(request.getBookId()).isPresent();

            // Send response
            BookValidatedResponse response = new BookValidatedResponse(
                request.getBookId(), exists, request.getCorrelationId()
            );
            String jsonResponse = objectMapper.writeValueAsString(response);
            template.convertAndSend(directExchange.getName(), "book.validated", jsonResponse);

            System.out.println(" [x] Sent BookValidated response: exists=" + exists + 
                             ", correlationId=" + request.getCorrelationId());
        } catch (Exception ex) {
            System.out.println(" [x] Exception processing ValidateBook request: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Lending_Returned.name}")
    public void receiveLendingReturned(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            LendingReturnedEvent event = objectMapper.readValue(jsonReceived, LendingReturnedEvent.class);

            System.out.println(" [x] Received LendingReturned in BooksQuery: bookId=" + event.getBookId() + 
                             ", comment=" + event.getComment() + ", grade=" + event.getGrade());

            try {
                System.out.println(" [x] Updating book read model with comment and grade for: " + event.getBookId());
            } catch (Exception e) {
                System.out.println(" [x] Error updating book with comment and grade: " + e.getMessage());
            }
        } catch (Exception ex) {
            System.out.println(" [x] Exception receiving LendingReturned event in BooksQuery: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }


}