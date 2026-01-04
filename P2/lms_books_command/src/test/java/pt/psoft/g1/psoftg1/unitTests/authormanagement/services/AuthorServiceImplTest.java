package pt.psoft.g1.psoftg1.unitTests.authormanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorMapper;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorServiceImpl;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
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

    @Test
    void createClearsMismatchedPhotoData() {
        CreateAuthorRequest request = new CreateAuthorRequest("Alice", "Bio", null, "/tmp/photo.png");
        Author mappedAuthor = new Author("Alice", "Bio", null);

        when(authorMapper.create(request)).thenReturn(mappedAuthor);
        when(authorRepository.save(mappedAuthor)).thenReturn(mappedAuthor);

        Author result = authorService.create(request);

        assertNull(request.getPhoto());
        assertNull(request.getPhotoURI());
        assertSame(mappedAuthor, result);
        verify(authorRepository).save(mappedAuthor);
    }

    @Test
    void partialUpdateThrowsWhenAuthorNotFound() {
        UpdateAuthorRequest updateRequest = new UpdateAuthorRequest("New bio", "New name", null, null);
        when(authorRepository.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authorService.partialUpdate(99L, updateRequest, 0L));
    }

    @Test
    void markAuthorAsFinalizedDoesNotSaveWhenAlreadyFinalized() {
        Author author = new Author("Bob", "Bio", null);
        author.setFinalized(true);
        when(authorRepository.findByAuthorNumber(5L)).thenReturn(Optional.of(author));

        authorService.markAuthorAsFinalized(5L);

        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    void markAuthorAsFinalizedPersistsWhenOpen() {
        Author author = new Author("Carol", "Bio", null);
        when(authorRepository.findByAuthorNumber(7L)).thenReturn(Optional.of(author));
        when(authorRepository.save(author)).thenReturn(author);

        authorService.markAuthorAsFinalized(7L);

        assertTrue(author.isFinalized());
        verify(authorRepository).save(author);
    }

    @Test
    void removeAuthorPhotoDeletesFileAndClearsPhoto() {
        Author author = new Author("Dave", "Bio", "photos/dave.png");
        String existingPhoto = author.getPhoto().getPhotoFile();
        when(authorRepository.findByAuthorNumber(11L)).thenReturn(Optional.of(author));
        when(authorRepository.save(author)).thenReturn(author);

        Optional<Author> updated = authorService.removeAuthorPhoto(11L, 0L);

        assertTrue(updated.isPresent());
        assertNull(updated.get().getPhoto());
        verify(photoRepository).deleteByPhotoFile(existingPhoto);
        verify(authorRepository).save(author);
    }
}
