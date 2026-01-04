package pt.psoft.g1.psoftg1.unitTests.bookmanagement.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookController;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookView;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewMapper;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.bookmanagement.services.CreateBookRequest;
import pt.psoft.g1.psoftg1.bookmanagement.services.CreateBookWithAuthorAndGenreRequest;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.configuration.FeatureFlagService;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.api.ListResponse;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.shared.services.SearchRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService bookService;

    @Mock
    private ConcurrencyService concurrencyService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private BookViewMapper bookViewMapper;

    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private BookController controller;

    @Test
    void createReturnsCreatedResponseWithEtag() {
        bindRequestToContext("/api/books/9780134685991");
        CreateBookRequest request = new CreateBookRequest();
        request.setPhotoURI("should-be-cleared");

        Book book = new Book("9780134685991", "Title", "Desc", new Genre("Drama"), List.of(new Author("Alice", "Bio", null)), null);
        ReflectionTestUtils.setField(book, "version", 2L);

        BookView bookView = new BookView();
        bookView.setIsbn("9780134685991");

        when(featureFlagService.isFeatureEnabled(anyString())).thenReturn(true);
        when(fileStorageService.getRequestPhoto(null)).thenReturn(null);
        when(bookService.create(request, "9780134685991")).thenReturn(book);
        when(bookViewMapper.toBookView(book)).thenReturn(bookView);

        ResponseEntity<BookView> response = controller.create(request, "9780134685991");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("9780134685991", response.getBody().getIsbn());
        assertEquals("\"2\"", response.getHeaders().getETag());
        verify(bookService).create(request, "9780134685991");
    }

    @Test
    void createReturnsBadRequestOnException() {
        CreateBookRequest request = new CreateBookRequest();
        when(featureFlagService.isFeatureEnabled(anyString())).thenReturn(true);
        when(fileStorageService.getRequestPhoto(null)).thenReturn(null);
        when(bookService.create(any(), anyString())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<BookView> response = controller.create(request, "9780596007126");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createWithAuthorAndGenreAcceptedWhenAsync() {
        bindRequestToContext("/api/books/create-complete");
        CreateBookWithAuthorAndGenreRequest request = new CreateBookWithAuthorAndGenreRequest();
        request.setTitle("A Title");
        request.setAuthorName("Alice");
        request.setGenreName("Drama");

        when(featureFlagService.isFeatureEnabled(anyString())).thenReturn(true);
        when(bookService.createWithAuthorAndGenre(any())).thenReturn(null);

        ResponseEntity<?> response = controller.createWithAuthorAndGenre(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?,?> body = (Map<?,?>) response.getBody();
        assertEquals("PROCESSING", body.get("status"));
    }

    @Test
    void updateBookThrowsWhenIfMatchMissing() {
        WebRequest webRequest = mock(WebRequest.class);
        UpdateBookRequest updateBookRequest = new UpdateBookRequest();

        when(featureFlagService.isFeatureEnabled(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.updateBook("isbn-12", webRequest, updateBookRequest));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateBookReturnsOkWithEtag() {
        WebRequest webRequest = mock(WebRequest.class);
        UpdateBookRequest updateBookRequest = new UpdateBookRequest();
        when(webRequest.getHeader(ConcurrencyService.IF_MATCH)).thenReturn("\"3\"");
        when(concurrencyService.getVersionFromIfMatchHeader("\"3\"")).thenReturn(3L);

        Book book = new Book("9781617294945", "Title", "Desc", new Genre("Sci-Fi"), List.of(new Author("Bob", "Bio", null)), null);
        ReflectionTestUtils.setField(book, "version", 3L);
        BookView bookView = new BookView();
        bookView.setIsbn("9781617294945");

        when(featureFlagService.isFeatureEnabled(anyString())).thenReturn(true);
        when(bookService.update(any(UpdateBookRequest.class), eq(3L))).thenReturn(book);
        when(bookViewMapper.toBookView(book)).thenReturn(bookView);

        ResponseEntity<BookView> response = controller.updateBook("9781617294945", webRequest, updateBookRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("\"3\"", response.getHeaders().getETag());
    }

    @Test
    void deleteBookPhotoReturnsNotFoundWhenNoPhoto() {
        Book book = new Book("9780321356680", "Title", "Desc", new Genre("Drama"), List.of(new Author("Alice", "Bio", null)), null);
        when(bookService.findByIsbn("9780321356680")).thenReturn(book);

        ResponseEntity<Void> response = controller.deleteBookPhoto("9780321356680");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    void deleteBookPhotoRemovesAndDeletesFile() {
        Book book = new Book("9780201633610", "Title", "Desc", new Genre("Drama"), List.of(new Author("Alice", "Bio", null)), null);
        String photoPath = "photos" + java.io.File.separator + "cover.png";
        ReflectionTestUtils.invokeMethod(book, "setPhotoInternal", photoPath);
        ReflectionTestUtils.setField(book, "version", 4L);

        when(bookService.findByIsbn("9780201633610")).thenReturn(book);

        ResponseEntity<Void> response = controller.deleteBookPhoto("9780201633610");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileStorageService).deleteFile(photoPath);
        verify(bookService).removeBookPhoto("9780201633610", 4L);
    }

    @Test
    void searchBooksReturnsMappedList() {
        SearchRequest<SearchBooksQuery> request = new SearchRequest<>(new Page(1, 5), new SearchBooksQuery("t", "g", "a"));
        BookView bookView = new BookView();
        bookView.setIsbn("9780134685991");

        when(bookService.searchBooks(any(), any())).thenReturn(List.of());
        when(bookViewMapper.toBookView(anyList())).thenReturn(List.of(bookView));

        ListResponse<BookView> response = controller.searchBooks(request);

        assertEquals(1, response.getItems().size());
        assertEquals("9780134685991", response.getItems().get(0).getIsbn());
    }

    @Test
    void checkBookExistsReturnsFalseOnNotFound() {
        when(bookService.findByIsbn("missing")).thenThrow(new NotFoundException(Book.class, "missing"));

        ResponseEntity<Map<String, Boolean>> response = controller.checkBookExists("missing");

        assertEquals(Boolean.FALSE, response.getBody().get("exists"));
    }

    private void bindRequestToContext(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
