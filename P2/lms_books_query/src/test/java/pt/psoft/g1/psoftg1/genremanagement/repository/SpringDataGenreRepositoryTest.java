package pt.psoft.g1.psoftg1.genremanagement.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringDataGenreRepositoryTest {

    @Mock
    private GenreRepository genreRepository;

    @Test
    void findByStringReturnsGenre() {
        Genre genre = new Genre("Adventure");
        when(genreRepository.findByString("Adventure")).thenReturn(Optional.of(genre));

        Optional<Genre> found = genreRepository.findByString("Adventure");

        assertTrue(found.isPresent());
        assertEquals("Adventure", found.get().getGenre());
    }
}
