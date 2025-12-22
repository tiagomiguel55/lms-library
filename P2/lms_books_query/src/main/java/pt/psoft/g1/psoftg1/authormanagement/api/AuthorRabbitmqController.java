package pt.psoft.g1.psoftg1.authormanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthorRabbitmqController {

    @Autowired
    private final AuthorService authorService;

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Created.name}")
    public void receiveAuthorCreatedMsg(Message msg) {

        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

            System.out.println(" [x] Received Author Created by AMQP: " + msg + ".");
            try {
                // Query service needs to handle AMQP message and create author
                // For now, log the event - actual implementation depends on your service layer
                System.out.println(" [x] New author received from AMQP: " + authorViewAMQP.getName());
            } catch (Exception e) {
                System.out.println(" [x] Author already exists. No need to store it.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Updated.name}")
    public void receiveAuthorUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

            System.out.println(" [x] Received Author Updated by AMQP: " + msg + ".");
            try {
                // Query service needs to handle AMQP message and update author
                System.out.println(" [x] Author updated from AMQP: " + authorViewAMQP.getName());
            } catch (Exception e) {
                System.out.println(" [x] Author does not exists or wrong version. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Deleted.name}")
    public void receiveAuthorDeleted(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

            System.out.println(" [x] Received Author Deleted by AMQP: " + msg + ".");
            try {
                // Query service needs to handle AMQP message and delete author
                System.out.println(" [x] Author deleted from AMQP: " + authorViewAMQP.getName());
            } catch (Exception e) {
                System.out.println(" [x] Author does not exists. Nothing to delete.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
        }
    }
}