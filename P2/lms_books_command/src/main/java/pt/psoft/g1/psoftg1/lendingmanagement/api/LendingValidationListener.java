package pt.psoft.g1.psoftg1.lendingmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import org.springframework.amqp.core.DirectExchange;

@Component
@RequiredArgsConstructor
public class LendingValidationListener {

    private final BookService bookService;
    private final RabbitTemplate rabbitTemplate;

    @Qualifier("direct")
    private final DirectExchange directExchange;

    @RabbitListener(queues = "lending.validation.request")
    public void handleLendingValidationRequest(String message) {
        try {
            System.out.println(" [BOOKS] 📥 Received lending validation request");

            ObjectMapper objectMapper = new ObjectMapper();
            LendingValidationRequest request = objectMapper.readValue(message, LendingValidationRequest.class);

            System.out.println(" [BOOKS] 🔍 Validating book with ISBN: " + request.getIsbn() +
                             " for lending: " + request.getLendingNumber());

            LendingValidationResponse response = new LendingValidationResponse();
            response.setRequestId(request.getRequestId());
            response.setLendingNumber(request.getLendingNumber());
            response.setIsbn(request.getIsbn());

            try {
                // Check if book exists
                bookService.findByIsbn(request.getIsbn());
                response.setBookExists(true);
                response.setMessage("Book exists and is available for lending");
                System.out.println(" [BOOKS] ✅ Book found: " + request.getIsbn());
            } catch (NotFoundException e) {
                response.setBookExists(false);
                response.setMessage("Book with ISBN " + request.getIsbn() + " does not exist");
                System.out.println(" [BOOKS] ❌ Book not found: " + request.getIsbn());
            }

            // Send response back to lending service
            String responseJson = objectMapper.writeValueAsString(response);
            rabbitTemplate.convertAndSend(directExchange.getName(), "lending.validation.response", responseJson);

            System.out.println(" [BOOKS] 📤 Sent validation response for lending: " + request.getLendingNumber() +
                             " (exists: " + response.isBookExists() + ")");

        } catch (Exception e) {
            System.err.println(" [BOOKS] ❌ Error processing lending validation request: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

