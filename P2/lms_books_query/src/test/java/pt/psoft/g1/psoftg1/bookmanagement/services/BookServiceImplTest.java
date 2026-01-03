package pt.psoft.g1.psoftg1.bookmanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookEventRepository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.ArrayList;
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
    private AuthorService authorService;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PendingBookEventRepository pendingBookEventRepository;

    @Mock
    private BookEventsPublisher bookEventsPublisher;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void createSkipsMissingAuthorsAndPublishesEvent() {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Test Title");
        request.setGenre("Fantasy");
        request.setDescription("Desc");
        request.setPhotoURI(null);
        List<Long> authors = new ArrayList<>();
        authors.add(1L);
        authors.add(2L);
        request.setAuthors(authors);

        Genre genre = new Genre("Fantasy");
        Author knownAuthor = new Author(1L, "Known", "Bio", null);

        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(knownAuthor));
        when(authorRepository.findByAuthorNumber(2L)).thenReturn(Optional.empty());
        when(genreRepository.findByString("Fantasy")).thenReturn(Optional.of(genre));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book created = bookService.create(request, "9780306406157");

        assertNotNull(created);
        assertEquals(1, created.getAuthors().size(), "Only known authors should be attached");
        verify(bookEventsPublisher).sendBookCreated(created);
    }

    @Test
    void updateKeepsExistingPhotoWhenPayloadMissing() {
        Author author = new Author(1L, "Known", "Bio", null);
        Genre genre = new Genre("Fantasy");
        Book existing = new Book("9780306406157", "Old Title", "Desc", genre, List.of(author), "photo.png");

        when(bookRepository.findByIsbn("9780306406157")).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBookRequest request = new UpdateBookRequest();
        request.setIsbn("9780306406157");
        request.setTitle("New Title");
        request.setPhotoURI("new-uri.png");
        request.setPhoto(null);
        request.setAuthors(null);

        Book updated = bookService.update(request, existing.getVersion());

        assertNotNull(updated.getPhoto(), "Photo should remain unchanged when no multipart payload is provided");
        assertEquals("photo.png", updated.getPhoto().getPhotoFile());
        assertEquals("New Title", updated.getTitle().toString());
        verify(bookRepository).save(existing);
        verify(bookEventsPublisher).sendBookUpdated(updated, existing.getVersion());
    }

    @Test
    void updateThrowsWhenBookMissing() {
        UpdateBookRequest request = new UpdateBookRequest();
        request.setIsbn("missing");
        when(bookRepository.findByIsbn("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookService.update(request, null));
    }
}
