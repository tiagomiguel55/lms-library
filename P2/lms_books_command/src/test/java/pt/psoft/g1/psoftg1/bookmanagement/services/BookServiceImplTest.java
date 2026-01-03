package pt.psoft.g1.psoftg1.bookmanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.PendingBookRequest;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookRequestRepository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PendingBookRequestRepository pendingBookRequestRepository;

    @Mock
    private BookEventsPublisher bookEventsPublisher;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void createWithAuthorAndGenreReturnsExistingBookImmediately() {
        Book existing = new Book("9780134685991", "Title", "Desc", new Genre("Drama"), List.of(new Author("Name", "Bio", null)), null);
        when(bookRepository.findByIsbn("9780134685991")).thenReturn(Optional.of(existing));

        BookRequestedEvent event = new BookRequestedEvent("9780134685991", "Title", "Author", "Drama");
        Book result = bookService.createWithAuthorAndGenre(event);

        assertSame(existing, result);
        verify(pendingBookRequestRepository, never()).save(any());
        verify(bookEventsPublisher, never()).sendBookRequestedEvent(anyString(), anyString(), anyString());
    }

    @Test
    void createWithAuthorAndGenreReturnsBookWhenPendingAlreadyCreated() {
        PendingBookRequest pending = new PendingBookRequest("9780596007126", "T", "D", "Author", "Genre");
        pending.setStatus(PendingBookRequest.RequestStatus.BOOK_CREATED);
        Book created = new Book("9780596007126", "Title", "Desc", new Genre("Drama"), List.of(new Author("Name", "Bio", null)), null);

        when(bookRepository.findByIsbn("9780596007126")).thenReturn(Optional.empty(), Optional.of(created));
        when(pendingBookRequestRepository.findByBookId("9780596007126")).thenReturn(Optional.of(pending));

        BookRequestedEvent event = new BookRequestedEvent("9780596007126", "Title", "Author", "Genre");
        Book result = bookService.createWithAuthorAndGenre(event);

        assertSame(created, result);
        verify(pendingBookRequestRepository, never()).save(any());
        verify(bookEventsPublisher, never()).sendBookRequestedEvent(anyString(), anyString(), anyString());
    }

    @Test
    void updateKeepsExistingPhotoWhenPhotoUriMissing() {
        Genre genre = new Genre("Mystery");
        Author author = new Author("Alice", "Bio", null);
        String photoPath = "photos" + java.io.File.separator + "cover.png";
        Book book = new Book("9781617294945", "Old title", "Old desc", genre, List.of(author), photoPath);

        UpdateBookRequest request = new UpdateBookRequest("9781617294945", "Old title", null, List.of(1L), "Old desc");
        request.setPhoto(mock(MultipartFile.class));
        request.setPhotoURI(null);

        when(bookRepository.findByIsbn("9781617294945")).thenReturn(Optional.of(book));
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.empty());
        when(bookRepository.save(book)).thenReturn(book);

        Book result = bookService.update(request, null);

        assertSame(book, result);
        assertNotNull(book.getPhoto());
        assertEquals(photoPath, book.getPhoto().getPhotoFile());
        verify(bookEventsPublisher).sendBookUpdated(book, null);
    }

    @Test
    void updateThrowsWhenGenreNotFound() {
        Genre genre = new Genre("Drama");
        Author author = new Author("Alice", "Bio", null);
        Book book = new Book("9780321356680", "Title", "Desc", genre, List.of(author), null);

        UpdateBookRequest request = new UpdateBookRequest("9780321356680", "Title", "missing", List.of(1L), "Desc");

        when(bookRepository.findByIsbn("9780321356680")).thenReturn(Optional.of(book));
        when(genreRepository.findByString("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookService.update(request, book.getVersion()));
        verify(bookRepository, never()).save(any());
        verify(bookEventsPublisher, never()).sendBookUpdated(any(), any());
    }

    @Test
    void createWithAuthorAndGenreCreatesPendingAndPublishesWhenNew() {
        BookRequestedEvent event = new BookRequestedEvent("9780201633610", "Title", "Author", "Genre");

        when(bookRepository.findByIsbn("9780201633610")).thenReturn(Optional.empty());
        when(pendingBookRequestRepository.findByBookId("9780201633610")).thenReturn(Optional.empty());

        Book result = bookService.createWithAuthorAndGenre(event);

        assertNull(result);
        verify(pendingBookRequestRepository).save(any(PendingBookRequest.class));
        verify(bookEventsPublisher).sendBookRequestedEvent("9780201633610", "Author", "Genre");
    }
}
