package pt.psoft.g1.psoftg1.genremanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreRabbitmqControllerTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private BookService bookService;

    @InjectMocks
    private GenreRabbitmqController controller;

    @Test
    void receiveGenreCreatedCreatesNewGenreAndProcessesPendingBooks() throws Exception {
        GenreViewAMQP payload = new GenreViewAMQP("Horror");
        payload.setVersion(1L);
        String json = new ObjectMapper().writeValueAsString(payload);
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();

        when(genreRepository.findByString("Horror")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.receiveGenreCreatedMsg(message);

        verify(genreRepository).save(any(Genre.class));
        verify(bookService).processPendingBooksForGenre("Horror");
    }

    @Test
    void receiveGenreDeletedRemovesExistingGenre() throws Exception {
        GenreViewAMQP payload = new GenreViewAMQP("Drama");
        payload.setVersion(1L);
        String json = new ObjectMapper().writeValueAsString(payload);
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();

        Genre existing = new Genre("Drama");
        when(genreRepository.findByString("Drama")).thenReturn(Optional.of(existing));

        controller.receiveGenreDeleted(message);

        verify(genreRepository).delete(existing);
    }
}
