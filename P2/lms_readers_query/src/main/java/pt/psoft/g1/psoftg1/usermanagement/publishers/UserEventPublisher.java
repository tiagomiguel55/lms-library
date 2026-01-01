package pt.psoft.g1.psoftg1.usermanagement.publishers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.model.UserEvents;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    @Autowired
    private RabbitTemplate template;
    @Autowired
    private DirectExchange direct;

    public void sendUserCreated(UserViewAMQP event) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(event);
            this.template.convertAndSend(direct.getName(), UserEvents.USER_CREATED, jsonString);
            System.out.println(" [x] Sent User Created event: '" + jsonString + "'");
        } catch (Exception ex) {
            System.out.println(" [x] Exception sending user created event: '" + ex.getMessage() + "'");
        }
    }

    public void sendUserUpdated(UserViewAMQP event) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(event);
            this.template.convertAndSend(direct.getName(), UserEvents.USER_UPDATED, jsonString);
            System.out.println(" [x] Sent User Updated event: '" + jsonString + "'");
        } catch (Exception ex) {
            System.out.println(" [x] Exception sending user updated event: '" + ex.getMessage() + "'");
        }
    }

    public void sendUserDeleted(String username) {
        try {
            this.template.convertAndSend(direct.getName(), UserEvents.USER_DELETED, username);
            System.out.println(" [x] Sent User Deleted event: '" + username + "'");
        } catch (Exception ex) {
            System.out.println(" [x] Exception sending user deleted event: '" + ex.getMessage() + "'");
        }
    }
}
