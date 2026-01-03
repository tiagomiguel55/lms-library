package pt.psoft.g1.psoftg1.bookmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookService bookService;

    @Mock
    private ConcurrencyService concurrencyService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private BookViewMapper bookViewMapper;

    @InjectMocks
    private BookController bookController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookController).build();
    }

    private Book buildBook(String isbn, String title) {
        Author author = new Author("Author", "Bio", null);
        Genre genre = new Genre("Genre");
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        return new Book(isbn, title, "Description", genre, authors, null);
    }

    @Test
    void findBooksMergesDistinctResultsAndSorts() throws Exception {
        Book alpha = buildBook("9780306406157", "Alpha");
        Book beta = buildBook("9781234567897", "Beta");

        when(bookService.findByTitle("query")).thenReturn(List.of(beta));
        when(bookService.findByGenre("Genre")).thenReturn(List.of(alpha));
        when(bookService.findByAuthorName("Author")).thenReturn(List.of(beta));

        when(bookViewMapper.toBookView(any(List.class))).thenAnswer(invocation -> {
            List<Book> books = invocation.getArgument(0);
            List<BookView> views = new ArrayList<>();
            for (Book book : books) {
                BookView view = new BookView();
                view.setTitle(book.getTitle().toString());
                view.setGenre(book.getGenre().toString());
                view.setIsbn(book.getIsbn());
                view.setAuthors(List.of("Author"));
                views.add(view);
            }
            return views;
        });

        mockMvc.perform(get("/api/books")
                        .param("title", "query")
                        .param("genre", "Genre")
                        .param("authorName", "Author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].title").value("Alpha"))
                .andExpect(jsonPath("$.items[1].title").value("Beta"));
    }

    @Test
    void findBooksWithoutMatchesReturnsNotFound() throws Exception {
        when(bookService.findByTitle(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/books").param("title", "missing"))
                .andExpect(status().isNotFound());
    }
}
