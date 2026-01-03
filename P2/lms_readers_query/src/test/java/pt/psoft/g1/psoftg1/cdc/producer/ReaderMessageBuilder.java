package pt.psoft.g1.psoftg1.cdc.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.SagaCreationResponse;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderSagaViewAMQP;

public class ReaderMessageBuilder {
    private ObjectMapper mapper = new ObjectMapper();
    private ReaderViewAMQP readerViewAMQP;
    private SagaCreationResponse sagaCreationResponse;
    private ReaderSagaViewAMQP readerSagaViewAMQP;

    public ReaderMessageBuilder withReader(ReaderViewAMQP readerViewAMQP) {
        this.readerViewAMQP = readerViewAMQP;
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

    public Message<String> build() throws JsonProcessingException {
        String payload;

        if (readerViewAMQP != null) {
            payload = this.mapper.writeValueAsString(this.readerViewAMQP);
        } else if (sagaCreationResponse != null) {
            payload = this.mapper.writeValueAsString(this.sagaCreationResponse);
        } else if (readerSagaViewAMQP != null) {
            payload = this.mapper.writeValueAsString(this.readerSagaViewAMQP);
        } else {
            throw new IllegalStateException("No payload object set");
        }

        return MessageBuilder.withPayload(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }
}
