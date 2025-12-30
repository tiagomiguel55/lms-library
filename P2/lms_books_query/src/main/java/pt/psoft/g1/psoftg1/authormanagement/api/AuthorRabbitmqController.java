package pt.psoft.g1.psoftg1.authormanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorRabbitmqController {

    private final AuthorRepository authorRepository;
    private final BookService bookService;

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Created.name}")
    public void receiveAuthorCreatedMsg(Message msg) {

        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

            System.out.println(" [QUERY] 📥 Received Author Created: " + authorViewAMQP.getName() + " (ID: " + authorViewAMQP.getAuthorNumber() + ")");

            // Check if author already exists
            Optional<Author> existing = authorRepository.findByAuthorNumber(authorViewAMQP.getAuthorNumber());
            if (existing.isPresent()) {
                System.out.println(" [QUERY] ℹ️ Author already exists in query model: " + authorViewAMQP.getName());
                // Still try to process pending books in case they weren't processed before
                bookService.processPendingBooksForAuthor(authorViewAMQP.getAuthorNumber());
                return;
            }

            // Create new author in query model
            try {
                Author newAuthor = new Author(
                    authorViewAMQP.getAuthorNumber(),
                    authorViewAMQP.getName(),
                    "Author biography",
                    null
                );
                System.out.println(" [QUERY] 🔧 DEBUG: About to save author: " + authorViewAMQP.getName());
                System.out.println(" [QUERY] 🔧 DEBUG: AuthorRepository class: " + authorRepository.getClass().getName());
                Author savedAuthor = authorRepository.save(newAuthor);
                System.out.println(" [QUERY] 🔧 DEBUG: Author saved, returned ID: " + (savedAuthor != null ? savedAuthor.getId() : "null"));

                // Verify the author was actually persisted
                Optional<Author> verifyAuthor = authorRepository.findByAuthorNumber(authorViewAMQP.getAuthorNumber());
                System.out.println(" [QUERY] 🔧 DEBUG: Verification lookup: " + (verifyAuthor.isPresent() ? "FOUND" : "NOT FOUND"));

                System.out.println(" [QUERY] ✅ Author created in query model: " + savedAuthor.getName() + " (ID: " + savedAuthor.getAuthorNumber() + ")");

                // Process any pending books waiting for this author
                // Call this AFTER the author is saved and transaction is committed
                System.out.println(" [QUERY] 🔍 Checking for pending books for author ID: " + authorViewAMQP.getAuthorNumber());
                bookService.processPendingBooksForAuthor(authorViewAMQP.getAuthorNumber());
            } catch (Exception e) {
                System.out.println(" [QUERY] ❌ Error creating author: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
        catch(Exception ex) {
            System.out.println(" [QUERY] ❌ Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Updated.name}")
    public void receiveAuthorUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

            System.out.println(" [QUERY] 📥 Received Author Updated: " + authorViewAMQP.getName());

            // Find and update author
            Optional<Author> existing = authorRepository.findByAuthorNumber(authorViewAMQP.getAuthorNumber());
            if (existing.isPresent()) {
                System.out.println(" [QUERY] ✅ Author found and can be updated: " + authorViewAMQP.getName());
            } else {
                System.out.println(" [QUERY] ⚠️ Author does not exist in query model. Nothing to update.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [QUERY] ❌ Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Deleted.name}")
    public void receiveAuthorDeleted(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

            System.out.println(" [QUERY] 📥 Received Author Deleted: " + authorViewAMQP.getName());

            // Find and delete author
            Optional<Author> existing = authorRepository.findByAuthorNumber(authorViewAMQP.getAuthorNumber());
            if (existing.isPresent()) {
                authorRepository.delete(existing.get());
                System.out.println(" [QUERY] ✅ Author deleted from query model: " + authorViewAMQP.getName());
            } else {
                System.out.println(" [QUERY] ⚠️ Author does not exist in query model. Nothing to delete.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [QUERY] ❌ Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }
}