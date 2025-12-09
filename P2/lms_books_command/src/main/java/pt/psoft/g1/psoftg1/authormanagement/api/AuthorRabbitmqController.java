package pt.psoft.g1.psoftg1.authormanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.publishers.AuthorEventsPublisher;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthorRabbitmqController {

    private final AuthorRepository authorRepository;
    private final AuthorEventsPublisher authorEventsPublisher;
    private final AuthorService authorService;

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Requested.name}")
    public void receiveBookRequested(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            BookRequestedEvent bookRequestedEvent = objectMapper.readValue(jsonReceived, BookRequestedEvent.class);

            System.out.println(" [x] Received Book Requested by AMQP: " + bookRequestedEvent.getBookId() +
                              " - Author: " + bookRequestedEvent.getAuthorName());

            String authorName = bookRequestedEvent.getAuthorName();
            String bookId = bookRequestedEvent.getBookId();
            String genreName = bookRequestedEvent.getGenreName();

            // Check if author already exists in the authors table
            List<Author> existingAuthors = authorRepository.searchByNameName(authorName);

            if (existingAuthors != null && !existingAuthors.isEmpty()) {
                // Author already exists, use the first one
                Author author = existingAuthors.get(0);
                System.out.println(" [x] Author already exists: " + authorName + " (ID: " + author.getAuthorNumber() + ")");

                // Publish AUTHOR_PENDING_CREATED with existing author ID
                authorEventsPublisher.sendAuthorPendingCreated(author.getAuthorNumber(), bookId, authorName, genreName);
            } else {
                // Author does NOT exist - create temporary author with finalized=false (default)
                System.out.println(" [x] Creating temporary author: " + authorName + " (finalized=false)");

                Author newAuthor = new Author(authorName, "Bio for " + authorName, null);
                // finalized defaults to false in the Author entity, no need to set it explicitly
                newAuthor = authorRepository.save(newAuthor);

                System.out.println(" [x] Temporary author created with ID: " + newAuthor.getAuthorNumber());
                System.out.println(" [x] ⏸️ WAITING 10 SECONDS - Check database now to see finalized=false!");

                try {
                    Thread.sleep(10000); // Wait 10 seconds to allow checking DB
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println(" [x] Sleep interrupted: " + e.getMessage());
                }

                System.out.println(" [x] ▶️ Continuing with book creation...");

                // Publish AUTHOR_PENDING_CREATED with the new author ID
                authorEventsPublisher.sendAuthorPendingCreated(newAuthor.getAuthorNumber(), bookId, authorName, genreName);
            }

            System.out.println(" [x] Author pending created event sent for book: " + bookId);
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving book requested event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Finalized.name}")
    public void receiveBookFinalized(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            BookFinalizedEvent event = objectMapper.readValue(jsonReceived, BookFinalizedEvent.class);

            System.out.println(" [x] Received Book Finalized by AMQP:");
            System.out.println("     - Author ID: " + event.getAuthorId());
            System.out.println("     - Author Name: " + event.getAuthorName());
            System.out.println("     - Book ID: " + event.getBookId());

            try {
                // Find the author and mark as finalized
                Optional<Author> authorOpt = authorRepository.findByAuthorNumber(event.getAuthorId());

                if (authorOpt.isPresent()) {
                    Author author = authorOpt.get();

                    // Mark the author as finalized (confirms the book was created successfully)
                    authorService.markAuthorAsFinalized(event.getAuthorId());

                    System.out.println(" [x] ✅ Author marked as finalized: " + event.getAuthorName() +
                                     " (ID: " + event.getAuthorId() + ")");

                    // Publish AUTHOR_CREATED event to message broker WITH the associated bookId
                    authorEventsPublisher.sendAuthorCreated(author, event.getBookId());
                    System.out.println(" [x] AUTHOR_CREATED event published for: " + event.getAuthorName() +
                                     " (ID: " + event.getAuthorId() + ") with bookId: " + event.getBookId());
                } else {
                    System.out.println(" [x] ERROR: Author not found with ID: " + event.getAuthorId());
                }
            } catch (Exception e) {
                System.out.println(" [x] Error processing book finalized: " + e.getMessage());
                e.printStackTrace();
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving book finalized event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }
}
