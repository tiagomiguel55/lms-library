package pt.psoft.g1.psoftg1.CDCTests.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;
import pt.psoft.g1.psoftg1.usermanagement.api.UserPendingCreated;
import pt.psoft.g1.psoftg1.usermanagement.api.ReaderUserRequestedEvent;

public class UserMessageBuilder {
    private ObjectMapper mapper = new ObjectMapper();
    private UserViewAMQP userViewAMQP;
    private UserPendingCreated userPendingCreated;
    private ReaderUserRequestedEvent readerUserRequestedEvent;
    private String deletedUsername;  // For user deleted events

    public UserMessageBuilder withUser(UserViewAMQP userViewAMQP) {
        this.userViewAMQP = userViewAMQP;
        return this;
    }

    public UserMessageBuilder withUserPendingCreated(UserPendingCreated userPendingCreated) {
        this.userPendingCreated = userPendingCreated;
        return this;
    }

    public UserMessageBuilder withReaderUserRequested(ReaderUserRequestedEvent readerUserRequestedEvent) {
        this.readerUserRequestedEvent = readerUserRequestedEvent;
        return this;
    }

    public UserMessageBuilder withDeletedUsername(String deletedUsername) {
        this.deletedUsername = deletedUsername;
        return this;
    }

    public Message<String> build() throws JsonProcessingException {
        String payload;

        if (userViewAMQP != null) {
            payload = this.mapper.writeValueAsString(this.userViewAMQP);
        } else if (userPendingCreated != null) {
            payload = this.mapper.writeValueAsString(this.userPendingCreated);
        } else if (readerUserRequestedEvent != null) {
            payload = this.mapper.writeValueAsString(this.readerUserRequestedEvent);
        } else if (deletedUsername != null) {
            payload = this.deletedUsername;  // USER_DELETED sends just the username string
        } else {
            throw new IllegalStateException("No payload object set");
        }

        return MessageBuilder.withPayload(payload)
                .setHeader("Content-Type",
                    deletedUsername != null ? "text/plain; charset=utf-8" : "application/json; charset=utf-8")
                .build();
    }
}
