package pt.psoft.g1.psoftg1.genremanagement.infrastructure.publishers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.genremanagement.api.*;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.publishers.GenreEventsPublisher;
import pt.psoft.g1.psoftg1.shared.model.GenreEvents;

@Service
@RequiredArgsConstructor
public class GenreEventsRabbitmqPublisherImpl implements GenreEventsPublisher {

    @Autowired
    private RabbitTemplate template;
    @Autowired
    private DirectExchange directGenres;
    @Autowired
    private final GenreViewAMQPMapper genreViewAMQPMapper;

    @Override
    public GenreViewAMQP sendGenreCreated(Genre genre) {
        return sendGenreEvent(genre, 1L, GenreEvents.GENRE_CREATED, null);
    }

    @Override
    public GenreViewAMQP sendGenreUpdated(Genre genre, Long currentVersion) {
        return sendGenreEvent(genre, currentVersion, GenreEvents.GENRE_UPDATED, null);
    }

    @Override
    public GenreViewAMQP sendGenreDeleted(Genre genre, Long currentVersion) {
        return sendGenreEvent(genre, currentVersion, GenreEvents.GENRE_DELETED, null);
    }

    @Override
    public void sendGenrePendingCreated(String genreName, String bookId) {
        System.out.println("Send Genre Pending Created event to AMQP Broker: " + genreName + " for book: " + bookId);

        try {
            GenrePendingCreated event = new GenrePendingCreated(genreName, bookId);

            ObjectMapper objectMapper = new ObjectMapper();
            String eventInString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(directGenres.getName(), GenreEvents.GENRE_PENDING_CREATED, eventInString);
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending genre pending created event: '" + ex.getMessage() + "'");
        }
    }

    @Override
    public void sendGenreCreationFailed(String bookId, String genreName, String errorMessage) {
        System.out.println("Send Genre Creation Failed event to AMQP Broker: " + genreName + " for book: " + bookId);

        try {
            GenreCreationFailed event = new GenreCreationFailed(bookId, genreName, errorMessage);

            ObjectMapper objectMapper = new ObjectMapper();
            String eventInString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(directGenres.getName(), GenreEvents.GENRE_CREATION_FAILED, eventInString);
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending genre creation failed event: '" + ex.getMessage() + "'");
        }
    }

    @Override
    public void sendGenreCreated(Genre genre, String bookId) {
        sendGenreEvent(genre, 1L, GenreEvents.GENRE_CREATED, bookId);
    }

    private GenreViewAMQP sendGenreEvent(Genre genre, Long currentVersion, String genreEventType, String bookId) {

        System.out.println("Send Genre event to AMQP Broker: " + genre.getGenre());

        try {
            GenreViewAMQP genreViewAMQP = genreViewAMQPMapper.toGenreViewAMQP(genre);
            genreViewAMQP.setVersion(currentVersion);
            if (bookId != null) {
                genreViewAMQP.setBookId(bookId);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String genreViewAMQPinString = objectMapper.writeValueAsString(genreViewAMQP);

            this.template.convertAndSend(directGenres.getName(), genreEventType, genreViewAMQPinString);

            return genreViewAMQP;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending genre event: '" + ex.getMessage() + "'");

            return null;
        }
    }
}
