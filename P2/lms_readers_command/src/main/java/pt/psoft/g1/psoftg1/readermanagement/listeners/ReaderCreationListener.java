package pt.psoft.g1.psoftg1.readermanagement.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderPendingCreated;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderUserRequestedEvent;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderMapper;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReaderCreationListener {

    private final ReaderRepository readerRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final ReaderMapper readerMapper;
    private final ForbiddenNameRepository forbiddenNameRepository;
    private final ReaderEventPublisher readerEventPublisher;

    @RabbitListener(queues = "reader.user.requested.reader")
    public void receiveReaderUserRequested(String jsonReceived) {
        try {
            System.out.println(" [x] Received message from reader.user.requested (Reader side): " + jsonReceived);

            ObjectMapper objectMapper = new ObjectMapper();
            ReaderUserRequestedEvent event = objectMapper.readValue(jsonReceived, ReaderUserRequestedEvent.class);

            System.out.println(" [x] Processing Reader-User Requested event (Reader side):");
            System.out.println("     - Reader Number: " + event.getReaderNumber());
            System.out.println("     - Username: " + event.getUsername());

            // Check if reader already exists
            if (readerRepository.findByReaderNumber(event.getReaderNumber()).isPresent()) {
                System.out.println(" [x] ❌ Reader already exists with reader number: " + event.getReaderNumber());
                return;
            }

            // Validate full name for forbidden words
            Iterable<String> words = List.of(event.getFullName().split("\\s+"));
            for (String word : words) {
                if (!forbiddenNameRepository.findByForbiddenNameIsContained(word).isEmpty()) {
                    System.out.println(" [x] ❌ Name contains forbidden word: " + word);
                    return;
                }
            }

            // Create temporary Reader user for authentication
            Reader user = new Reader(event.getUsername(), event.getPassword()); // Password will be encoded later

            // Create temporary ReaderDetails entity
            // Generate reader number parts
            String[] readerNumberParts = event.getReaderNumber().split("/");
            int readerSequence = Integer.parseInt(readerNumberParts[1]);

            ReaderDetails readerDetails = readerMapper.createReaderDetails(
                    readerSequence,
                    user,
                    createTempCreateReaderRequest(event),
                    event.getPhotoURI(),
                    List.of() // Empty interest list for now
            );

            ReaderDetails savedReader = readerRepository.save(readerDetails);
            System.out.println(" [x] ✅ Created temporary ReaderDetails with number: " + savedReader.getReaderNumber());

            // Send ReaderPendingCreated event
            ReaderPendingCreated readerPendingEvent = new ReaderPendingCreated(
                    event.getReaderNumber(),
                    savedReader.getReader().getId(), // readerId
                    event.getUsername(),
                    false // Not finalized yet
            );

            readerEventPublisher.sendReaderPendingCreated(readerPendingEvent);
            System.out.println(" [x] ✅ Sent ReaderPendingCreated event for reader number: " + event.getReaderNumber());

        } catch (Exception e) {
            System.out.println(" [x] ❌ Error processing reader-user requested event (Reader side): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest createTempCreateReaderRequest(ReaderUserRequestedEvent event) {
        pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest request =
            new pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest();
        request.setUsername(event.getUsername());
        request.setPassword(event.getPassword());
        request.setFullName(event.getFullName());
        request.setBirthDate(event.getBirthDate());
        request.setPhoneNumber(event.getPhoneNumber());
        request.setGdpr(event.isGdpr());
        request.setMarketing(event.isMarketing());
        request.setThirdParty(event.isThirdParty());
        // Photo will be handled separately via photoURI
        return request;
    }
}
