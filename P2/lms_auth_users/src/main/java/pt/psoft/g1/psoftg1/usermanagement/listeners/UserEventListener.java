package pt.psoft.g1.psoftg1.usermanagement.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final UserService userService;

    @RabbitListener(queues = "#{userCreatedQueue.name}")
    public void receiveUserCreated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            UserViewAMQP userViewAMQP = objectMapper.readValue(jsonReceived, UserViewAMQP.class);

            System.out.println(" [x] Received User Created by AMQP: " + msg + ".");
            try {
                userService.create(userViewAMQP);
                System.out.println(" [x] New user processed from AMQP: " + msg + ".");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(" [x] User already exists or error processing. No action taken.");
            }
        } catch (Exception ex) {
            System.out.println(" [x] Exception receiving user created event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{userUpdatedQueue.name}")
    public void receiveUserUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            UserViewAMQP userViewAMQP = objectMapper.readValue(jsonReceived, UserViewAMQP.class);

            System.out.println(" [x] Received User Updated by AMQP: " + msg + ".");
            try {
                userService.update(userViewAMQP);
                System.out.println(" [x] User updated from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] User does not exist or wrong version. Nothing updated.");
            }
        } catch (Exception ex) {
            System.out.println(" [x] Exception receiving user updated event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{userDeletedQueue.name}")
    public void receiveUserDeleted(String username) {
        System.out.println(" [x] Received User Deleted '" + username + "'");
        try {
            userService.delete(username);
            System.out.println(" [x] User deleted from AMQP: " + username + ".");
        } catch (Exception ex) {
            System.out.println(" [x] Exception deleting user from AMQP: '" + ex.getMessage() + "'");
        }
    }

    // Listener for requests from other modules (especially lms_readers_command)
    @RabbitListener(queues = "reader.user.requested.user")
    public void receiveUserRequest(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);

            System.out.println(" [x] Received User Request by AMQP: " + jsonReceived);

            // This would typically parse the request and respond with user data
            // The implementation would depend on the specific request format
            // and what data the requesting module needs

        } catch (Exception ex) {
            System.out.println(" [x] Exception processing user request from AMQP: '" + ex.getMessage() + "'");
        }
    }
}
