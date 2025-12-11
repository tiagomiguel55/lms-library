package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.publishers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQPMapper;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;

import pt.psoft.g1.psoftg1.shared.model.BookEvents;

@Service
@RequiredArgsConstructor
public class BookEventsRabbitmqPublisherImpl implements BookEventsPublisher {

    @Autowired
    private RabbitTemplate template;
    @Autowired
    private DirectExchange direct;
    @Autowired
    private final BookViewAMQPMapper bookViewAMQPMapper;

    private int count = 0;

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
        System.out.println("Send Book Requested event to AMQP Broker: " + book.getTitle());

        try {
            // Create simplified event with only bookId, authorName, and genreName
            BookRequestedEvent event = new BookRequestedEvent();
            event.setBookId(book.getIsbn());

            // Get the first author's name (assuming at least one author exists)
            String authorName = book.getAuthors().isEmpty() ? "" : book.getAuthors().get(0).getName();
            event.setAuthorName(authorName);

            event.setGenreName(book.getGenre().toString());

            ObjectMapper objectMapper = new ObjectMapper();
            String eventInString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(direct.getName(), BookEvents.BOOK_REQUESTED, eventInString);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending book requested event: '" + ex.getMessage() + "'");
            return null;
        }
    }

    @Override
    public BookRequestedEvent sendBookRequestedEvent(String bookId, String authorName, String genreName) {
        System.out.println("Send Book Requested event to AMQP Broker: " + bookId + " - " + authorName + " - " + genreName);

        try {
            BookRequestedEvent event = new BookRequestedEvent(bookId, authorName, genreName);

            ObjectMapper objectMapper = new ObjectMapper();
            String eventInString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(direct.getName(), BookEvents.BOOK_REQUESTED, eventInString);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending book requested event: '" + ex.getMessage() + "'");
            return null;
        }
    }

    @Override
    public BookFinalizedEvent sendBookFinalizedEvent(Long authorId, String authorName, String bookId, String genreName) {
        System.out.println("Send Book Finalized event to AMQP Broker: Author ID " + authorId + " for book: " + bookId + " - Genre: " + genreName);

        try {
            BookFinalizedEvent event = new BookFinalizedEvent(authorId, authorName, bookId, genreName);

            ObjectMapper objectMapper = new ObjectMapper();
            String eventInString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(direct.getName(), BookEvents.BOOK_FINALIZED, eventInString);

            return event;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending book finalized event: '" + ex.getMessage() + "'");
            return null;
        }
    }

    private BookViewAMQP sendBookEvent(Book book, Long currentVersion, String bookEventType) {

        System.out.println("Send Book event to AMQP Broker: " + book.getTitle());

        try {
            BookViewAMQP bookViewAMQP = bookViewAMQPMapper.toBookViewAMQP(book);
            bookViewAMQP.setVersion(currentVersion);

            ObjectMapper objectMapper = new ObjectMapper();
            String bookViewAMQPinString = objectMapper.writeValueAsString(bookViewAMQP);

            this.template.convertAndSend(direct.getName(), bookEventType, bookViewAMQPinString);

            return bookViewAMQP;
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending book event: '" + ex.getMessage() + "'");

            return null;
        }
    }
}