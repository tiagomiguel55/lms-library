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

            System.out.println(" [x] Received Reader Created by AMQP: " + msg + ".");
            try {
                readerService.create(readerViewAMQP);
                System.out.println(" [x] New reader inserted from AMQP: " + msg + ".");
            } catch (Exception e) {

                e.printStackTrace();
                System.out.println(" [x] Reader already exists. No need to store it.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving reader event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{readerUpdatedQueue.name}")
    public void receiveReaderUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] Received reader Updated by AMQP: " + msg + ".");
            try {
                readerService.update(readerViewAMQP);
                System.out.println(" [x] reader updated from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] reader does not exists or wrong version. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving reader event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{readerDeletedQueue.name}")
    public void receiveReaderDeleted(String in) {
        System.out.println(" [x] Received reader Deleted '" + in + "'");

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(in.getBytes(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] Received reader Deleted by AMQP: " + in + ".");
            try {
                readerService.delete(readerViewAMQP);
                System.out.println(" [x] reader deleted from AMQP: " + in + ".");
            } catch (Exception e) {
                System.out.println(" [x] reader does not exists. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving reader event from AMQP: '" + ex.getMessage() + "'");
        }
    }
}