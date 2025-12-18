package pt.psoft.g1.psoftg1.genremanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreService;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class GenreRabbitmqController {

    @Autowired
    private final GenreService genreService;

    @RabbitListener(queues = "#{autoDeleteQueue_Genre_Created.name}")
    public void receiveGenreCreatedMsg(Message msg) {

        try {
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            GenreViewAMQP genreViewAMQP = objectMapper.readValue(jsonReceived, GenreViewAMQP.class);

            System.out.println(" [x] Received Genre Created by AMQP: " + msg + ".");
            try {
                // Query service needs to handle AMQP message and create genre
                System.out.println(" [x] New genre received from AMQP: " + genreViewAMQP.getGenre());
            } catch (Exception e) {
                System.out.println(" [x] Genre already exists. No need to store it.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving genre event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Genre_Updated.name}")
    public void receiveGenreUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            GenreViewAMQP genreViewAMQP = objectMapper.readValue(jsonReceived, GenreViewAMQP.class);

            System.out.println(" [x] Received Genre Updated by AMQP: " + msg + ".");
            try {
                // Query service needs to handle AMQP message and update genre
                System.out.println(" [x] Genre updated from AMQP: " + genreViewAMQP.getGenre());
            } catch (Exception e) {
                System.out.println(" [x] Genre does not exists or wrong version. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving genre event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Genre_Deleted.name}")
    public void receiveGenreDeleted(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            GenreViewAMQP genreViewAMQP = objectMapper.readValue(jsonReceived, GenreViewAMQP.class);

            System.out.println(" [x] Received Genre Deleted by AMQP: " + msg + ".");
            try {
                // Query service needs to handle AMQP message and delete genre
                System.out.println(" [x] Genre deleted from AMQP: " + genreViewAMQP.getGenre());
            } catch (Exception e) {
                System.out.println(" [x] Genre does not exists. Nothing to delete.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving genre event from AMQP: '" + ex.getMessage() + "'");
        }
    }
}

