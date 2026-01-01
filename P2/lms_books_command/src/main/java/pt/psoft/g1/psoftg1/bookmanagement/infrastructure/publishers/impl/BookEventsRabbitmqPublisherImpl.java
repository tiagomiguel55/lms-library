package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.publishers.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQPMapper;
import pt.psoft.g1.psoftg1.bookmanagement.api.LendingValidationResponse;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
import pt.psoft.g1.psoftg1.shared.model.BookEvents;
import pt.psoft.g1.psoftg1.shared.services.OutboxService;

@Service
@RequiredArgsConstructor
public class BookEventsRabbitmqPublisherImpl implements BookEventsPublisher {

    private final BookViewAMQPMapper bookViewAMQPMapper;
    private final OutboxService outboxService;

    @Override
    public BookViewAMQP sendBookCreated(Book book) {
        return sendBookEvent(book, 1L, BookEvents.BOOK_CREATED);
    }

    @Override
    public BookViewAMQP sendBookUpdated(Book book, Long currentVersion) {
        return sendBookEvent(book, currentVersion, BookEvents.BOOK_UPDATED);
    }

    @Override
    public BookViewAMQP sendBookDeleted(Book book, Long currentVersion) {
        return sendBookEvent(book, currentVersion, BookEvents.BOOK_DELETED);
    }

    @Override
    public BookRequestedEvent sendBookRequested(Book book) {
        System.out.println("Save Book Requested event to Outbox: " + book.getTitle());

        try {
            BookRequestedEvent event = new BookRequestedEvent();
            event.setBookId(book.getIsbn());

            String authorName = book.getAuthors().isEmpty() ? "" : book.getAuthors().get(0).getName();
            event.setAuthorName(authorName);
            event.setGenreName(book.getGenre().toString());

            outboxService.saveEvent("Book", book.getIsbn(), BookEvents.BOOK_REQUESTED, event);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving book requested event to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }

    @Override
    public BookRequestedEvent sendBookRequestedEvent(String bookId, String authorName, String genreName) {
        System.out.println("Save Book Requested event to Outbox: " + bookId + " - " + authorName + " - " + genreName);

        try {
            BookRequestedEvent event = new BookRequestedEvent(bookId, null, authorName, genreName);

            outboxService.saveEvent("Book", bookId, BookEvents.BOOK_REQUESTED, event);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving book requested event to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }

    @Override
    public BookFinalizedEvent sendBookFinalizedEvent(Long authorId, String authorName, String bookId, String genreName, String title, String description) {
        System.out.println("Save Book Finalized event to Outbox: Author ID " + authorId + " for book: " + bookId + " - Genre: " + genreName + " - Title: " + title);

        try {
            BookFinalizedEvent event = new BookFinalizedEvent(authorId, authorName, bookId, genreName, title, description);

            outboxService.saveEvent("Book", bookId, BookEvents.BOOK_FINALIZED, event);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving book finalized event to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }

    @Override
    public LendingValidationResponse sendBookValidationResponse(LendingValidationResponse response) {
        System.out.println(" [BOOKS] 📤 Saving book validation response to Outbox for lending: " + response.getLendingNumber());

        try {
            // Save to outbox with routing key for lending validation response
            outboxService.saveEvent(
                "Book",
                response.getIsbn(),
                "lending.validation.response",
                response
            );

            System.out.println(" [BOOKS] ✅ Validation response saved to Outbox | RequestId: " + response.getRequestId() +
                             " | Book exists: " + response.isBookExists());

            return response;
        }
        catch( Exception ex ) {
            System.out.println(" [BOOKS] ❌ Exception saving validation response to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }

    private BookViewAMQP sendBookEvent(Book book, Long currentVersion, String bookEventType) {
        System.out.println("Save Book event to Outbox: " + book.getTitle());

        try {
            BookViewAMQP bookViewAMQP = bookViewAMQPMapper.toBookViewAMQP(book);
            bookViewAMQP.setVersion(currentVersion);

            outboxService.saveEvent("Book", book.getIsbn(), bookEventType, bookViewAMQP);

            return bookViewAMQP;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception saving book event to outbox: '" + ex.getMessage() + "'");
            return null;
        }
    }
}