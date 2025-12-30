package pt.psoft.g1.psoftg1.shared.listeners;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.bootstrapping.Bootstrapper;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQP;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreService;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.api.ViewContainer;
import pt.psoft.g1.psoftg1.shared.publishers.RpcBootstrapPublisher;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RpcBootstrapListener {

    private final ReaderService readerService;
    private final BookService bookService;
    private final GenreService genreService;
    private final LendingService lendingService;
    private final RpcBootstrapPublisher rpcBootstrapPublisher;




    @RabbitListener(queues = "#{readerServiceInstanciatedQueue.name}")
    @Transactional
    public void receiveReaderServiceInstanciatedResponse(Message msg) {

        try {
            System.out.println(" [x] Received Reader Service instanciated resquest.");

            ViewContainer viewContainer = new ViewContainer();

            viewContainer.setReaders(readerService.getAllReaders());

            viewContainer.setBooks(bookService.getAllBooks());

            viewContainer.setGenres(genreService.getAllGenres());

            viewContainer.setLendings(lendingService.getAllLendings());

            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString = objectMapper.writeValueAsString(viewContainer);



            rpcBootstrapPublisher.sendRpcBootstrapResponse(jsonString,msg.getMessageProperties().getCorrelationId(),msg.getMessageProperties().getReplyTo());

        } catch (Exception e) {
            System.out.println(e);

            System.out.println(" [x] Error bootstrapping Reader Service.");
        }

    }
}
