package pt.psoft.g1.psoftg1.usermanagement.publishers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.usermanagement.api.UserPendingCreated;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    @Autowired
    private RabbitTemplate template;
    @Autowired
    private DirectExchange direct;

    public void sendUserPendingCreated(UserPendingCreated event) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(direct.getName(), "user.pending.created", jsonString);

            System.out.println(" [x] Sent User Pending Created event: '" + jsonString + "'");
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending user pending created event: '" + ex.getMessage() + "'");
        }
    }
}
