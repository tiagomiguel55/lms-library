package pt.psoft.g1.psoftg1.lendingmanagement.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.lendingmanagement.api.SagaCreationResponse;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class LendingEventListener {

    private final LendingService lendingService;

    @RabbitListener(queues = "#{bookLendingResponseQueue.name}")
    public void receiveBookLendingResponse(Message msg) {
        try {
            // Usando ObjectMapper para converter a string JSON em um objeto BookCreationResponse
            ObjectMapper objectMapper = new ObjectMapper();
            String message = new String(msg.getBody(), StandardCharsets.UTF_8);
            SagaCreationResponse response = objectMapper.readValue(message, SagaCreationResponse.class);

            String lendingNumber = response.getLendingNumber();
            String status = response.getStatus();

            if (status.equals("SUCCESS")) {
                System.out.println("Livro associado com sucesso ao Lending " + lendingNumber);
                lendingService.bookValidated(lendingNumber);
            } else {
                String error = response.getError();
                System.out.println("Falha ao associar livro ao Lending " + lendingNumber + ". Erro: " + error);
                lendingService.delete(lendingNumber);
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar a resposta: " + e.getMessage());
        }
    }

    @RabbitListener(queues = "#{readerLendingResponseQueue.name}")
    public void receiveReaderLendingResponse(Message msg) {
        try {
            // Usando ObjectMapper para converter a string JSON em um objeto BookCreationResponse
            ObjectMapper objectMapper = new ObjectMapper();
            String message = new String(msg.getBody(), StandardCharsets.UTF_8);
            SagaCreationResponse response = objectMapper.readValue(message, SagaCreationResponse.class);

            String lendingNumber = response.getLendingNumber();
            String status = response.getStatus();
            System.out.println(status);

            if (status.equals("SUCCESS")) {
                System.out.println("reader associado com sucesso ao Lending " + lendingNumber);
                lendingService.readerValidated(lendingNumber);
            } else {
                String error = response.getError();
                System.out.println("Falha ao associar reader ao Lending " + lendingNumber + ". Erro: " + error);
                lendingService.delete(lendingNumber);
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar a resposta: " + e.getMessage());
        }
    }


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
