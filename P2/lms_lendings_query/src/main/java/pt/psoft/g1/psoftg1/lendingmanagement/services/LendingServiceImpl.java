package pt.psoft.g1.psoftg1.lendingmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingDetailsView;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.LendingNumber;
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

        l.setBookValid(true);
        l.setReaderValid(true);
        l.setLendingStatus("VALIDATED");

        Lending saved=lendingRepository.save(l);

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

        lending.setReturned(desiredVersion, resource.getCommentary());

        if(lending.getDaysDelayed() > 0){
            final var fine = new Fine(lending);
            fineRepository.save(fine);
        }

        return lendingRepository.save(lending);
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
    public List<Lending> getAll() {
        return lendingRepository.findAll();
    }

    @Override
    public Lending create(LendingViewAMQP resource) {
        // Idempotency guard so we do not re-create the same lending if the event is re-delivered
        Optional<Lending> existing = lendingRepository.findByLendingNumber(resource.getLendingNumber());
        if (existing.isPresent()) {
            System.out.println(" [DEBUG] Lending already exists: " + resource.getLendingNumber());
            return existing.get();
        }

        // Retry logic to wait for book/reader to be synced from other services
        // Increased retries to 10 with 1 second interval (total 10 seconds)
        final var book = findBookWithRetry(resource.getIsbn(), 10);
        final var readerDetails = findReaderWithRetry(resource.getReaderNumber(), 10);

        LocalDate startDate = LocalDate.parse(resource.getStartDate());
        LocalDate limitDate = LocalDate.parse(resource.getLimitDate());
        LocalDate returnedDate = resource.getReturnedDate() != null ? LocalDate.parse(resource.getReturnedDate()) : null;

        long version = 0L;
        if (resource.getVersion() != null) {
            try {
                version = Long.parseLong(resource.getVersion());
            } catch (NumberFormatException ignored) {
                // keep default version 0 when the incoming payload has no numeric version
            }
        }

        Lending lending = Lending.builder()
                .book(book)
                .readerDetails(readerDetails)
                .lendingNumber(new LendingNumber(resource.getLendingNumber()))
                .startDate(startDate)
                .limitDate(limitDate)
                .returnedDate(returnedDate)
                .fineValuePerDayInCents(fineValuePerDayInCents)
                .genId(resource.getGenId())
                .readerValid(true)
                .bookValid(true)
                .lendingStatus("VALIDATED")
                .version(version)
                .build();

        return lendingRepository.save(lending);
    }

    private Book findBookWithRetry(String isbn, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            Optional<Book> bookOpt = bookRepository.findByIsbn(isbn);
            if (bookOpt.isPresent()) {
                System.out.println(" [DEBUG] Found book on attempt " + (i + 1) + ": " + isbn);
                return bookOpt.get();
            }
            if (i < maxRetries - 1) {
                System.out.println(" [DEBUG] Book not found yet (attempt " + (i + 1) + "/" + maxRetries + "), retrying in 1000ms: " + isbn);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new NotFoundException("Book not found after " + maxRetries + " retries: " + isbn);
    }

    private ReaderDetails findReaderWithRetry(String readerNumber, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            Optional<ReaderDetails> readerOpt = readerRepository.findByReaderNumber(readerNumber);
            if (readerOpt.isPresent()) {
                System.out.println(" [DEBUG] Found reader on attempt " + (i + 1) + ": " + readerNumber);
                return readerOpt.get();
            }
            if (i < maxRetries - 1) {
                System.out.println(" [DEBUG] Reader not found yet (attempt " + (i + 1) + "/" + maxRetries + "), retrying in 1000ms: " + readerNumber);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new NotFoundException("Reader not found after " + maxRetries + " retries: " + readerNumber);
    }

    @Override
    public Lending update(LendingViewAMQP lending) {
        Optional<Lending> existing = lendingRepository.findByLendingNumber(lending.getLendingNumber());

        // Use the same increased retry logic as in create method
        Book book = findBookWithRetry(lending.getIsbn(), 10);
        ReaderDetails readerDetails = findReaderWithRetry(lending.getReaderNumber(), 10);

        LocalDate startDate = LocalDate.parse(lending.getStartDate());
        LocalDate limitDate = LocalDate.parse(lending.getLimitDate());
        LocalDate returnedDate = lending.getReturnedDate() != null
                ? LocalDate.parse(lending.getReturnedDate())
                : existing.map(Lending::getReturnedDate).orElse(null);

        long version = existing.map(Lending::getVersion).orElse(0L);
        if (lending.getVersion() != null) {
            try {
                version = Long.parseLong(lending.getVersion());
            } catch (NumberFormatException ignored) {
                // keep previously resolved version
            }
        }

        String genId = lending.getGenId() != null ? lending.getGenId() : existing.map(Lending::getGenId).orElse(null);
        String lendingStatus = returnedDate != null
                ? "DELIVERED"
                : existing.map(Lending::getLendingStatus).orElse("VALIDATED");

        Lending updated = Lending.builder()
                .pk(existing.map(Lending::getPk).orElse(0L))
                .book(book)
                .readerDetails(readerDetails)
                .lendingNumber(new LendingNumber(lending.getLendingNumber()))
                .startDate(startDate)
                .limitDate(limitDate)
                .returnedDate(returnedDate)
                .fineValuePerDayInCents(fineValuePerDayInCents)
                .genId(genId)
                .readerValid(true)
                .bookValid(true)
                .lendingStatus(lendingStatus)
                .version(version)
                .commentary(existing.map(Lending::getCommentary).orElse(null))
                .build();

        return lendingRepository.save(updated);
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

    @Override
    public void markLendingAsReturned(String lendingNumber, String comment, Integer grade) {
        System.out.println(" [DEBUG] markLendingAsReturned called for: " + lendingNumber + ", comment: " + comment + ", grade: " + grade);
        
        Lending lending = lendingRepository.findByLendingNumber(lendingNumber)
                .orElseThrow(() -> {
                    System.out.println(" [ERROR] Lending not found with number: " + lendingNumber);
                    return new NotFoundException("Lending not found: " + lendingNumber);
                });
        
        System.out.println(" [DEBUG] Found existing lending: " + lending.getLendingNumber());
        
        if (comment != null && !comment.isEmpty()) {
            lending.setCommentary(comment);
            System.out.println(" [DEBUG] Set comment: " + comment);
        }
        
        lending.markAsReturned();
        System.out.println(" [DEBUG] Marked as returned - status: " + lending.getLendingStatus() + ", returnedDate: " + lending.getReturnedDate());
        
        Lending saved = lendingRepository.save(lending);
        System.out.println(" [DEBUG] Lending saved successfully: " + saved.getLendingNumber() + " with returnedDate: " + saved.getReturnedDate());
    }

    @Override
    public void setLendingStatusDelivered(String lendingNumber) {
        Lending lending = lendingRepository.findByLendingNumber(lendingNumber)
                .orElseThrow(() -> new NotFoundException("Lending not found: " + lendingNumber));
        
        lending.markAsReturned();
        lendingRepository.save(lending);
        System.out.println(" [x] Lending status set to DELIVERED and returned_date set for: " + lendingNumber);
    }


}
