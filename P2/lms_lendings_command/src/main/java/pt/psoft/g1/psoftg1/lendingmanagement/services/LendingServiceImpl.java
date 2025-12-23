package pt.psoft.g1.psoftg1.lendingmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.external.service.BooksServiceClient;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingDetailsView;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.publishers.LendingEventPublisher;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@PropertySource({"classpath:config/library.properties"})
public class LendingServiceImpl implements LendingService{
    private final LendingRepository lendingRepository;
    private final FineRepository fineRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final LendingEventPublisher lendingEventPublisher;
    private final BooksServiceClient booksServiceClient;

    @Value("${lendingDurationInDays}")
    private int lendingDurationInDays;
    @Value("${fineValuePerDayInCents}")
    private int fineValuePerDayInCents;
    private final UserRepository userRepository;

    @Override
    public Optional<Lending> findByLendingNumber(String lendingNumber){
        //System.out.println(lendingRepository.findByLendingNumber(lendingNumber).get().getClass());
        Optional<Lending> lending = lendingRepository.findByLendingNumber(lendingNumber);
        System.out.println(lending);
        return lending;
    }

    @Override
    public List<Lending> listByReaderNumberAndIsbn(String readerNumber, String isbn, Optional<Boolean> returned){
        List<Lending> lendings = lendingRepository.listByReaderNumberAndIsbn(readerNumber, isbn);
        if(returned.isEmpty()){
            return lendings;
        }else{
            for(int i = 0; i < lendings.size(); i++){
                if ((lendings.get(i).getReturnedDate() == null) == returned.get()){
                    lendings.remove(i);
                    i--;
                }
            }
        }
        return lendings;
    }

    @Override
    public Lending create(final CreateLendingRequest resource) {
        int count = 0;
        System.out.println("Creating Lending");

        final var readerDetails = readerRepository.findByUsername(resource.getUsername())
            .orElseThrow(() -> new NotFoundException("Reader not found"));

        Iterable<Lending> lendingList = lendingRepository.listOutstandingByReaderNumber(readerDetails.getReaderNumber());
        System.out.println("Lending list");
        for (Lending lending : lendingList) {

            //Business rule: cannot create a lending if user has late outstanding books to return.
            if (lending.getDaysDelayed() > 0) {
                throw new LendingForbiddenException("Reader has book(s) past their due date");
            }
            count++;
            System.out.println(count);
            //Business rule: cannot create a lending if user already has 3 outstanding books to return.
            if (count >= 3) {
                throw new LendingForbiddenException("Reader has three books outstanding already");
            }
        }

        // Check if book exists in the Books service
        System.out.println("Checking if book exists in Books service with ISBN: " + resource.getIsbn());
        boolean bookExists = booksServiceClient.checkBookExists(resource.getIsbn());
        if (!bookExists) {
            throw new NotFoundException("Book with ISBN " + resource.getIsbn() + " does not exist in Books service");
        }
        System.out.println("Book exists in Books service");

        System.out.println("Fetching book with ISBN: " + resource.getIsbn());
        final var b = bookRepository.findByIsbn(resource.getIsbn())
                .orElseThrow(() -> new NotFoundException("Book not found"));
        System.out.println("Book found: " + b.getIsbn());
        
        final var r = readerDetails;
        System.out.println("Reader details: " + r.getReaderNumber());
        
        int seq = lendingRepository.getCountFromCurrentYear()+1;
        System.out.println("Creating Lending object with seq: " + seq);
        
        final Lending l = new Lending(b,r,seq, lendingDurationInDays, fineValuePerDayInCents );
        System.out.println("Lending object created successfully");

        l.setBookValid(true);
        l.setReaderValid(true);
        l.setLendingStatus("VALIDATED");
        System.out.println("About to save lending");

        Lending saved=lendingRepository.save(l);
        System.out.println("Lending saved successfully");

        if (saved != null) {
            lendingEventPublisher.sendLendingCreated(saved);
        }

        return saved;
    }

    @Override
    public Lending createWithDetails(LendingDetailsView resource) {
        // Validar os dados recebidos e criar os objetos necessários
        final var book = createBookFromDetails(resource);

        final var readerDetails = createReaderFromDetails(resource);



        int seq = lendingRepository.getCountFromCurrentYear()+1;
        // Criar o Lending
        final var lending = new Lending(book, readerDetails, seq, lendingDurationInDays, fineValuePerDayInCents);

        lending.setLendingStatus("PENDENT");
        // Guardar o Lending
        //userRepository.save(readerDetails.getReader());
        Lending saved = lendingRepository.save(lending);

        if (saved != null) {
            //lendingEventPublisher.sendLendingCreated(saved);
            lendingEventPublisher.sendBookCreatedInLending(resource, saved.getLendingNumber());
            lendingEventPublisher.sendReaderCreatedInLending(resource, saved.getLendingNumber());
        }


        return lending;
    }

    private Book createBookFromDetails(LendingDetailsView lendingDetails) {
        return new Book(lendingDetails.getBookIsbn(),
                lendingDetails.getBookTitle(),
                lendingDetails.getBookDescription(),
                null);
    }

    private ReaderDetails createReaderFromDetails(LendingDetailsView lendingDetails) {

        Reader reader = new Reader(lendingDetails.getReaderUsername(), lendingDetails.getReaderPassword());

        reader.setName(lendingDetails.getReaderFullName());

        int readerNumber =Integer.parseInt(lendingDetails.getReaderNumber().split("/")[1]);

        return new ReaderDetails(readerNumber, reader);
    }

