package pt.psoft.g1.psoftg1.authormanagement.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringDataAuthorRepositoryTest {

    @Mock
    private AuthorRepository authorRepository;

    @Test
    void findByAuthorNumberReturnsAuthor() {
        Author author = new Author(10L, "Name", "Bio", null);
        when(authorRepository.findByAuthorNumber(10L)).thenReturn(Optional.of(author));

        Optional<Author> found = authorRepository.findByAuthorNumber(10L);

        assertTrue(found.isPresent());
        assertEquals(10L, found.get().getAuthorNumber());
        assertEquals("Name", found.get().getName());
    }

    @Test
    void findCoAuthorsByAuthorNumberReturnsCoAuthors() {
        Author author = new Author(10L, "Name", "Bio", null);
        when(authorRepository.findCoAuthorsByAuthorNumber(10L)).thenReturn(List.of(author));

        List<Author> coAuthors = authorRepository.findCoAuthorsByAuthorNumber(10L);

        assertEquals(1, coAuthors.size());
        assertEquals(10L, coAuthors.get(0).getAuthorNumber());
    }
}
