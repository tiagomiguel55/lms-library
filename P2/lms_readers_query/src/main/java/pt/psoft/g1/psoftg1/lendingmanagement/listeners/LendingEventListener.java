package pt.psoft.g1.psoftg1.lendingmanagement.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class LendingEventListener {

    private final LendingService lendingService;

    @RabbitListener(queues = "#{lendingCreatedQueue.name}")
    public void receiveBookCreated(Message msg) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            LendingViewAMQP lendingViewAMQP = objectMapper.readValue(jsonReceived, LendingViewAMQP.class);

            System.out.println(" [x] Received lending Created by AMQP: " + msg + ".");
            try {
                lendingService.create(lendingViewAMQP);
                System.out.println(" [x] New lending inserted from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] lending already exists. No need to store it.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving lending event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{lendingUpdatedQueue.name}")
    public void receiveBookUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            LendingViewAMQP lendingViewAMQP = objectMapper.readValue(jsonReceived, LendingViewAMQP.class);

            System.out.println(" [x] Received lending Updated by AMQP: " + msg + ".");
            try {
                lendingService.update(lendingViewAMQP);
                System.out.println(" [x] lending updated from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] lending does not exists or wrong version. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving lending event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{lendingDeletedQueue.name}")
    public void receiveBookDeleted(String in) {
        System.out.println(" [x] Received lending Deleted '" + in + "'");
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(in.getBytes(), StandardCharsets.UTF_8);
            LendingViewAMQP lendingViewAMQP = objectMapper.readValue(jsonReceived, LendingViewAMQP.class);

            System.out.println(" [x] Received lending Deleted by AMQP: " + in + ".");
            try {
                lendingService.delete(lendingViewAMQP);
                System.out.println(" [x] lending deleted from AMQP: " + in + ".");
            } catch (Exception e) {
                System.out.println(" [x] lending does not exists. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving lending event from AMQP: '" + ex.getMessage() + "'");
        }
    }

}
