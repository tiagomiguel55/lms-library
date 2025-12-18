package pt.psoft.g1.psoftg1.readermanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ReaderRabbitmqController {

    @Autowired
    private final ReaderService readerService;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private DirectExchange directExchange;

    @RabbitListener(queues = "#{validateReaderQueue.name}")
    public void receiveValidateReader(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            ValidateReaderRequest request = objectMapper.readValue(jsonReceived, ValidateReaderRequest.class);

            System.out.println(" [x] Received ValidateReader request: readerId=" + request.getReaderId() + 
                             ", correlationId=" + request.getCorrelationId());

            // Check if reader exists
            boolean exists = readerService.findByUsername(request.getReaderId()).isPresent();

            // Send response
            ReaderValidatedResponse response = new ReaderValidatedResponse(
                request.getReaderId(), exists, request.getCorrelationId()
            );
            String jsonResponse = objectMapper.writeValueAsString(response);
            template.convertAndSend(directExchange.getName(), "reader.validated", jsonResponse);

            System.out.println(" [x] Sent ReaderValidated response: exists=" + exists + 
                             ", correlationId=" + request.getCorrelationId());
        } catch (Exception ex) {
            System.out.println(" [x] Exception processing ValidateReader request: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }
}
