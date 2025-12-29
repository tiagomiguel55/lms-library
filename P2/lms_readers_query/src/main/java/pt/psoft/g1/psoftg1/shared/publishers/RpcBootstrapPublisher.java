package pt.psoft.g1.psoftg1.shared.publishers;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.api.ViewContainer;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RpcBootstrapPublisher {
    @Autowired
    private RabbitTemplate template;
    @Autowired
    private DirectExchange direct;

    private boolean hasReceivedResponse = false;

    public void sendRpcBootstrapResponse(String jsonString, String correlationId, String replyTo) {
        try {
            MessageProperties props = new MessageProperties();
            props.setCorrelationId(correlationId);
            //props.setReplyTo(replyTo);  // Fila para a resposta

            Message responseMessage = new Message(jsonString.getBytes(), props);
            this.template.send(replyTo, responseMessage);
            System.out.println(" [x] Sent '" + jsonString + "'");
        } catch (Exception ex) {
            System.out.println(" [x] Exception sending rpc bootstrap response: '" + ex.getMessage() + "'");
        }
    }

    public ViewContainer sendRpcBootstrapRequest() {
        try {
            String replyQueueName = "rpcReplyQueue";

            MessageProperties props = new MessageProperties();
            String correlationId = UUID.randomUUID().toString();
            props.setCorrelationId(correlationId);
            props.setReplyTo(replyQueueName);  // Fila para a resposta


            ViewContainer request = new ViewContainer();
            String jsonMessage = new ObjectMapper().writeValueAsString(request);
            Message requestMessage = new Message(jsonMessage.getBytes(), props);

            template.setReceiveTimeout(5000);

            // Enviar a solicitação para a fila principal (onde o servidor vai ouvir)
            Message responseMessage = template.sendAndReceive(direct.getName(),"rpc.bootstrap.request", requestMessage);

            String responseCorrelationId = responseMessage.getMessageProperties().getCorrelationId();
            if (responseCorrelationId == null || !responseCorrelationId.equals(correlationId)) {
                System.out.println(" [x] Ignorando resposta com correlationId diferente.");
                return null; // Ignorar a resposta se o correlationId não corresponder
            }

            // Se já recebeu uma resposta antes, ignora esta resposta
            if (hasReceivedResponse) {
                System.out.println(" [x] Ignorando resposta adicional.");
                return null;
            }

            // Marcar que uma resposta foi recebida
            hasReceivedResponse = true;


            // Processar a resposta da fila de resposta
            return new ObjectMapper().readValue(responseMessage.getBody(), ViewContainer.class);
        } catch (Exception ex) {
            System.out.println(" [x] Exception sending rpc bootstrap request: '" + ex.getMessage() + "'");
            return null;
        }
    }

}
