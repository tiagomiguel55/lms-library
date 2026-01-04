package pt.psoft.g1.psoftg1.usermanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.publishers.UserEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.nio.charset.StandardCharsets;

/**
 * Centralized RabbitMQ Listener for User-related events
 *
 * Handles:
 * 1. SYNC EVENTS: User lifecycle events (user.created, user.updated, user.deleted)
 * 2. SAGA: Reader + User creation (reader.user.requested.user)
 *
 * NOTE: Sync events are processed to maintain data consistency across microservices.
 */
@Component
@RequiredArgsConstructor
public class UserRabbitmqController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher userEventPublisher;
    private final UserService userService;

    // ========================================
    // SYNC EVENTS: User lifecycle events
    // ========================================

    /**
     * Listens to user.created queue
     * Handles user creation synchronization events
     */
    @RabbitListener(queues = "user.created", concurrency = "1")
    public void receiveUserCreated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            UserViewAMQP event = objectMapper.readValue(jsonReceived, UserViewAMQP.class);

            System.out.println(" [x] [AUTH-USERS] Received User Created by AMQP: " + jsonReceived);
            try {
                // Process user creation event using UserService
                userService.handleUserCreated(event);
                System.out.println(" [x] [AUTH-USERS] ✅ User created event processed successfully");
            } catch (Exception e) {
                System.out.println(" [x] [AUTH-USERS] ❌ Error processing user created event: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(" [x] [AUTH-USERS] ❌ Exception receiving user created event from AMQP: " + ex.getMessage());
        }
    }

    /**
     * Listens to user.updated queue
     * Handles user update synchronization events
     */
    @RabbitListener(queues = "user.updated", concurrency = "1")
    public void receiveUserUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            UserViewAMQP event = objectMapper.readValue(jsonReceived, UserViewAMQP.class);

            System.out.println(" [x] [AUTH-USERS] Received User Updated by AMQP: " + jsonReceived);
            try {
                // Process user update event using UserService
                userService.handleUserUpdated(event);
                System.out.println(" [x] [AUTH-USERS] ✅ User updated event processed successfully");
            } catch (Exception e) {
                System.out.println(" [x] [AUTH-USERS] ❌ Error processing user updated event: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(" [x] [AUTH-USERS] ❌ Exception receiving user updated event from AMQP: " + ex.getMessage());
        }
    }

    /**
     * Listens to user.deleted queue
     * Handles user deletion synchronization events
     */
    @RabbitListener(queues = "user.deleted", concurrency = "1")
    public void receiveUserDeleted(String username) {
        try {
            System.out.println(" [x] [AUTH-USERS] Received User Deleted '" + username + "'");

            // Process user deletion event using UserService
            userService.handleUserDeleted(username);
            System.out.println(" [x] [AUTH-USERS] ✅ User deleted event processed successfully");
        } catch (Exception ex) {
            System.out.println(" [x] [AUTH-USERS] ❌ Exception processing user deleted event: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ========================================
    // SAGA: Reader + User Creation
    // ========================================

    /**
     * Listens to reader.user.requested.user queue
     * Creates a User (Reader) when a Reader+User creation is requested
     */
    @RabbitListener(queues = "reader.user.requested.user", concurrency = "1")
    public void receiveReaderUserRequested(String jsonReceived) {
        try {
            System.out.println(" [x] [AUTH-USERS] Received Reader-User creation request");

            ObjectMapper objectMapper = new ObjectMapper();
            ReaderUserRequestedEvent event = objectMapper.readValue(jsonReceived, ReaderUserRequestedEvent.class);

            System.out.println("     - Reader Number: " + event.getReaderNumber());
            System.out.println("     - Username: " + event.getUsername());

            // Check if user already exists
            if (userRepository.findByUsername(event.getUsername()).isPresent()) {
                System.out.println(" [x] [AUTH-USERS] ❌ User already exists with username: " + event.getUsername());
                // Still send event to allow SAGA to handle duplicates
                sendUserPendingCreatedEvent(event);
                return;
            }

            // Create User (Reader) entity with encoded password
            Reader user = Reader.newReader(
                    event.getUsername(),
                    passwordEncoder.encode(event.getPassword()),
                    event.getFullName()
            );

            // Save the user
            Reader savedUser = userRepository.save(user);
            System.out.println(" [x] [AUTH-USERS] ✅ Created User with ID: " + savedUser.getId());

            // Send UserPendingCreated event to notify SAGA coordinator
            UserPendingCreated userPendingEvent = new UserPendingCreated(
                    event.getReaderNumber(),
                    savedUser.getId().toString(),
                    event.getUsername(),
                    false // Not finalized yet
            );

            userEventPublisher.sendUserPendingCreated(userPendingEvent);
            System.out.println(" [x] [AUTH-USERS] ✅ Sent UserPendingCreated event");

        } catch (Exception e) {
            System.out.println(" [x] [AUTH-USERS] ❌ Error processing reader-user requested event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendUserPendingCreatedEvent(ReaderUserRequestedEvent event) {
        try {
            // Find existing user
            var existingUser = userRepository.findByUsername(event.getUsername());
            if (existingUser.isPresent()) {
                UserPendingCreated userPendingEvent = new UserPendingCreated(
                        event.getReaderNumber(),
                        existingUser.get().getId().toString(),
                        event.getUsername(),
                        false
                );
                userEventPublisher.sendUserPendingCreated(userPendingEvent);
                System.out.println(" [x] [AUTH-USERS] Sent UserPendingCreated for existing user");
            }
        } catch (Exception e) {
            System.out.println(" [x] [AUTH-USERS] Error sending event for existing user: " + e.getMessage());
        }
    }
}
