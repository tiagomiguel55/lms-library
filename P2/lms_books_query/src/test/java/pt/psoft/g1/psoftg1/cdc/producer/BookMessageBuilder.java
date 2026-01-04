package pt.psoft.g1.psoftg1.cdc.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQP;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQP;

import java.util.Arrays;
import java.util.HashMap;

public class BookMessageBuilder {
    private ObjectMapper mapper = new ObjectMapper();
    private BookViewAMQP bookViewAMQP;

    public BookMessageBuilder withBook(BookViewAMQP bookViewAMQP) {
        this.bookViewAMQP = bookViewAMQP;
        return this;
    }

    public Message<String> build() throws JsonProcessingException {
        return MessageBuilder.withPayload(this.mapper.writeValueAsString(this.bookViewAMQP))
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }

    // Static factory methods for creating sample messages

    public static Message<String> createSampleBookCreatedMessage() throws JsonProcessingException {
        BookViewAMQP book = new BookViewAMQP(
            "978-0134685991",
            "Effective Java",
            "Best practices for Java programming",
            Arrays.asList(1L, 2L),
            "Programming"
        );
        book.setVersion(1L);

        return new BookMessageBuilder().withBook(book).build();
    }

    public static Message<String> createSampleBookUpdatedMessage() throws JsonProcessingException {
        BookViewAMQP book = new BookViewAMQP(
            "978-0134685991",
            "Effective Java - 3rd Edition",
            "Best practices for Java programming - Updated",
            Arrays.asList(1L, 2L),
            "Programming"
        );
        book.setVersion(2L);

        return new BookMessageBuilder().withBook(book).build();
    }

    public static Message<String> createSampleBookDeletedMessage() throws JsonProcessingException {
        BookViewAMQP book = new BookViewAMQP(
            "978-0134685991",
            "Effective Java",
            "Best practices for Java programming",
            Arrays.asList(1L, 2L),
            "Programming"
        );
        book.setVersion(1L);

        return new BookMessageBuilder().withBook(book).build();
    }

    public static Message<String> createSampleAuthorCreatedMessage() throws JsonProcessingException {
        AuthorViewAMQP author = new AuthorViewAMQP(
            1L,
            "Joshua Bloch",
            "Software engineer and author",
            "http://example.com/photo.jpg"
        );
        author.setVersion(1L);

        ObjectMapper mapper = new ObjectMapper();
        return MessageBuilder.withPayload(mapper.writeValueAsString(author))
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }

    public static Message<String> createSampleGenreCreatedMessage() throws JsonProcessingException {
        GenreViewAMQP genre = new GenreViewAMQP("Programming");
        genre.setVersion(1L);

        ObjectMapper mapper = new ObjectMapper();
        return MessageBuilder.withPayload(mapper.writeValueAsString(genre))
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }

    public static Message<String> createSampleBookFinalizedMessage() throws JsonProcessingException {
        BookViewAMQP book = new BookViewAMQP(
            "978-0134685991",
            "Effective Java - Final Edition",
            "Best practices for Java programming - Finalized",
            Arrays.asList(1L, 2L),
            "Programming"
        );
        book.setVersion(3L);

        return new BookMessageBuilder().withBook(book).build();
    }
}