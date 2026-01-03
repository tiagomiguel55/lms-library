package pt.psoft.g1.psoftg1.readermanagement.publishers;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderPendingCreated;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderUserRequestedEvent;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.api.SagaCreationResponse;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.shared.model.ReaderEvents;
import pt.psoft.g1.psoftg1.shared.model.UserEvents;

@Component
@RequiredArgsConstructor
public class ReaderEventPublisher {

    @Autowired
    private RabbitTemplate template;
    @Autowired
    private DirectExchange direct;

    private final ReaderViewAMQPMapper readerViewAMQPMapper;

    public void sendReaderCreated(ReaderDetails reader) {
        sendReaderEvent(reader, reader.getVersion(), ReaderEvents.READER_CREATED);
    }


    public void sendReaderUpdated(ReaderDetails reader, Long currentVersion) {
        sendReaderEvent(reader, currentVersion, ReaderEvents.READER_UPDATED);
    }


    public void sendReaderDeleted(ReaderDetails reader, Long currentVersion) {
        sendReaderEvent(reader, currentVersion, ReaderEvents.READER_DELETED);
    }

    public void sendReaderEvent(ReaderDetails reader, Long currentVersion, String readerEventType) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            ReaderViewAMQP readerViewAMQP = readerViewAMQPMapper.toReaderViewAMQP(reader);
            readerViewAMQP.setVersion(currentVersion.toString());

            String jsonString = objectMapper.writeValueAsString(readerViewAMQP);

            this.template.convertAndSend(direct.getName(), readerEventType, jsonString);

            System.out.println(" [x] Sent '" + jsonString + "'");
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending reader event: '" + ex.getMessage() + "'");
        }
    }

    public void sendReaderUserRequestedEvent(ReaderUserRequestedEvent request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString = objectMapper.writeValueAsString(request);

            // Send to both user and reader queues
            this.template.convertAndSend(direct.getName(), UserEvents.USER_REQUESTED, jsonString);
            this.template.convertAndSend(direct.getName(), "reader.user.requested.reader", jsonString);

            System.out.println(" [x] Sent Reader-User Requested event to both queues: '" + jsonString + "'");
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending reader-user requested event: '" + ex.getMessage() + "'");
        }
    }

    public void sendReaderPendingCreated(ReaderPendingCreated event) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString = objectMapper.writeValueAsString(event);

            this.template.convertAndSend(direct.getName(), "reader.pending.created", jsonString);

            System.out.println(" [x] Sent Reader Pending Created event: '" + jsonString + "'");
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending reader pending created event: '" + ex.getMessage() + "'");
        }
    }
}
