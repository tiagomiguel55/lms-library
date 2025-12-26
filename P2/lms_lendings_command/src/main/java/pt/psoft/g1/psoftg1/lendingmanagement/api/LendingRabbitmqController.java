package pt.psoft.g1.psoftg1.lendingmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.DirectExchange;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LendingRabbitmqController {

    @Autowired
    private final LendingService lendingService;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    @Qualifier("directExchange")
    private DirectExchange directExchange;

    // Store pending validations by correlation ID
    private final Map<String, ValidationState> pendingValidations = new ConcurrentHashMap<>();

    @RabbitListener(queues = "#{bookValidatedQueue.name}")
    public void receiveBookValidated(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            BookValidatedResponse response = objectMapper.readValue(jsonReceived, BookValidatedResponse.class);

            System.out.println(" [x] Received Book Validated: bookId=" + response.getBookId() + 
                             ", exists=" + response.isExists() + 
                             ", correlationId=" + response.getCorrelationId());

            // Update validation state
            ValidationState state = pendingValidations.get(response.getCorrelationId());
            if (state != null) {
                state.setBookValidated(true);
                state.setBookExists(response.isExists());
                checkAndCreateLending(response.getCorrelationId());
            }
        } catch (Exception ex) {
            System.out.println(" [x] Exception receiving book validated event: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{readerValidatedQueue.name}")
    public void receiveReaderValidated(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            ReaderValidatedResponse response = objectMapper.readValue(jsonReceived, ReaderValidatedResponse.class);

            System.out.println(" [x] Received Reader Validated: readerId=" + response.getReaderId() + 
                             ", exists=" + response.isExists() + 
                             ", correlationId=" + response.getCorrelationId());

            // Update validation state
            ValidationState state = pendingValidations.get(response.getCorrelationId());
            if (state != null) {
                state.setReaderValidated(true);
                state.setReaderExists(response.isExists());
                checkAndCreateLending(response.getCorrelationId());
            }
        } catch (Exception ex) {
            System.out.println(" [x] Exception receiving reader validated event: '" + ex.getMessage() + "'");
        }
    }

    public void publishValidateBook(String bookId, String correlationId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ValidateBookRequest request = new ValidateBookRequest(bookId, correlationId);
            String jsonString = objectMapper.writeValueAsString(request);

            template.convertAndSend(directExchange.getName(), "book.validate", jsonString);
            System.out.println(" [x] Sent ValidateBook request: bookId=" + bookId + 
                             ", correlationId=" + correlationId);
        } catch (Exception ex) {
            System.out.println(" [x] Exception sending ValidateBook request: '" + ex.getMessage() + "'");
        }
    }

    public void publishValidateReader(String readerId, String correlationId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ValidateReaderRequest request = new ValidateReaderRequest(readerId, correlationId);
            String jsonString = objectMapper.writeValueAsString(request);

            template.convertAndSend(directExchange.getName(), "reader.validate", jsonString);
            System.out.println(" [x] Sent ValidateReader request: readerId=" + readerId + 
                             ", correlationId=" + correlationId);
        } catch (Exception ex) {
            System.out.println(" [x] Exception sending ValidateReader request: '" + ex.getMessage() + "'");
        }
    }

    public String initiateLendingValidation(String bookId, String readerId) {
        String correlationId = java.util.UUID.randomUUID().toString();
        ValidationState state = new ValidationState(bookId, readerId);
        pendingValidations.put(correlationId, state);

        publishValidateBook(bookId, correlationId);
        publishValidateReader(readerId, correlationId);

        return correlationId;
    }

    private void checkAndCreateLending(String correlationId) {
        ValidationState state = pendingValidations.get(correlationId);
        if (state != null && state.isBookValidated() && state.isReaderValidated()) {
            if (state.isBookExists() && state.isReaderExists()) {
                System.out.println(" [x] Both book and reader validated successfully for correlationId=" + correlationId);
                // Here you would call the service to actually create the lending
                // lendingService.createValidatedLending(state.getBookId(), state.getReaderId());
            } else {
                System.out.println(" [x] Validation failed: book exists=" + state.isBookExists() + 
                                 ", reader exists=" + state.isReaderExists());
            }
            pendingValidations.remove(correlationId);
        }
    }

    // Inner class to track validation state
    @RequiredArgsConstructor
    private static class ValidationState {
        private final String bookId;
        private final String readerId;
        private boolean bookValidated = false;
        private boolean readerValidated = false;
        private boolean bookExists = false;
        private boolean readerExists = false;

        public String getBookId() {return bookId;}
        public String getReaderId() {return readerId;}
        public boolean isBookValidated() {return bookValidated;}
        public void setBookValidated(boolean bookValidated) {this.bookValidated = bookValidated;}
        public boolean isReaderValidated() {return readerValidated;}
        public void setReaderValidated(boolean readerValidated) {this.readerValidated = readerValidated;}
        public boolean isBookExists() {return bookExists;}
        public void setBookExists(boolean bookExists) {this.bookExists = bookExists;}
        public boolean isReaderExists() {return readerExists;}
        public void setReaderExists(boolean readerExists) {this.readerExists = readerExists;}
    }
}
