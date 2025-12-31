package pt.psoft.g1.psoftg1.usermanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderUserRequestedEvent;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.publishers.UserEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

@Component
@RequiredArgsConstructor
public class UserRabbitmqController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher userEventPublisher;

    @RabbitListener(queues = "reader.user.requested.user")
    public void receiveReaderUserRequested(String jsonReceived) {
        try {
            System.out.println(" [x] Received message from reader.user.requested: " + jsonReceived);

            ObjectMapper objectMapper = new ObjectMapper();
            ReaderUserRequestedEvent event = objectMapper.readValue(jsonReceived, ReaderUserRequestedEvent.class);

            System.out.println(" [x] Processing Reader-User Requested event:");
            System.out.println("     - Reader Number: " + event.getReaderNumber());
            System.out.println("     - Username: " + event.getUsername());

            // Check if user already exists
            if (userRepository.findByUsername(event.getUsername()).isPresent()) {
                System.out.println(" [x] ❌ User already exists with username: " + event.getUsername());
                return;
            }

            // Create temporary User (Reader) entity
            Reader user = new Reader(
                    event.getUsername(),
                    passwordEncoder.encode(event.getPassword())
            );

            Reader savedUser = userRepository.save(user);
            System.out.println(" [x] ✅ Created temporary User with ID: " + savedUser.getId());

            // Send UserPendingCreated event
            UserPendingCreated userPendingEvent = new UserPendingCreated(
                    event.getReaderNumber(),
                    savedUser.getId().toString(),
                    event.getUsername(),
                    false // Not finalized yet
            );

            userEventPublisher.sendUserPendingCreated(userPendingEvent);
            System.out.println(" [x] ✅ Sent UserPendingCreated event for reader number: " + event.getReaderNumber());

        } catch (Exception e) {
            System.out.println(" [x] ❌ Error processing reader-user requested event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
