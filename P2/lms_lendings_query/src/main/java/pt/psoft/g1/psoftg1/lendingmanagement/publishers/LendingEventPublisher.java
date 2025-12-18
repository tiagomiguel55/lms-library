package pt.psoft.g1.psoftg1.lendingmanagement.publishers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookSagaViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingDetailsView;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQPMapper;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderSagaViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.shared.model.LendingEvents;

@Component
@RequiredArgsConstructor
public class LendingEventPublisher {

    @Autowired
    private RabbitTemplate template;
    @Autowired
    private DirectExchange direct;

    private final LendingViewAMQPMapper lendingViewAMQPMapper;

    public void sendBookCreatedInLending(LendingDetailsView lendingDetailsView, String lendingNumber) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            BookSagaViewAMQP bookViewAMQP = new BookSagaViewAMQP();

            bookViewAMQP.setLendingNumber(lendingNumber);
            bookViewAMQP.setIsbn(lendingDetailsView.getBookIsbn());
            bookViewAMQP.setTitle(lendingDetailsView.getBookTitle());
            bookViewAMQP.setDescription(lendingDetailsView.getBookDescription());
            bookViewAMQP.setAuthorIds(lendingDetailsView.getBookAuthorIds());
            bookViewAMQP.setGenre(lendingDetailsView.getBookGenre());



            String jsonString = objectMapper.writeValueAsString(bookViewAMQP);

            this.template.convertAndSend(direct.getName(), "book.lending.requests", jsonString);
            System.out.println(" [x] Sent '" + lendingDetailsView.getBookIsbn() + "'");
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending lending event: '" + ex.getMessage() + "'");
        }
    }

    public void sendLendingCreated(Lending lending) {
        sendLendingEvent(lending,lending.getVersion() , LendingEvents.LENDING_CREATED);
    }

    public void sendLendingUpdated(Lending lending, Long currentVersion) {
        sendLendingEvent(lending,currentVersion, LendingEvents.LENDING_UPDATED);
    }


    public void sendLendingDeleted(Lending lending, Long currentVersion) {
        sendLendingEvent(lending,currentVersion, LendingEvents.LENDING_DELETED);
    }


    public void sendLendingEvent(Lending lending, Long currentVersion, String lendingEventType) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            LendingViewAMQP lendingViewAMQP = lendingViewAMQPMapper.toLendingViewAMQP(lending);
            System.out.println(lendingViewAMQP);
            lendingViewAMQP.setVersion(currentVersion.toString());

            String jsonString = objectMapper.writeValueAsString(lendingViewAMQP);

            this.template.convertAndSend(direct.getName(), lendingEventType, jsonString);
            System.out.println(" [x] Sent '" + lending.getLendingNumber() + "'");
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending lending event: '" + ex.getMessage() + "'");
        }
    }

    public void sendReaderCreatedInLending(LendingDetailsView resource, String lendingNumber) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            ReaderSagaViewAMQP readerViewAMQP = new ReaderSagaViewAMQP();

            readerViewAMQP.setLendingNumber(lendingNumber);
            readerViewAMQP.setUsername(resource.getReaderUsername());
            readerViewAMQP.setFullName(resource.getReaderFullName());
            readerViewAMQP.setPassword(resource.getReaderPassword());
            readerViewAMQP.setReaderNumber(resource.getReaderNumber());
            readerViewAMQP.setBirthDate(resource.getReaderBirthDate());
            readerViewAMQP.setPhoneNumber(resource.getReaderPhoneNumber());
            readerViewAMQP.setGdpr(resource.isReaderGdpr());
            readerViewAMQP.setMarketing(resource.isReaderMarketing());
            readerViewAMQP.setThirdParty(resource.isReaderThirdParty());
            readerViewAMQP.setInterestList(resource.getReaderInterestList());

            // continuar

            String jsonString = objectMapper.writeValueAsString(readerViewAMQP);

            this.template.convertAndSend(direct.getName(), "reader.lending.requests", jsonString);
            System.out.println(" [x] Sent '" + resource.getReaderUsername() + "'");
        }
        catch( Exception ex ) {
            System.out.println(" [x] Exception sending reader lending event: '" + ex.getMessage() + "'");
        }
    }
}
