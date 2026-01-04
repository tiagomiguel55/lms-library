package pt.psoft.g1.psoftg1.CDCTests.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderPendingCreated;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderUserRequestedEvent;
import pt.psoft.g1.psoftg1.readermanagement.api.SagaCreationResponse;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderSagaViewAMQP;
import pt.psoft.g1.psoftg1.usermanagement.api.UserPendingCreated;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;

public class ReaderMessageBuilder {
    private final ObjectMapper mapper = new ObjectMapper();
    private ReaderViewAMQP readerViewAMQP;
    private ReaderPendingCreated readerPendingCreated;
    private ReaderUserRequestedEvent readerUserRequestedEvent;
    private SagaCreationResponse sagaCreationResponse;
    private ReaderSagaViewAMQP readerSagaViewAMQP;
    private UserPendingCreated userPendingCreated;
    private UserViewAMQP userViewAMQP;

    public ReaderMessageBuilder withReader(ReaderViewAMQP readerViewAMQP) {
        this.readerViewAMQP = readerViewAMQP;
        return this;
    }

    public ReaderMessageBuilder withReaderPendingCreated(ReaderPendingCreated readerPendingCreated) {
        this.readerPendingCreated = readerPendingCreated;
        return this;
    }

    public ReaderMessageBuilder withReaderUserRequested(ReaderUserRequestedEvent readerUserRequestedEvent) {
        this.readerUserRequestedEvent = readerUserRequestedEvent;
        return this;
    }

    public ReaderMessageBuilder withSagaCreationResponse(SagaCreationResponse sagaCreationResponse) {
        this.sagaCreationResponse = sagaCreationResponse;
        return this;
    }

    public ReaderMessageBuilder withReaderSagaView(ReaderSagaViewAMQP readerSagaViewAMQP) {
        this.readerSagaViewAMQP = readerSagaViewAMQP;
        return this;
    }

    public ReaderMessageBuilder withUserPendingCreated(UserPendingCreated userPendingCreated) {
        this.userPendingCreated = userPendingCreated;
        return this;
    }

    public ReaderMessageBuilder withUser(UserViewAMQP userViewAMQP) {
        this.userViewAMQP = userViewAMQP;
        return this;
    }

    public Message<String> build() throws JsonProcessingException {
        String payload;

        if (readerViewAMQP != null) {
            payload = this.mapper.writeValueAsString(this.readerViewAMQP);
        } else if (readerPendingCreated != null) {
            payload = this.mapper.writeValueAsString(this.readerPendingCreated);
        } else if (readerUserRequestedEvent != null) {
            payload = this.mapper.writeValueAsString(this.readerUserRequestedEvent);
        } else if (sagaCreationResponse != null) {
            payload = this.mapper.writeValueAsString(this.sagaCreationResponse);
        } else if (readerSagaViewAMQP != null) {
            payload = this.mapper.writeValueAsString(this.readerSagaViewAMQP);
        } else if (userPendingCreated != null) {
            payload = this.mapper.writeValueAsString(this.userPendingCreated);
        } else if (userViewAMQP != null) {
            payload = this.mapper.writeValueAsString(this.userViewAMQP);
        } else {
            throw new IllegalStateException("No payload object set");
        }

        return MessageBuilder.withPayload(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }
}