    @Override
    public Lending setReturned(final String lendingNumber, final SetLendingReturnedRequest resource, final long desiredVersion) {

        var lending = lendingRepository.findByLendingNumber(lendingNumber)
                .orElseThrow(() -> new NotFoundException("Cannot update lending with this lending number"));

        lending.setReturned(desiredVersion, resource.getComment());

        if(lending.getDaysDelayed() > 0){
            final var fine = new Fine(lending);
            fineRepository.save(fine);
        }

        Lending savedLending = lendingRepository.save(lending);

        // Publish LendingReturned event
        lendingEventPublisher.sendLendingReturned(
            savedLending.getLendingNumber(),
            savedLending.getBook().getIsbn(),
            savedLending.getReaderDetails().getReaderNumber(),
            resource.getComment(),
            resource.getGrade()
        );

        return savedLending;
    }

    @Override
    public Double getAverageDuration(){
        Double avg = lendingRepository.getAverageDuration();
        return Double.valueOf(String.format(Locale.US,"%.1f", avg));
    }

    @Override
    public List<Lending> getOverdue(Page page) {
        if (page == null) {
            page = new Page(1, 10);
        }
        return lendingRepository.getOverdue(page);
    }

    @Override
    public Double getAvgLendingDurationByIsbn(String isbn){
        Double avg = lendingRepository.getAvgLendingDurationByIsbn(isbn);
        return Double.valueOf(String.format(Locale.US,"%.1f", avg));
    }

    @Override
    public List<Lending> searchLendings(Page page, SearchLendingQuery query){
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (page == null) {
            page = new Page(1, 10);
        }
        if (query == null)
            query = new SearchLendingQuery("",
                    "",
                    null,
                    LocalDate.now().minusDays(10L).toString(),
                    null);

        try {
            if(query.getStartDate()!=null)
                startDate = LocalDate.parse(query.getStartDate());
            if(query.getEndDate()!=null)
                endDate = LocalDate.parse(query.getEndDate());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Expected format is YYYY-MM-DD");
        }

        return lendingRepository.searchLendings(page,
                query.getReaderNumber(),
                query.getIsbn(),
                query.getReturned(),
                startDate,
                endDate);

    }

    @Override
    public Lending create(LendingViewAMQP resource) {

        int count = 0;
        System.out.println("Creating Lending");
        Iterable<Lending> lendingList = lendingRepository.listOutstandingByReaderNumber(resource.getReaderNumber());
        System.out.println("Lending list");
        for (Lending lending : lendingList) {

            //Business rule: cannot create a lending if user has late outstanding books to return.
            if (lending.getDaysDelayed() > 0) {
                throw new LendingForbiddenException("Reader has book(s) past their due date");
            }
            count++;
            System.out.println(count);
            //Business rule: cannot create a lending if user already has 3 outstanding books to return.
            if (count >= 3) {
                throw new LendingForbiddenException("Reader has three books outstanding already");
            }
        }

        final var b = bookRepository.findByIsbn(resource.getIsbn())
                .orElseThrow(() -> new NotFoundException("Book not found"));
        final var r = readerRepository.findByReaderNumber(resource.getReaderNumber())
                .orElseThrow(() -> new NotFoundException("Reader not found"));
        int seq = lendingRepository.getCountFromCurrentYear()+1;
        final Lending l = new Lending(b,r,seq, lendingDurationInDays, fineValuePerDayInCents );

        l.setReaderValid(true);
        l.setBookValid(true);
        l.setLendingStatus("VALIDATED");

        Lending saved=lendingRepository.save(l);

        return saved;
    }

    @Override
    public Lending update(LendingViewAMQP lending) {
        return null;
        // tentar fazer se possivel
    }

    @Override
    public void delete(LendingViewAMQP lending) {

        Lending l = lendingRepository.findByLendingNumber(lending.getLendingNumber())
                .orElseThrow(() -> new NotFoundException("Cannot update lending with this lending number"));

        lendingRepository.delete(l);
    }

    @Override
    public void delete(String lendingNumber) {
        Lending l = lendingRepository.findByLendingNumber(lendingNumber)
                .orElseThrow(() -> new NotFoundException("Cannot update lending with this lending number"));

        lendingRepository.delete(l);
    }

    @Override
    public void readerValidated(String lendingNumber) {
        Lending l = lendingRepository.findByLendingNumber(lendingNumber)
                .orElseThrow(() -> new NotFoundException("No Lending Found"));

        if (!l.readerValid) {
            l.setReaderValid(true);

                if (l.bookValid && l.getLendingStatus().equals("PENDENT")) {
                    l.setLendingStatus("VALIDATED");
                }
            System.out.println("version"+l.getVersion());
            Lending saved = lendingRepository.save(l);

                if (saved != null) {
                    if (saved.getLendingStatus().equals("VALIDATED")) {
                        lendingEventPublisher.sendLendingCreated(saved);

                    }
                }

        }

    }

    @Override
    public void bookValidated(String lendingNumber) {
        Lending l = lendingRepository.findByLendingNumber(lendingNumber)
                .orElseThrow(() -> new NotFoundException("No Lending Found"));

        if (!l.bookValid) {
            l.setBookValid(true);

            if (l.readerValid && l.getLendingStatus().equals("PENDENT")) {
                l.setLendingStatus("VALIDATED");
            }

            Lending saved = lendingRepository.save(l);

            if (saved != null) {
                if (saved.getLendingStatus().equals("VALIDATED")) {
                    lendingEventPublisher.sendLendingCreated(saved);

                }
            }

        }
    }
}
