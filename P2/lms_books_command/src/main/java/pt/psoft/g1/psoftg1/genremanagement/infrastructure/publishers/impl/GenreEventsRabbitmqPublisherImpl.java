package pt.psoft.g1.psoftg1.genremanagement.infrastructure.publishers.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.genremanagement.api.*;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.publishers.GenreEventsPublisher;
import pt.psoft.g1.psoftg1.shared.model.GenreEvents;
import pt.psoft.g1.psoftg1.shared.services.OutboxService;

@Service
@RequiredArgsConstructor
public class GenreEventsRabbitmqPublisherImpl implements GenreEventsPublisher {

    private final GenreViewAMQPMapper genreViewAMQPMapper;
    private final OutboxService outboxService;

    @Override
    public GenreViewAMQP sendGenreCreated(Genre genre) {
        return sendGenreEvent(genre, 1L, GenreEvents.GENRE_CREATED, null);
    }

    @Override
    public void sendGenreCreated(Genre genre, String bookId) {
        sendGenreEvent(genre, 1L, GenreEvents.GENRE_CREATED, bookId);
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
        System.out.println("Save Genre Pending Created event to Outbox: " + genreName + " for book: " + bookId);

        try {
            GenrePendingCreated event = new GenrePendingCreated(genreName, bookId);

            outboxService.saveEvent("Genre", genreName, GenreEvents.GENRE_PENDING_CREATED, event);
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving genre pending created event to outbox: '" + ex.getMessage() + "'");
        }
    }

    @Override
    public void sendGenreCreationFailed(String bookId, String genreName, String errorMessage) {
        System.out.println("Save Genre Creation Failed event to Outbox: " + genreName + " for book: " + bookId);

        try {
            GenreCreationFailed event = new GenreCreationFailed(bookId, genreName, errorMessage);

            outboxService.saveEvent("Genre", genreName, GenreEvents.GENRE_CREATION_FAILED, event);
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving genre creation failed event to outbox: '" + ex.getMessage() + "'");
        }
    }

    private GenreViewAMQP sendGenreEvent(Genre genre, Long currentVersion, String genreEventType, String bookId) {
        System.out.println("Save Genre event to Outbox: " + genre.getGenre());

        try {
            GenreViewAMQP genreViewAMQP = genreViewAMQPMapper.toGenreViewAMQP(genre);
            genreViewAMQP.setVersion(currentVersion);
            genreViewAMQP.setBookId(bookId); // ✅ SET THE BOOK ID!

            outboxService.saveEvent("Genre", genre.getGenre(), genreEventType, genreViewAMQP);

            return genreViewAMQP;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving genre event to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }
}
