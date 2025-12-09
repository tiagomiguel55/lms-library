package pt.psoft.g1.psoftg1.authormanagement.publishers;

import pt.psoft.g1.psoftg1.authormanagement.api.AuthorPendingCreated;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQP;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;

public interface AuthorEventsPublisher {

    AuthorViewAMQP sendAuthorCreated(Author author, String bookId);

    AuthorViewAMQP sendAuthorUpdated(Author author, Long currentVersion);

    AuthorViewAMQP sendAuthorDeleted(Author author, Long currentVersion);

    AuthorPendingCreated sendAuthorPendingCreated(Long authorId, String bookId, String authorName, String genreName);
}
