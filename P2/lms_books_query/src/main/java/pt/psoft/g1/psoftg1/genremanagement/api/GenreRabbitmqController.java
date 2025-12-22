package pt.psoft.g1.psoftg1.genremanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GenreRabbitmqController {

    private final GenreRepository genreRepository;
    private final BookService bookService;

    @RabbitListener(queues = "#{autoDeleteQueue_Genre_Created.name}")
    public void receiveGenreCreatedMsg(Message msg) {

        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            GenreViewAMQP genreViewAMQP = objectMapper.readValue(jsonReceived, GenreViewAMQP.class);

            System.out.println(" [QUERY] 📥 Received Genre Created: " + genreViewAMQP.getGenre());

            // Check if genre already exists
            Optional<Genre> existing = genreRepository.findByString(genreViewAMQP.getGenre());
            if (existing.isPresent()) {
                System.out.println(" [QUERY] ℹ️ Genre already exists in query model: " + genreViewAMQP.getGenre());
                return;
            }

            // Create new genre in query model
            try {
                Genre newGenre = new Genre(genreViewAMQP.getGenre());
                genreRepository.save(newGenre);
                System.out.println(" [QUERY] ✅ Genre created in query model: " + genreViewAMQP.getGenre());

                // Process any pending books waiting for this genre
                bookService.processPendingBooksForGenre(genreViewAMQP.getGenre());
            } catch (Exception e) {
                System.out.println(" [QUERY] ❌ Error creating genre: " + e.getMessage());
            }
        }
        catch(Exception ex) {
            System.out.println(" [QUERY] ❌ Exception receiving genre event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Genre_Updated.name}")
    public void receiveGenreUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            GenreViewAMQP genreViewAMQP = objectMapper.readValue(jsonReceived, GenreViewAMQP.class);

            System.out.println(" [QUERY] 📥 Received Genre Updated: " + genreViewAMQP.getGenre());

            // Find and update genre
            Optional<Genre> existing = genreRepository.findByString(genreViewAMQP.getGenre());
            if (existing.isPresent()) {
                System.out.println(" [QUERY] ✅ Genre found and can be updated: " + genreViewAMQP.getGenre());
            } else {
                System.out.println(" [QUERY] ⚠️ Genre does not exist in query model. Nothing to update.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [QUERY] ❌ Exception receiving genre event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Genre_Deleted.name}")
    public void receiveGenreDeleted(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            GenreViewAMQP genreViewAMQP = objectMapper.readValue(jsonReceived, GenreViewAMQP.class);

            System.out.println(" [QUERY] 📥 Received Genre Deleted: " + genreViewAMQP.getGenre());

            // Find and delete genre
            Optional<Genre> existing = genreRepository.findByString(genreViewAMQP.getGenre());
            if (existing.isPresent()) {
                genreRepository.delete(existing.get());
                System.out.println(" [QUERY] ✅ Genre deleted from query model: " + genreViewAMQP.getGenre());
            } else {
                System.out.println(" [QUERY] ⚠️ Genre does not exist in query model. Nothing to delete.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [QUERY] ❌ Exception receiving genre event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }
}
