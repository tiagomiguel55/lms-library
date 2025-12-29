package pt.psoft.g1.psoftg1.producerTests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;

public class ReaderMessageBuilder {
    private ObjectMapper mapper = new ObjectMapper();
    private ReaderViewAMQP readerViewAMQP;

    public ReaderMessageBuilder withReader(ReaderViewAMQP readerViewAMQP) {
        this.readerViewAMQP = readerViewAMQP;
        return this;
    }

    public Message<String> build() throws JsonProcessingException {
        return MessageBuilder.withPayload(this.mapper.writeValueAsString(this.readerViewAMQP))
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }

}