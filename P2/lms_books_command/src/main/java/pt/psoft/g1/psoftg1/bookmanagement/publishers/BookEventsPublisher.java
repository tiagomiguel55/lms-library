package pt.psoft.g1.psoftg1.bookmanagement.publishers;

import pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.api.LendingValidationResponse;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;

public interface BookEventsPublisher {

    BookViewAMQP sendBookCreated(Book book);

    BookViewAMQP sendBookUpdated(Book book, Long currentVersion);

    BookViewAMQP sendBookDeleted(Book book, Long currentVersion);

    BookRequestedEvent sendBookRequested(Book book);

    BookRequestedEvent sendBookRequestedEvent(String bookId, String authorName, String genreName);

    BookFinalizedEvent sendBookFinalizedEvent(Long authorId, String authorName, String bookId, String genreName);

    LendingValidationResponse sendBookValidationResponse(LendingValidationResponse response);
}
