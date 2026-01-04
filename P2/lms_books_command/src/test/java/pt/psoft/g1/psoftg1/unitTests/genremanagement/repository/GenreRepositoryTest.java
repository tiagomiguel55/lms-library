package pt.psoft.g1.psoftg1.unitTests.genremanagement.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenreRepositoryTest {

    private GenreRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGenreRepository();
        repository.save(new Genre("Drama"));
        repository.save(new Genre("Comedy"));
    }

    @Test
    void findByStringReturnsMatch() {
        assertTrue(repository.findByString("Drama").isPresent());
        assertTrue(repository.findByString("drama").isPresent());
    }

    @Test
    void findAllReturnsAllGenres() {
        Iterable<Genre> all = repository.findAll();
        int count = 0;
        for (Genre ignored : all) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void deleteRemovesGenre() {
        Genre genre = repository.findByString("Drama").orElseThrow();
        repository.delete(genre);
        assertTrue(repository.findByString("Drama").isEmpty());
    }

    private static final class InMemoryGenreRepository implements GenreRepository {
        private final Map<String, Genre> store = new HashMap<>();

        @Override
        public Iterable<Genre> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<Genre> findByString(String genreName) {
            return store.values().stream()
                    .filter(g -> g.toString().equalsIgnoreCase(genreName))
                    .findFirst();
        }

        @Override
        public Genre save(Genre genre) {
            store.put(genre.toString().toLowerCase(Locale.ROOT), genre);
            return genre;
        }

        @Override
        public void delete(Genre genre) {
            store.remove(genre.toString().toLowerCase(Locale.ROOT));
        }
    }
}
