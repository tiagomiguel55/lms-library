package pt.psoft.g1.psoftg1.lendingmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;

@Service
@RequiredArgsConstructor
public class LendingRabbitmqController {

    private final LendingService lendingService;

    /**
     * Listener para respostas de validação de livros do Books Command
     * Este é o único listener necessário para o novo fluxo assíncrono via RabbitMQ
     */
    @RabbitListener(queues = "lending.validation.response")
    public void receiveLendingValidationResponse(String message) {
        try {
            System.out.println(" [LENDING] 📥 Received book validation response");

            ObjectMapper objectMapper = new ObjectMapper();
            LendingValidationResponse response = objectMapper.readValue(message, LendingValidationResponse.class);

            System.out.println(" [LENDING] 📋 Processing validation for lending: " + response.getLendingNumber() +
                             " | Book exists: " + response.isBookExists());

            // Process the validation response in the service
            lendingService.processBookValidationResponse(response);

        } catch (Exception e) {
            System.err.println(" [LENDING] ❌ Error processing validation response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
