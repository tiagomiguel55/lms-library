package pt.psoft.g1.psoftg1.genremanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.publishers.GenreEventsPublisher;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GenreRabbitmqController {

    private final GenreRepository genreRepository;
    private final GenreEventsPublisher genreEventsPublisher;
    private final GenreService genreService;

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Requested_Genre.name}")
    public void receiveBookRequested(Message msg) {
        String bookId = null;
        String authorName = null;
        String genreName = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            BookRequestedEvent bookRequestedEvent = objectMapper.readValue(jsonReceived, BookRequestedEvent.class);

            System.out.println(" [x] Received Book Requested by AMQP (GenreCmd): " + bookRequestedEvent.getBookId() +
                              " - Genre: " + bookRequestedEvent.getGenreName());

            genreName = bookRequestedEvent.getGenreName();
            bookId = bookRequestedEvent.getBookId();
            authorName = bookRequestedEvent.getAuthorName();

            // Check if genre already exists in the genres table
            Optional<Genre> existingGenre = genreRepository.findByString(genreName);

            if (existingGenre.isPresent()) {
                // Genre already exists, use it
                Genre genre = existingGenre.get();
                System.out.println(" [x] Genre already exists: " + genreName);

                // Publish GENRE_PENDING_CREATED
                genreEventsPublisher.sendGenrePendingCreated(genreName, bookId);
            } else {
                // Genre does NOT exist - create temporary genre with finalized=false (default)
                System.out.println(" [x] Creating temporary genre: " + genreName + " (finalized=false)");

                Genre newGenre = new Genre(genreName);
                // finalized defaults to false in the Genre entity
                newGenre = genreRepository.save(newGenre);


                System.out.println(" [x] ▶️ Continuing with book creation...");

                // Publish GENRE_PENDING_CREATED
                genreEventsPublisher.sendGenrePendingCreated(genreName, bookId);
            }

            System.out.println(" [x] Genre pending created event sent for book: " + bookId);
        }
        catch(Exception ex) {
            System.out.println(" [x] ❌ CRITICAL ERROR: Exception creating genre for request: '" + ex.getMessage() + "'");
            ex.printStackTrace();

            // SAGA COMPENSATION: Publish GENRE_CREATION_FAILED event
            if (bookId != null && genreName != null) {
                try {
                    System.out.println(" [x] 🔄 Publishing GENRE_CREATION_FAILED compensation event...");
                    genreEventsPublisher.sendGenreCreationFailed(bookId, genreName, ex.getMessage());
                    System.out.println(" [x] ✅ GENRE_CREATION_FAILED event published successfully");
                } catch (Exception publishEx) {
                    System.out.println(" [x] ❌ FATAL: Failed to publish GENRE_CREATION_FAILED event: " + publishEx.getMessage());
                    publishEx.printStackTrace();
                }
            } else {
                System.out.println(" [x] ⚠️ Cannot publish GENRE_CREATION_FAILED: Missing required information");
            }
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Finalized_Genre.name}")
    public void receiveBookFinalized(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent event =
                objectMapper.readValue(jsonReceived, pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent.class);

            System.out.println(" [x] Received Book Finalized by AMQP (GenreCmd):");
            System.out.println("     - Book ID: " + event.getBookId());
            System.out.println("     - Author ID: " + event.getAuthorId());
            System.out.println("     - Author Name: " + event.getAuthorName());
            System.out.println("     - Genre Name: " + event.getGenreName());

            try {
                String genreName = event.getGenreName();
                String bookId = event.getBookId();

                if (genreName == null || genreName.isEmpty()) {
                    System.out.println(" [x] ⚠️ No genre name in BOOK_FINALIZED event, skipping");
                    return;
                }

                // Find the genre
                Optional<Genre> genreOpt = genreRepository.findByString(genreName);
                if (genreOpt.isEmpty()) {
                    System.out.println(" [x] ⚠️ Genre not found: " + genreName);
                    return;
                }

                Genre genre = genreOpt.get();

                // Check if already finalized
                if (genre.isFinalized()) {
                    System.out.println(" [x] ℹ️ Genre already finalized: " + genreName);
                    // Don't publish event again
                    return;
                }

                // Finalize the genre
                System.out.println(" [x] 🔧 Finalizing genre: " + genreName);
                genreService.markGenreAsFinalized(genreName);
                System.out.println(" [x] ✅ Genre finalized: " + genreName);

                // Publish GENRE_CREATED event with bookId
                genreEventsPublisher.sendGenreCreated(genre, bookId);
                System.out.println(" [x] 📤 GENRE_CREATED event published for book: " + bookId);

            } catch (Exception e) {
                System.out.println(" [x] Error processing book finalized for genre: " + e.getMessage());
                e.printStackTrace();
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving book finalized event from AMQP (GenreCmd): '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }
}
