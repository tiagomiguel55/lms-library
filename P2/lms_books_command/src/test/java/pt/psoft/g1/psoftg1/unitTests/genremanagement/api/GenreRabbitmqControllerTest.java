package pt.psoft.g1.psoftg1.unitTests.genremanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreRabbitmqController;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.publishers.GenreEventsPublisher;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreRabbitmqControllerTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private GenreEventsPublisher genreEventsPublisher;

    @Mock
    private GenreService genreService;

    @InjectMocks
    private GenreRabbitmqController controller;

    @Test
    void receiveBookRequestedUsesExistingGenre() throws Exception {
        BookRequestedEvent event = new BookRequestedEvent("isbn-1", "Title", "Author", "Drama");
        Message message = MessageBuilder.withBody(new ObjectMapper().writeValueAsString(event).getBytes(StandardCharsets.UTF_8)).build();

        when(genreRepository.findByString("Drama")).thenReturn(Optional.of(new Genre("Drama")));

        controller.receiveBookRequested(message);

        verify(genreRepository, never()).save(any());
        verify(genreEventsPublisher).sendGenrePendingCreated("Drama", "isbn-1");
    }

    @Test
    void receiveBookRequestedCreatesGenreWhenMissing() throws Exception {
        BookRequestedEvent event = new BookRequestedEvent("isbn-2", "Title", "Author", "NewGenre");
        Message message = MessageBuilder.withBody(new ObjectMapper().writeValueAsString(event).getBytes(StandardCharsets.UTF_8)).build();

        when(genreRepository.findByString("NewGenre")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.receiveBookRequested(message);

        verify(genreRepository).save(any(Genre.class));
        verify(genreEventsPublisher).sendGenrePendingCreated("NewGenre", "isbn-2");
    }

    @Test
    void receiveBookFinalizedFinalizesAndPublishes() throws Exception {
        BookFinalizedEvent event = new BookFinalizedEvent(1L, "Author", "isbn-3", "Drama", "Title", "Desc");
        Message message = MessageBuilder.withBody(new ObjectMapper().writeValueAsString(event).getBytes(StandardCharsets.UTF_8)).build();

        Genre genre = new Genre("Drama");
        when(genreRepository.findByString("Drama")).thenReturn(Optional.of(genre));

        controller.receiveBookFinalized(message);

        verify(genreService).markGenreAsFinalized("Drama");
        verify(genreEventsPublisher).sendGenreCreated(genre, "isbn-3");
    }

    @Test
    void receiveBookFinalizedSkipsWhenAlreadyFinalized() throws Exception {
        BookFinalizedEvent event = new BookFinalizedEvent(1L, "Author", "isbn-4", "Drama", "Title", "Desc");
        Message message = MessageBuilder.withBody(new ObjectMapper().writeValueAsString(event).getBytes(StandardCharsets.UTF_8)).build();

        Genre genre = new Genre("Drama");
        genre.setFinalized(true);
        when(genreRepository.findByString("Drama")).thenReturn(Optional.of(genre));

        controller.receiveBookFinalized(message);

        verify(genreService, never()).markGenreAsFinalized(anyString());
        verify(genreEventsPublisher, never()).sendGenreCreated(any(), anyString());
    }
}
