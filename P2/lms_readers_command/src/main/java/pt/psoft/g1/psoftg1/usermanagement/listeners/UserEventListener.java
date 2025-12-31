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

            System.out.println(" [x] Received User Created by AMQP in readers_command: " + jsonReceived);
            try {
                // Process user creation event - might need to create reader details or update local cache
                userService.handleUserCreated(userViewAMQP);
                System.out.println(" [x] User created event processed successfully in readers_command");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(" [x] Error processing user created event: " + e.getMessage());
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

            System.out.println(" [x] Received User Updated by AMQP in readers_command: " + jsonReceived);
            try {
                // Process user update event - might need to update reader details or local cache
                userService.handleUserUpdated(userViewAMQP);
                System.out.println(" [x] User updated event processed successfully in readers_command");
            } catch (Exception e) {
                System.out.println(" [x] Error processing user updated event: " + e.getMessage());
            }
        } catch (Exception ex) {
            System.out.println(" [x] Exception receiving user updated event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{userDeletedQueue.name}")
    public void receiveUserDeleted(String username) {
        System.out.println(" [x] Received User Deleted '" + username + "' in readers_command");
        try {
            // Process user deletion event - might need to handle reader cleanup
            userService.handleUserDeleted(username);
            System.out.println(" [x] User deleted event processed successfully in readers_command");
        } catch (Exception ex) {
            System.out.println(" [x] Exception processing user deleted event: '" + ex.getMessage() + "'");
        }
    }
}
