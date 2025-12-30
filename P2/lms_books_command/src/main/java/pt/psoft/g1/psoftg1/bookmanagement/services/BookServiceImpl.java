package pt.psoft.g1.psoftg1.bookmanagement.services;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.model.*;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookRequestRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@PropertySource({ "classpath:config/library.properties" })
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;
    private final PhotoRepository photoRepository;
    private final PendingBookRequestRepository pendingBookRequestRepository;

    private final BookEventsPublisher bookEventsPublisher;

    @Value("${suggestionsLimitPerGenre}")
    private long suggestionsLimitPerGenre;

    @Override
    @Transactional
    public Book createWithAuthorAndGenre(BookRequestedEvent request) {
        // Extract data from the event
        final String isbn = request.getBookId();
        final String authorName = request.getAuthorName();
        final String genreName = request.getGenreName();

        // Check if book already exists - if so, return it
        Optional<Book> existingBook = bookRepository.findByIsbn(isbn);
        if (existingBook.isPresent()) {
            System.out.println(" [x] Book already exists with ISBN: " + isbn);
            return existingBook.get(); // Return existing book
        }

        // Check if request is already pending
        Optional<PendingBookRequest> pendingRequest = pendingBookRequestRepository.findByBookId(isbn);
        if (pendingRequest.isPresent()) {
            PendingBookRequest pending = pendingRequest.get();
            System.out.println(" [x] Book creation request already pending for ISBN: " + isbn +
                             " (Status: " + pending.getStatus() + ")");

            // Check status and provide appropriate response
            if (pending.getStatus() == PendingBookRequest.RequestStatus.BOOK_CREATED) {
                // Book was already created, try to find it
                existingBook = bookRepository.findByIsbn(isbn);
                if (existingBook.isPresent()) {
                    return existingBook.get();
                }
            }

            // Request is still pending, return null to indicate async processing
            return null;
        }

        // Save pending request - we need to wait for both AuthorCmd and GenreCmd
        PendingBookRequest newPendingRequest = new PendingBookRequest(isbn, request.getTitle(), authorName, genreName);
        pendingBookRequestRepository.save(newPendingRequest);

        System.out.println(" [x] Saved pending book request for ISBN: " + isbn);

        // Publish BookRequestedEvent - BOTH AuthorCmd and GenreCmd will listen to this event
        System.out.println(" [x] Publishing Book Requested event for ISBN: " + isbn);
        bookEventsPublisher.sendBookRequestedEvent(isbn, authorName, genreName);

        // Return null - the controller will handle this by returning HTTP 202 Accepted
        // The actual book will be created asynchronously when both AuthorPendingCreated and GenrePendingCreated events arrive
        return null;
    }

    @Override
    @Transactional
    public Book create(CreateBookRequest request, String isbn) {

        final String title = request.getTitle();
        final String description = request.getDescription();
        final String photoURI = request.getPhotoURI();
        final String genre = request.getGenre();
        final List<Long> authorIds = request.getAuthors();

        Book savedBook = create(isbn, title, description, photoURI, genre, authorIds);

        if( savedBook!=null ) {
            bookEventsPublisher.sendBookCreated(savedBook);
            bookEventsPublisher.sendBookRequested(savedBook);
        }

        return savedBook;
    }

    @Override
    @Transactional
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
        } catch (ConstraintViolationException e) {
            // Book already exists, return the existing one
            return bookRepository.findByIsbn(isbn).orElseThrow(() -> new NotFoundException("Book not found"));
        }
    }

    @Override
    @Transactional
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
    @Transactional
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
        return bookRepository.findByAuthorName(authorName + "%");
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

    @Override
    @Transactional
    public void publishBookFinalized(Long authorId, String authorName, String bookId, String genreName) {
        System.out.println(" [x] Publishing BOOK_FINALIZED event for Author ID: " + authorId + " - Book ID: " + bookId + " - Genre: " + genreName);
        bookEventsPublisher.sendBookFinalizedEvent(authorId, authorName, bookId, genreName);
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

}
