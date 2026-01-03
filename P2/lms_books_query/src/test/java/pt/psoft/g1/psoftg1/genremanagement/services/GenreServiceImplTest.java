package pt.psoft.g1.psoftg1.genremanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreServiceImpl genreService;

    @Test
    void findByStringDelegatesToRepository() {
        Genre genre = new Genre("Mystery");
        when(genreRepository.findByString("Mystery")).thenReturn(Optional.of(genre));

        Optional<Genre> found = genreService.findByString("Mystery");

        assertTrue(found.isPresent());
        assertEquals("Mystery", found.get().getGenre());
    }

    @Test
    void findAllReturnsRepositoryData() {
        when(genreRepository.findAll()).thenReturn(List.of(new Genre("Drama")));

        Iterable<Genre> all = genreService.findAll();

        assertTrue(all.iterator().hasNext());
    }

    @Test
    void savePersistsGenre() {
        Genre genre = new Genre("Action");
        when(genreRepository.save(any(Genre.class))).thenReturn(genre);

        Genre saved = genreService.save(genre);

        assertEquals("Action", saved.getGenre());
    }
}
