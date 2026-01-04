package pt.psoft.g1.psoftg1.cdc.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRequestedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent;
import pt.psoft.g1.psoftg1.bookmanagement.api.LendingValidationResponse;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorPendingCreated;
import pt.psoft.g1.psoftg1.genremanagement.api.GenrePendingCreated;

public class BookMessageBuilder {
    private ObjectMapper mapper = new ObjectMapper();
    private BookViewAMQP bookViewAMQP;
    private BookRequestedEvent bookRequestedEvent;
    private BookFinalizedEvent bookFinalizedEvent;
    private LendingValidationResponse lendingValidationResponse;
    private AuthorPendingCreated authorPendingCreated;
    private GenrePendingCreated genrePendingCreated;

    public BookMessageBuilder withBook(BookViewAMQP bookViewAMQP) {
        this.bookViewAMQP = bookViewAMQP;
        return this;
    }

    public BookMessageBuilder withBookRequested(BookRequestedEvent bookRequestedEvent) {
        this.bookRequestedEvent = bookRequestedEvent;
        return this;
    }

    public BookMessageBuilder withBookFinalized(BookFinalizedEvent bookFinalizedEvent) {
        this.bookFinalizedEvent = bookFinalizedEvent;
        return this;
    }

    public BookMessageBuilder withLendingValidationResponse(LendingValidationResponse lendingValidationResponse) {
        this.lendingValidationResponse = lendingValidationResponse;
        return this;
    }

    public BookMessageBuilder withAuthorPendingCreated(AuthorPendingCreated authorPendingCreated) {
        this.authorPendingCreated = authorPendingCreated;
        return this;
    }

    public BookMessageBuilder withGenrePendingCreated(GenrePendingCreated genrePendingCreated) {
        this.genrePendingCreated = genrePendingCreated;
        return this;
    }

    public Message<String> build() throws JsonProcessingException {
        String payload;

        if (bookViewAMQP != null) {
            payload = this.mapper.writeValueAsString(this.bookViewAMQP);
        } else if (bookRequestedEvent != null) {
            payload = this.mapper.writeValueAsString(this.bookRequestedEvent);
        } else if (bookFinalizedEvent != null) {
            payload = this.mapper.writeValueAsString(this.bookFinalizedEvent);
        } else if (lendingValidationResponse != null) {
            payload = this.mapper.writeValueAsString(this.lendingValidationResponse);
        } else if (authorPendingCreated != null) {
            payload = this.mapper.writeValueAsString(this.authorPendingCreated);
        } else if (genrePendingCreated != null) {
            payload = this.mapper.writeValueAsString(this.genrePendingCreated);
        } else {
            throw new IllegalStateException("No payload object set");
        }

        return MessageBuilder.withPayload(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }
}
