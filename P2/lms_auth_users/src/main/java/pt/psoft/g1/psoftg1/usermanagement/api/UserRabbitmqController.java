package pt.psoft.g1.psoftg1.usermanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.publishers.UserEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

/**
 * Centralized RabbitMQ Listener for User-related events
 *
 * Handles:
 * 1. SAGA: Reader + User creation (reader.user.requested.user)
 *
 * NOTE: Sync events (user.created, user.updated, user.deleted) are NOT needed
 * because all replicas share the same database.
 */
@Component
@RequiredArgsConstructor
public class UserRabbitmqController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher userEventPublisher;

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
