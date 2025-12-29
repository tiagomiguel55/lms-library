package pt.psoft.g1.psoftg1.authormanagement.infrastructure.publishers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorCreationFailed;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorPendingCreated;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQP;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQPMapper;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.publishers.AuthorEventsPublisher;
import pt.psoft.g1.psoftg1.shared.model.AuthorEvents;
import pt.psoft.g1.psoftg1.shared.services.OutboxService;

@Service
@RequiredArgsConstructor
public class AuthorEventsRabbitmqPublisherImpl implements AuthorEventsPublisher {

    private final AuthorViewAMQPMapper authorViewAMQPMapper;
    private final OutboxService outboxService;

    @Override
    public AuthorViewAMQP sendAuthorCreated(Author author, String bookId) {
        return sendAuthorEvent(author, 1L, AuthorEvents.AUTHOR_CREATED, bookId);
    }

    @Override
    public AuthorViewAMQP sendAuthorUpdated(Author author, Long currentVersion) {
        return sendAuthorEvent(author, currentVersion, AuthorEvents.AUTHOR_UPDATED, null);
    }

    @Override
    public AuthorViewAMQP sendAuthorDeleted(Author author, Long currentVersion) {
        return sendAuthorEvent(author, currentVersion, AuthorEvents.AUTHOR_DELETED, null);
    }

    @Override
    public AuthorPendingCreated sendAuthorPendingCreated(Long authorId, String bookId, String authorName, String genreName) {
        System.out.println("Save Author Pending Created event to Outbox: " + authorName + " for book: " + bookId);

        try {
            AuthorPendingCreated event = new AuthorPendingCreated(authorId, bookId, authorName, genreName);

            outboxService.saveEvent("Author", authorId.toString(), AuthorEvents.AUTHOR_PENDING_CREATED, event);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving author pending created event to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }

    @Override
    public AuthorCreationFailed sendAuthorCreationFailed(String bookId, String authorName, String genreName, String errorMessage) {
        System.out.println("Save Author Creation Failed event to Outbox: " + authorName + " for book: " + bookId);

        try {
            AuthorCreationFailed event = new AuthorCreationFailed(bookId, authorName, genreName, errorMessage);

            outboxService.saveEvent("Author", authorName, AuthorEvents.AUTHOR_CREATION_FAILED, event);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving author creation failed event to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }

    private AuthorViewAMQP sendAuthorEvent(Author author, Long currentVersion, String authorEventType, String bookId) {
        System.out.println("Save Author event to Outbox: " + author.getName());

        try {
            AuthorViewAMQP authorViewAMQP = authorViewAMQPMapper.toAuthorViewAMQP(author);
            authorViewAMQP.setVersion(currentVersion);
            authorViewAMQP.setBookId(bookId); // ✅ SET THE BOOK ID!

            outboxService.saveEvent("Author", String.valueOf(author.getAuthorNumber()), authorEventType, authorViewAMQP);

            return authorViewAMQP;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving author event to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }
}
