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
        System.out.println(" [LENDING] Creating Lending");

        final var readerDetails = readerRepository.findByUsername(resource.getUsername())
            .orElseThrow(() -> new NotFoundException("Reader not found"));

        Iterable<Lending> lendingList = lendingRepository.listOutstandingByReaderNumber(readerDetails.getReaderNumber());
        System.out.println(" [LENDING] Validating reader lending rules...");
        for (Lending lending : lendingList) {

            //Business rule: cannot create a lending if user has late outstanding books to return.
            if (lending.getDaysDelayed() > 0) {
                throw new LendingForbiddenException("Reader has book(s) past their due date");
            }
            count++;
            //Business rule: cannot create a lending if user already has 3 outstanding books to return.
            if (count >= 3) {
                throw new LendingForbiddenException("Reader has three books outstanding already");
            }
        }

        // Check if book already exists locally (from previous book creation events)
        Book bookToUse = bookRepository.findByIsbn(resource.getIsbn()).orElseGet(() -> {
            System.out.println(" [LENDING] Creating pending book placeholder for ISBN: " + resource.getIsbn());
            // Create and persist a temporary book placeholder - will be validated asynchronously
            Book tempBook = new Book(resource.getIsbn(), "Pending Validation", "Book under validation", null);
            return bookRepository.save(tempBook);
        });

        System.out.println(" [LENDING] Reader details: " + readerDetails.getReaderNumber());

        int seq = lendingRepository.getCountFromCurrentYear()+1;
        System.out.println(" [LENDING] Creating Lending object with seq: " + seq);

        final Lending l = new Lending(bookToUse, readerDetails, seq, lendingDurationInDays, fineValuePerDayInCents);

        // Set initial status as PENDING_VALIDATION (will be validated asynchronously)
        l.setBookValid(false);
        l.setReaderValid(true);
        l.setLendingStatus("PENDING_VALIDATION");
        System.out.println(" [LENDING] Lending status: PENDING_VALIDATION");

        Lending saved = lendingRepository.save(l);
        System.out.println(" [LENDING] Lending saved with pending status: " + saved.getLendingNumber());

        // Send asynchronous validation request to Books Command via RabbitMQ
        try {
            lendingEventPublisher.requestBookValidation(resource.getIsbn(), saved.getLendingNumber());
            System.out.println(" [LENDING] 📤 Validation request sent for: " + saved.getLendingNumber());
        } catch (Exception e) {
            System.err.println(" [LENDING] ❌ Failed to send validation request: " + e.getMessage());
            // If we can't send the validation request, delete the pending lending
            lendingRepository.delete(saved);
            throw new RuntimeException("Failed to initiate book validation for lending", e);
        }

        // Return immediately - validation will happen asynchronously
        // Client receives 202 ACCEPTED
        return saved;
    }

    @Override
    public Lending createWithDetails(LendingDetailsView resource) {
        // Validate that the book exists in the Books service
        System.out.println("Checking if book exists in Books service with ISBN: " + resource.getBookIsbn());
        boolean bookExists = booksServiceClient.checkBookExists(resource.getBookIsbn());
        if (!bookExists) {
            throw new NotFoundException("Book with ISBN " + resource.getBookIsbn() + " does not exist in Books service");
        }
        System.out.println("Book exists in Books service");

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

        // Use the current version from the fetched entity to avoid race conditions
        // The optimistic locking will be handled by Hibernate on save
        lending.setReturned(lending.getVersion(), resource.getComment());

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

    @Override
    public void processBookValidationResponse(pt.psoft.g1.psoftg1.lendingmanagement.api.LendingValidationResponse response) {
        try {
            System.out.println(" [LENDING] 🔄 Processing book validation response for lending: " + response.getLendingNumber());

            // Find the lending by lending number
            Lending lending = lendingRepository.findByLendingNumber(response.getLendingNumber())
                    .orElseThrow(() -> new NotFoundException("Lending not found: " + response.getLendingNumber()));

            if (response.isBookExists()) {
                // Book exists - validate and finalize the lending
                System.out.println(" [LENDING] ✅ Book validated successfully: " + response.getIsbn());

                // Try to get the real book from local repository (synchronized from Books Command events)
                Optional<Book> realBook = bookRepository.findByIsbn(response.getIsbn());

                if (realBook.isPresent() && !realBook.get().getTitle().toString().equals("Pending Validation")) {
                    // Real book data is available, update the lending with it
                    System.out.println(" [LENDING] 📚 Updating lending with real book data: " + realBook.get().getTitle());
                    lending.setBook(realBook.get());
                } else {
                    System.out.println(" [LENDING] ⚠️ Real book data not yet synchronized, keeping placeholder");
                }

                lending.setBookValid(true);
                lending.setLendingStatus("VALIDATED");

                Lending saved = lendingRepository.save(lending);

                if (saved != null) {
                    lendingEventPublisher.sendLendingCreated(saved);
                    System.out.println(" [LENDING] 📤 Lending created event sent: " + saved.getLendingNumber());
                }
            } else {
                // Book does not exist - reject the lending
                System.out.println(" [LENDING] ❌ Book validation failed: " + response.getIsbn());
                System.out.println(" [LENDING] ⚠️ Rejecting lending: " + response.getLendingNumber());

                // Delete the pending lending
                lendingRepository.delete(lending);
                System.out.println(" [LENDING] 🗑️ Pending lending deleted: " + response.getLendingNumber());
            }

        } catch (NotFoundException e) {
            System.err.println(" [LENDING] ❌ Lending not found: " + response.getLendingNumber());
        } catch (Exception e) {
            System.err.println(" [LENDING] ❌ Error processing validation response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
