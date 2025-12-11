package pt.psoft.g1.psoftg1.authormanagement.infrastructure.publishers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorCreationFailed;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorPendingCreated;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQP;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQPMapper;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.publishers.AuthorEventsPublisher;
import pt.psoft.g1.psoftg1.shared.model.AuthorEvents;

@Service
@RequiredArgsConstructor
public class AuthorEventsRabbitmqPublisherImpl implements AuthorEventsPublisher {

    @Autowired
    private RabbitTemplate template;
    @Autowired
    private DirectExchange directAuthors;
    @Autowired
    private final AuthorViewAMQPMapper authorViewAMQPMapper;

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
        System.out.println("Send Author Pending Created event to AMQP Broker: " + authorName + " for book: " + bookId);

        try {
            AuthorPendingCreated event = new AuthorPendingCreated(authorId, bookId, authorName, genreName);

            ObjectMapper objectMapper = new ObjectMapper();
            String eventInString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(directAuthors.getName(), AuthorEvents.AUTHOR_PENDING_CREATED, eventInString);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending author pending created event: '" + ex.getMessage() + "'");
            return null;
        }
    }

    @Override
    public AuthorCreationFailed sendAuthorCreationFailed(String bookId, String authorName, String genreName, String errorMessage) {
        System.out.println("Send Author Creation Failed event to AMQP Broker: " + authorName + " for book: " + bookId);

        try {
            AuthorCreationFailed event = new AuthorCreationFailed(bookId, authorName, genreName, errorMessage);

            ObjectMapper objectMapper = new ObjectMapper();
            String eventInString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(directAuthors.getName(), AuthorEvents.AUTHOR_CREATION_FAILED, eventInString);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending author creation failed event: '" + ex.getMessage() + "'");
            return null;
        }
    }

    private AuthorViewAMQP sendAuthorEvent(Author author, Long currentVersion, String authorEventType, String bookId) {

        System.out.println("Send Author event to AMQP Broker: " + author.getName());

        try {
            AuthorViewAMQP authorViewAMQP = authorViewAMQPMapper.toAuthorViewAMQP(author);
            authorViewAMQP.setVersion(currentVersion);
            authorViewAMQP.setBookId(bookId); // Set the associated bookId

            ObjectMapper objectMapper = new ObjectMapper();
            String authorViewAMQPinString = objectMapper.writeValueAsString(authorViewAMQP);

            this.template.convertAndSend(directAuthors.getName(), authorEventType, authorViewAMQPinString);

            return authorViewAMQP;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending author event: '" + ex.getMessage() + "'");

            return null;
        }
    }
}
