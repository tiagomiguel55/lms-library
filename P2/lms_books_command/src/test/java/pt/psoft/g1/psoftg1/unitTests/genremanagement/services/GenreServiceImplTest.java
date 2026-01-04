package pt.psoft.g1.psoftg1.unitTests.genremanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.publishers.GenreEventsPublisher;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private GenreEventsPublisher genreEventsPublisher;

    @InjectMocks
    private GenreServiceImpl genreService;

    @Test
    void savePublishesGenreCreatedEvent() {
        Genre genre = new Genre("Horror");
        when(genreRepository.save(genre)).thenReturn(genre);

        Genre saved = genreService.save(genre);

        assertSame(genre, saved);
        verify(genreEventsPublisher).sendGenreCreated(genre);
    }

    @Test
    void markGenreAsFinalizedThrowsWhenMissing() {
        when(genreRepository.findByString("Fantasy")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> genreService.markGenreAsFinalized("Fantasy"));
    }

    @Test
    void markGenreAsFinalizedSkipsSaveWhenAlreadyFinalized() {
        Genre genre = new Genre("Thriller");
        genre.setFinalized(true);
        when(genreRepository.findByString("Thriller")).thenReturn(Optional.of(genre));

        genreService.markGenreAsFinalized("Thriller");

        verify(genreRepository, never()).save(any());
    }

    @Test
    void markGenreAsFinalizedUpdatesAndPersists() {
        Genre genre = new Genre("Sci-Fi");
        when(genreRepository.findByString("Sci-Fi")).thenReturn(Optional.of(genre));
        when(genreRepository.save(genre)).thenReturn(genre);

        genreService.markGenreAsFinalized("Sci-Fi");

        assertTrue(genre.isFinalized());
        verify(genreRepository).save(genre);
    }
}
