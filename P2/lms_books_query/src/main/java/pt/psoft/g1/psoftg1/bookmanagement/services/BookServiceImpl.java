package pt.psoft.g1.psoftg1.bookmanagement.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.model.*;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import lombok.RequiredArgsConstructor;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookEventRepository;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQP;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQP;
import org.springframework.dao.DuplicateKeyException;

@Service
@RequiredArgsConstructor
@PropertySource({ "classpath:config/library.properties" })
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;
    private final AuthorService authorService;
    private final PhotoRepository photoRepository;
    private final PendingBookEventRepository pendingBookEventRepository;

    private final BookEventsPublisher bookEventsPublisher;

    @Value("${suggestionsLimitPerGenre}")
    private long suggestionsLimitPerGenre;

    @Override
    public Book create(CreateBookRequest request, String isbn) {

        final String title = request.getTitle();
        final String description = request.getDescription();
        final String photoURI = request.getPhotoURI();
        final String genre = request.getGenre();
        final List<Long> authorIds = request.getAuthors();

        Book savedBook = create(isbn, title, description, photoURI, genre, authorIds);

        if( savedBook!=null ) {
            bookEventsPublisher.sendBookCreated(savedBook);
        }

        return savedBook;
    }

    @Override
    public Book create(BookViewAMQP bookViewAMQP) {

        final String isbn = bookViewAMQP.getIsbn();
        final String description = bookViewAMQP.getDescription();
        final String title = bookViewAMQP.getTitle();
        final String photoURI = null;
        final String genre = bookViewAMQP.getGenre();
        final List<Long> authorIds = bookViewAMQP.getAuthorIds();

        Book bookCreated = create(isbn, title, description, photoURI, genre, authorIds);

        return bookCreated;
    }

    private Book create( String isbn,
                            String title,
                            String description,
                            String photoURI,
                            String genreName,
                            List<Long> authorIds) {

        List<Author> authors = getAuthors(authorIds);

        final Genre genre = genreRepository.findByString(String.valueOf(genreName))
                .orElseThrow(() -> new NotFoundException("Genre not found"));

        Book newBook = new Book(isbn, title, description, genre, authors, photoURI);

        try {
            Book savedBook = bookRepository.save(newBook);
            return savedBook;
        } catch (DuplicateKeyException e) {
            // Book already exists, return the existing one
            return bookRepository.findByIsbn(isbn).orElseThrow(() -> new NotFoundException("Book not found"));
        }
    }

    @Override
    public Book update(UpdateBookRequest request, Long currentVersion) {

        var book = findByIsbn(request.getIsbn());

        List<Long> authorsId = request.getAuthors();

        MultipartFile photo = request.getPhoto();
        String photoURI = request.getPhotoURI();
        if (photo == null && photoURI != null || photo != null && photoURI == null) {
            photoURI = null;
        }

        String genreId = request.getGenre();
        String title = request.getTitle();
        String description = request.getDescription();

        Book updatedBook = update( book, currentVersion, title, description, photoURI, genreId, authorsId);
        if( updatedBook!=null ) {
            bookEventsPublisher.sendBookUpdated(updatedBook, currentVersion);
        }

        return updatedBook;
    }

    @Override
    public Book update(BookViewAMQP bookViewAMQP) {

        final Long version = bookViewAMQP.getVersion();
        final String isbn = bookViewAMQP.getIsbn();
        final String description = bookViewAMQP.getDescription();
        final String title = bookViewAMQP.getTitle();
        final String photoURI = null;
        final String genre = bookViewAMQP.getGenre();
        final List<Long> authorIds = bookViewAMQP.getAuthorIds();

        var book = findByIsbn(isbn);

        Book bookUpdated = update(book, version, title, description, photoURI, genre, authorIds);

        return bookUpdated;
    }

    private Book update( Book book,
                         Long currentVersion,
                         String title,
                         String description,
                         String photoURI,
                         String genreId,
                         List<Long> authorsId) {

        Genre genreObj = null;
        if (genreId != null) {
            Optional<Genre> genre = genreRepository.findByString(genreId);
            if (genre.isEmpty()) {
                throw new NotFoundException("Genre not found");
            }
            genreObj = genre.get();
        }

        List<Author> authors = new ArrayList<>();
        if (authorsId != null) {
            for (Long authorNumber : authorsId) {
                Optional<Author> temp = authorRepository.findByAuthorNumber(authorNumber);
                if (temp.isEmpty()) {
                    continue;
                }
                Author author = temp.get();
                authors.add(author);
            }
        }
        else
            authors = null;

        book.applyPatch(currentVersion, title, description, photoURI, genreObj, authors);

        Book updatedBook = bookRepository.save(book);

        return updatedBook;
    }

    @Override
    public Book removeBookPhoto(String isbn, long desiredVersion) {
        Book book = this.findByIsbn(isbn);
        String photoFile;
        try {
            photoFile = book.getPhoto().getPhotoFile();
        } catch (NullPointerException e) {
            throw new NotFoundException("Book did not have a photo assigned to it.");
        }

        book.removePhoto(desiredVersion);

        var deletedBook = bookRepository.save(book);
        if( deletedBook!=null ) {
            photoRepository.deleteByPhotoFile(photoFile);

            bookEventsPublisher.sendBookDeleted(deletedBook, desiredVersion);
        }
        return deletedBook;
    }

    @Override
    public List<Book> findByGenre(String genre) {
        return this.bookRepository.findByGenre(genre);
    }

    public List<Book> findByTitle(String title) {
        return bookRepository.findByTitle(title);
    }

    @Override
    public List<Book> findByAuthorName(String authorName) {
        return bookRepository.findByAuthorName(authorName);
    }

    public Book findByIsbn(String isbn) {
        return this.bookRepository.findByIsbn(isbn).orElseThrow(() -> new NotFoundException(Book.class, isbn));
    }

    @Override
    public List<Book> searchBooks(Page page, SearchBooksQuery query) {
        if (page == null) {
            page = new Page(1, 10);
        }
        if (query == null) {
            query = new SearchBooksQuery("", "", "");
        }
        return bookRepository.searchBooks(page, query);
    }



    private List<Author> getAuthors(List<Long> authorNumbers) {

        List<Author> authors = new ArrayList<>();
        for (Long authorNumber : authorNumbers) {

            Optional<Author> temp = authorRepository.findByAuthorNumber(authorNumber);
            if (temp.isEmpty()) {
                continue;
            }

            Author author = temp.get();
            authors.add(author);
        }

        return authors;
    }

    @Override
    public void handleAuthorCreated(AuthorViewAMQP authorViewAMQP) {
        // Delegate to AuthorService to handle author creation in the author bounded context
        Optional<Author> existing = authorRepository.findByAuthorNumber(authorViewAMQP.getAuthorNumber());
        if (!existing.isPresent()) {
            System.out.println(" [QUERY] ⚠️ Author not found in query model: " + authorViewAMQP.getName() + " (ID: " + authorViewAMQP.getAuthorNumber() + ")");
            System.out.println(" [QUERY] ℹ️ Author should be created by AuthorService. Waiting for eventual consistency...");
        } else {
            System.out.println(" [QUERY] ✅ Author exists in query model: " + authorViewAMQP.getName());
        }
    }

    @Override
    public void handleAuthorCreated(AuthorViewAMQP authorViewAMQP, String bookId) {
        // Check if author exists - if not, it will eventually be created by AuthorService
        Optional<Author> authorOpt = authorRepository.findByAuthorNumber(authorViewAMQP.getAuthorNumber());

        if (authorOpt.isEmpty()) {
            System.out.println(" [QUERY] ⚠️ Author not yet available in query model for book creation: " + authorViewAMQP.getName());
            System.out.println(" [QUERY] ℹ️ Waiting for author to be synced from AuthorService...");
            return;
        }

        // Check if book already exists
        if (bookRepository.findByIsbn(bookId).isPresent()) {
            System.out.println(" [QUERY] ℹ️ Book already exists: " + bookId);
            return;
        }

        // Try to create book with author and available genre
        try {
            Author author = authorOpt.get();

            // Try to find a suitable genre
            Optional<Genre> genreOpt = Optional.empty();
            for (Genre g : genreRepository.findAll()) {
                genreOpt = Optional.of(g);
                break;
            }
            if (genreOpt.isEmpty()) {
                System.out.println(" [QUERY] ⚠️ No genre available yet to create book with");
                return;
            }

            Genre genre = genreOpt.get();
            String title = "Book " + bookId;
            String description = "Book by " + author.getName();

            Book newBook = new Book(bookId, title, description, genre, List.of(author), null);
            bookRepository.save(newBook);
            System.out.println(" [QUERY] ✅ Book created with author: " + bookId + " by " + author.getName());
        } catch (Exception e) {
            System.out.println(" [QUERY] ⚠️ Could not create book with author: " + e.getMessage());
        }
    }

    @Override
    public void handleGenreCreated(GenreViewAMQP genreViewAMQP) {
        // Just verify if the genre exists - don't create it
        // Genre creation is the responsibility of GenreService in the genre bounded context
        Optional<Genre> existing = genreRepository.findByString(genreViewAMQP.getGenre());
        if (!existing.isPresent()) {
            System.out.println(" [QUERY] ⚠️ Genre not found in query model: " + genreViewAMQP.getGenre());
            System.out.println(" [QUERY] ℹ️ Genre should be created by GenreService. Waiting for eventual consistency...");
        } else {
            System.out.println(" [QUERY] ✅ Genre exists in query model: " + genreViewAMQP.getGenre());
        }
    }

    @Override
    public void handleGenreCreated(GenreViewAMQP genreViewAMQP, String bookId) {
        // Check if genre exists - if not, it will eventually be created by GenreService
        Optional<Genre> genreOpt = genreRepository.findByString(genreViewAMQP.getGenre());

        if (genreOpt.isEmpty()) {
            System.out.println(" [QUERY] ⚠️ Genre not yet available in query model for book creation: " + genreViewAMQP.getGenre());
            System.out.println(" [QUERY] ℹ️ Waiting for genre to be synced from GenreService...");
            return;
        }

        // Check if book already exists
        if (bookRepository.findByIsbn(bookId).isPresent()) {
            System.out.println(" [QUERY] ℹ️ Book already exists: " + bookId);
            return;
        }

        // Try to create book with genre and available author
        try {
            Genre genre = genreOpt.get();

            // Try to find a suitable author
            Optional<Author> authorOpt = Optional.empty();
            for (Author a : authorRepository.findAll()) {
                authorOpt = Optional.of(a);
                break;
            }
            if (authorOpt.isEmpty()) {
                System.out.println(" [QUERY] ⚠️ No author available yet to create book with");
                return;
            }

            Author author = authorOpt.get();
            String title = "Book " + bookId;
            String description = "Book in genre " + genre.getGenre();

            Book newBook = new Book(bookId, title, description, genre, List.of(author), null);
            bookRepository.save(newBook);
            System.out.println(" [QUERY] ✅ Book created with genre: " + bookId + " in " + genre.getGenre());
        } catch (Exception e) {
            System.out.println(" [QUERY] ⚠️ Could not create book with genre: " + e.getMessage());
        }
    }

    @Override
    public void handleBookFinalized(pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent event) {
        // The book might not exist yet due to out-of-order events
        // Try to find it, if not found, create it with the finalized info
        try {
            Optional<Book> existingBook = bookRepository.findByIsbn(event.getBookId());

            if (existingBook.isPresent()) {
                Book book = existingBook.get();
                System.out.println(" [QUERY] 📚 Book already finalized in read model: " + book.getIsbn() +
                                 " with author: " + event.getAuthorName() +
                                 " and genre: " + event.getGenreName());
            } else {
                // Book doesn't exist yet - create it with finalized info
                try {
                    Author author = authorRepository.findByAuthorNumber(event.getAuthorId())
                            .orElse(null);
                    Optional<Genre> genreOpt = genreRepository.findByString(event.getGenreName());

                    if (genreOpt.isEmpty()) {
                        System.out.println(" [QUERY] ⚠️ Genre not found for finalized book: " + event.getGenreName());
                        // Check if already pending before trying to save
                        if (pendingBookEventRepository.findByBookId(event.getBookId()).isEmpty()) {
                            // Store the pending event to process later when genre becomes available
                            savePendingBookEvent(event);
                            System.out.println(" [QUERY] 📝 Stored pending book event, waiting for genre: " + event.getGenreName());
                        } else {
                            System.out.println(" [QUERY] ℹ️ Pending book event already exists, skipping duplicate: " + event.getBookId());
                        }
                        return;
                    }

                    if (author == null) {
                        System.out.println(" [QUERY] ⚠️ Author not found for finalized book (ID: " + event.getAuthorId() + ")");
                        return;
                    }

                    Genre genre = genreOpt.get();
                    String title = "Book by " + event.getAuthorName();
                    String description = "Finalized book from event";

                    Book newBook = new Book(event.getBookId(), title, description, genre, List.of(author), null);
                    bookRepository.save(newBook);
                    System.out.println(" [QUERY] 📚 Book created from finalized event: " + event.getBookId() +
                                     " with author: " + event.getAuthorName() +
                                     " and genre: " + event.getGenreName());
                } catch (Exception e) {
                    System.out.println(" [QUERY] ⚠️ Could not create book from finalized event: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println(" [QUERY] ❌ Error handling book finalized: " + e.getMessage());
        }
    }

    /**
     * Process pending book events when a genre becomes available
     * This handles out-of-order event processing
     */
    public void processPendingBooksForGenre(String genreName) {
        List<PendingBookEvent> pendingEvents = pendingBookEventRepository.findByGenreName(genreName);

        if (pendingEvents.isEmpty()) {
            return;
        }

        System.out.println(" [QUERY] 🔄 Processing " + pendingEvents.size() + " pending book events for genre: " + genreName);

        for (PendingBookEvent pending : pendingEvents) {
            try {
                // Check if book already exists
                if (bookRepository.findByIsbn(pending.getBookId()).isPresent()) {
                    System.out.println(" [QUERY] ℹ️ Pending book already created: " + pending.getBookId());
                    pendingBookEventRepository.delete(pending);
                    continue;
                }

                // Get the genre (we know it exists now)
                Optional<Genre> genreOpt = genreRepository.findByString(genreName);
                if (genreOpt.isEmpty()) {
                    System.out.println(" [QUERY] ⚠️ Genre still not available: " + genreName);
                    continue;
                }

                // Get the author - if not available, keep the pending event for retry
                Optional<Author> authorOpt = authorRepository.findByAuthorNumber(pending.getAuthorId());
                if (authorOpt.isEmpty()) {
                    System.out.println(" [QUERY] ⏳ Author not yet available for pending book: " + pending.getBookId() +
                                     " (ID: " + pending.getAuthorId() + "), will retry when author is created");
                    // Don't delete - we'll try again when author is created
                    continue;
                }

                // Create the book with all required info
                Genre genre = genreOpt.get();
                Author author = authorOpt.get();

                Book newBook = new Book(
                    pending.getBookId(),
                    pending.getTitle(),
                    pending.getDescription(),
                    genre,
                    List.of(author),
                    null
                );

                bookRepository.save(newBook);
                System.out.println(" [QUERY] ✅ Pending book finalized and created: " + pending.getBookId() +
                                 " with author: " + pending.getAuthorName() +
                                 " and genre: " + genreName);

                // Remove from pending only after successful creation
                pendingBookEventRepository.delete(pending);

            } catch (Exception e) {
                System.out.println(" [QUERY] ⚠️ Could not process pending book event: " + e.getMessage());
            }
        }
    }

    /**
     * Process pending book events when an author becomes available
     * This handles out-of-order event processing where genre is ready but author isn't yet
     */
    public void processPendingBooksForAuthor(Long authorId) {
        List<PendingBookEvent> pendingEvents = pendingBookEventRepository.findAll();
        List<PendingBookEvent> relevantPending = new ArrayList<>();

        for (PendingBookEvent pending : pendingEvents) {
            if (pending.getAuthorId().equals(authorId)) {
                relevantPending.add(pending);
            }
        }

        if (relevantPending.isEmpty()) {
            return;
        }

        System.out.println(" [QUERY] 🔄 Processing " + relevantPending.size() + " pending book events for author ID: " + authorId);

        for (PendingBookEvent pending : relevantPending) {
            try {
                // Check if book already exists
                if (bookRepository.findByIsbn(pending.getBookId()).isPresent()) {
                    System.out.println(" [QUERY] ℹ️ Pending book already created: " + pending.getBookId());
                    pendingBookEventRepository.delete(pending);
                    continue;
                }

                // Get the genre
                Optional<Genre> genreOpt = genreRepository.findByString(pending.getGenreName());
                if (genreOpt.isEmpty()) {
                    System.out.println(" [QUERY] ⚠️ Genre still not available: " + pending.getGenreName());
                    continue;
                }

                // Get the author (we know it exists now)
                Optional<Author> authorOpt = authorRepository.findByAuthorNumber(authorId);
                if (authorOpt.isEmpty()) {
                    System.out.println(" [QUERY] ⚠️ Author still not available: " + authorId);
                    continue;
                }

                // Create the book with all required info
                Genre genre = genreOpt.get();
                Author author = authorOpt.get();

                Book newBook = new Book(
                    pending.getBookId(),
                    pending.getTitle(),
                    pending.getDescription(),
                    genre,
                    List.of(author),
                    null
                );

                bookRepository.save(newBook);
                System.out.println(" [QUERY] ✅ Pending book finalized and created: " + pending.getBookId() +
                                 " with author: " + pending.getAuthorName() +
                                 " and genre: " + pending.getGenreName());

                // Remove from pending only after successful creation
                pendingBookEventRepository.delete(pending);

            } catch (Exception e) {
                System.out.println(" [QUERY] ⚠️ Could not process pending book event: " + e.getMessage());
            }
        }
    }

    /**
     * Store a finalized book event that is waiting for its genre
     * Uses database constraints to prevent duplicates in concurrent scenarios
     */
    private void savePendingBookEvent(pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent event) {
        try {
            // Double-check before saving (helps reduce unnecessary DB calls)
            if (pendingBookEventRepository.findByBookId(event.getBookId()).isPresent()) {
                return;
            }

            PendingBookEvent pending = new PendingBookEvent(
                event.getBookId(),
                event.getGenreName(),
                event.getAuthorId(),
                event.getAuthorName(),
                "Book by " + event.getAuthorName(),
                "Finalized book from event"
            );

            pendingBookEventRepository.save(pending);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Duplicate key violation - expected in concurrent scenarios with duplicate events
            // This is normal: two threads try to insert the same pending event, one wins
            System.out.println(" [QUERY] ℹ️ Pending book event already stored (concurrent duplicate): " + event.getBookId());
        } catch (Exception e) {
            System.out.println(" [QUERY] ⚠️ Could not save pending book event: " + e.getMessage());
        }
    }
}
