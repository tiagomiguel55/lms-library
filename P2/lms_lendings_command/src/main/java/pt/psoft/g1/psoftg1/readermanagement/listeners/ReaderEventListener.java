package pt.psoft.g1.psoftg1.readermanagement.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ReaderEventListener {

    private final ReaderService readerService;

    @RabbitListener(queues = "#{readerCreatedQueue.name}")
    public void receiveReaderCreated(Message msg) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] Received Reader Created event via AMQP - Reader Number: " + readerViewAMQP.getReaderNumber());
            try {
                readerService.create(readerViewAMQP);
                System.out.println(" [x] Reader successfully stored in lendings_command service");
            } catch (Exception e) {
                System.out.println(" [x] Error creating reader in lendings_command: " + e.getMessage());
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving reader created event from AMQP: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @RabbitListener(queues = "#{readerUpdatedQueue.name}")
    public void receiveReaderUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] Received Reader Updated event via AMQP - Reader Number: " + readerViewAMQP.getReaderNumber());
            try {
                readerService.update(readerViewAMQP);
                System.out.println(" [x] Reader successfully updated in lendings_command service");
            } catch (Exception e) {
                System.out.println(" [x] Error updating reader in lendings_command: " + e.getMessage());
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving reader updated event from AMQP: " + ex.getMessage());
        }
    }

    @RabbitListener(queues = "#{readerDeletedQueue.name}")
    public void receiveReaderDeleted(String in) {
        System.out.println(" [x] Received Reader Deleted event via AMQP");

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(in.getBytes(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] Processing Reader Deleted - Reader Number: " + readerViewAMQP.getReaderNumber());
            try {
                readerService.delete(readerViewAMQP);
                System.out.println(" [x] Reader successfully deleted in lendings_command service");
            } catch (Exception e) {
                System.out.println(" [x] Error deleting reader in lendings_command: " + e.getMessage());
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving reader deleted event from AMQP: " + ex.getMessage());
        }
    }
}