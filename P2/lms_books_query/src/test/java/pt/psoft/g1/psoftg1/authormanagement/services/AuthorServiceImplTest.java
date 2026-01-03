package pt.psoft.g1.psoftg1.authormanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorMapper authorMapper;

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private AuthorServiceImpl authorService;

    @BeforeEach
    void setupDefaultMocks() {
        lenient().when(authorRepository.save(any(Author.class))).thenAnswer((Answer<Author>) invocation -> invocation.getArgument(0));
    }

    @Test
    void createRemovesMismatchedPhotoInformation() {
        CreateAuthorRequest request = new CreateAuthorRequest("Alice", "Bio", null, "dangling-photo-uri.jpg");
        Author mappedAuthor = new Author("Alice", "Bio", null);

        when(authorMapper.create(any(CreateAuthorRequest.class))).thenReturn(mappedAuthor);

        Author created = authorService.create(request);

        assertNotNull(created);
        assertNull(request.getPhoto(), "Photo should be cleared when uri is mismatched");
        assertNull(request.getPhotoURI(), "Photo URI should be cleared when photo payload is missing");
        verify(authorRepository).save(mappedAuthor);
    }

    @Test
    void removeAuthorPhotoDeletesStoredFile() {
        Author author = new Author(1L, "Bob", "Bio", "portrait.png");
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(author));
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Author> updated = authorService.removeAuthorPhoto(1L, author.getVersion());

        assertTrue(updated.isPresent());
        assertNull(updated.get().getPhoto(), "Author photo should be removed");
        verify(photoRepository).deleteByPhotoFile("portrait.png");
        verify(authorRepository).save(any(Author.class));
    }
}
