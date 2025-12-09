package pt.psoft.g1.psoftg1.bookmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorPendingCreated;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.PendingBookRequest;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookRequestRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import org.springframework.amqp.core.Message;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookRabbitmqController {

    @Autowired
    private final BookService bookService;

    @Autowired
    private final BookRepository bookRepository;

    @Autowired
    private final AuthorRepository authorRepository;

    @Autowired
    private final GenreRepository genreRepository;

    @Autowired
    private final PendingBookRequestRepository pendingBookRequestRepository;

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Created.name}")
    public void receiveBookCreatedMsg(Message msg) {

        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);

            System.out.println(" [x] Received Book Created by AMQP: " + msg + ".");
            try {
                bookService.create(bookViewAMQP);
                System.out.println(" [x] New book inserted from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] Book already exists. No need to store it.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving book event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Book_Updated.name}")
    public void receiveBookUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);

            System.out.println(" [x] Received Book Updated by AMQP: " + msg + ".");
            try {
                bookService.update(bookViewAMQP);
                System.out.println(" [x] Book updated from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] Book does not exists or wrong version. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving book event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener
    public void receive(String payload) {
        System.out.println(" [x] Received '" + payload + "'");
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Pending_Created.name}")
    public void receiveAuthorPendingCreated(Message msg) {
        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            AuthorPendingCreated event = objectMapper.readValue(jsonReceived, AuthorPendingCreated.class);

            System.out.println(" [x] Received Author Pending Created by AMQP:");
            System.out.println("     - Book ID (ISBN): " + event.getBookId());
            System.out.println("     - Author ID: " + event.getAuthorId());
            System.out.println("     - Author Name: " + event.getAuthorName());
            System.out.println("     - Genre Name: " + event.getGenreName());

            try {
                // Check if book already exists
                String isbn = event.getBookId();
                Optional<Book> existingBook = bookRepository.findByIsbn(isbn);

                if (existingBook.isPresent()) {
                    System.out.println(" [x] Book already exists with ISBN: " + isbn);
                    return;
                }

                // Get the author by ID (author is always persisted at this point)
                Optional<Author> authorOpt = authorRepository.findByAuthorNumber(event.getAuthorId());
                if (authorOpt.isEmpty()) {
                    System.out.println(" [x] Author not found with ID: " + event.getAuthorId());
                    return;
                }

                Author author = authorOpt.get();
                List<Author> authors = new ArrayList<>();
                authors.add(author);

                System.out.println(" [x] Using author: " + author.getName() + " (ID: " + author.getAuthorNumber() +
                                 ", finalized: " + author.isFinalized() + ")");

                // Find or create Genre
                Genre genre = genreRepository.findByString(event.getGenreName())
                        .orElseGet(() -> {
                            Genre newGenre = new Genre(event.getGenreName());
                            return genreRepository.save(newGenre);
                        });

                // Create book with available information
                String title = "Book by " + event.getAuthorName() + " (" + event.getGenreName() + ")";
                String description = "Requested book - ISBN: " + isbn;

                Book newBook = new Book(isbn, title, description, genre, authors, null);
                Book savedBook = bookRepository.save(newBook);

                System.out.println(" [x] ✅ Book created successfully: " + savedBook.getIsbn() + " - " + savedBook.getTitle());

                // After creating the book, process the pending request
                processPendingRequest(isbn, author, genre);

            } catch (Exception e) {
                System.out.println(" [x] Error creating book with author information: " + e.getMessage());
                e.printStackTrace();
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving author pending created event from AMQP: '" + ex.getMessage() + "'");
            ex.printStackTrace();
        }
    }

    private void processPendingRequest(String isbn, Author author, Genre genre) {
        try {
            // Find the pending request for this book using bookId (ISBN)
            Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(isbn);

            if (pendingRequestOpt.isPresent()) {
                PendingBookRequest pendingRequest = pendingRequestOpt.get();

                // Update the request status to BOOK_CREATED
                pendingRequest.setStatus(PendingBookRequest.RequestStatus.BOOK_CREATED);
                pendingBookRequestRepository.save(pendingRequest);

                System.out.println(" [x] Processed pending request for ISBN: " + isbn);

                // Publish BOOK_FINALIZED event to notify that the book is finalized
                bookService.publishBookFinalized(author.getAuthorNumber(), author.getName(), isbn);

                // Optionally delete the pending request after successful completion
                // pendingBookRequestRepository.delete(pendingRequest);
            } else {
                System.out.println(" [x] No pending request found for ISBN: " + isbn);
            }
        } catch (Exception e) {
            System.out.println(" [x] Error processing pending request: " + e.getMessage());
            e.printStackTrace();
        }
    }

}