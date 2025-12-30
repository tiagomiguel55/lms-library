package pt.psoft.g1.psoftg1.lendingmanagement.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.StaleObjectStateException;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.shared.services.generator.IdGeneratorFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@code Lending} class associates a {@code Reader} and a {@code Book}.
 * <p>It stores the date it was registered, the date it is supposed to
 * be returned, and the date it actually was returned.
 * It also stores an optional reader {@code commentary} (submitted at the time of the return) and
 * the {@code Fine}, if applicable.
 * <p>It is identified in the system by an auto-generated {@code id}, and has a unique-constrained
 * natural key ({@code LendingNumber}) with its own business rules.
 */
@Getter
@NoArgsConstructor
public class Lending {


    private LendingNumber lendingNumber;

    private  String genId;

    private Book book;

    private ReaderDetails readerDetails;

    @Getter
    private LocalDate startDate;

    @Getter
    private LocalDate limitDate;

    @Getter
    private LocalDate returnedDate;


    private long version;


    private String commentary = null;

    private int fineValuePerDayInCents;

    private Integer daysUntilReturn;

    @Getter
    private Integer daysOverdue;


    private final IdGeneratorFactory idGeneratorFactory = new IdGeneratorFactory();

    /**
     * Constructs a new {@code Lending} object.
     *
     * @param book             {@code Book} object, which should be retrieved from the database.
     * @param readerDetails    {@code Reader} object, which should be retrieved from the database.
     * @param seq              sequential number, which should be obtained from the year's count on the database.
     * @param lendingDuration  duration for the lending in days.
     * @param fineValuePerDayInCents fine value per day in cents.
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public Lending(Book book, ReaderDetails readerDetails, int seq, int lendingDuration, int fineValuePerDayInCents) {
        try {
            this.book = Objects.requireNonNull(book);
            this.readerDetails = Objects.requireNonNull(readerDetails);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Null objects passed to lending");
        }
        this.lendingNumber = new LendingNumber(seq);
        this.startDate = LocalDate.now();
        this.limitDate = LocalDate.now().plusDays(lendingDuration);
        this.returnedDate = null;
        this.fineValuePerDayInCents = fineValuePerDayInCents;
        setDaysUntilReturn();
        setDaysOverdue();
        setGenId(null);
    }

    public void setGenId(String genId) {
        if (this.genId == null) {
            this.genId = idGeneratorFactory.getGenerator().generateId();
        }else {
            this.genId = genId;
        }
    }
    @Builder
    public Lending(Book book, ReaderDetails readerDetails, LendingNumber lendingNumber, LocalDate startDate, LocalDate limitDate, LocalDate returnedDate, int fineValuePerDayInCents, String genId) {
        try {
            this.book = Objects.requireNonNull(book);
            //System.out.println("Book ON THE @BUILDER OF LENDING:" + this.book.getTitle());
            //System.out.println("dawdadwawdawdawdawdawdawdadwad");
            //System.out.println("READER DETAILS: " + readerDetails.getReaderNumber());
            this.readerDetails = Objects.requireNonNull(readerDetails);
            //System.out.println("READER DETAILS");
            //System.out.println("Reader:" + this.readerDetails.getReaderNumber());
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Null objects passed to lending");
        }
        this.lendingNumber = lendingNumber;
        this.startDate = startDate;
        this.limitDate = limitDate;
        this.returnedDate = returnedDate;
        this.fineValuePerDayInCents = fineValuePerDayInCents;
        //System.out.println("Start Date: " + this.startDate);
        //System.out.println("Limit Date: " + this.limitDate);
        //System.out.println("Returned Date: " + this.returnedDate);
        setDaysUntilReturn();
        setDaysOverdue();
        setGenId(genId);
    }


    public int getDaysDelayed() {
        if (this.returnedDate != null) {
            return Math.max((int) ChronoUnit.DAYS.between(this.limitDate, this.returnedDate), 0);
        } else {
            return Math.max((int) ChronoUnit.DAYS.between(this.limitDate, LocalDate.now()), 0);
        }
    }

    private void setDaysUntilReturn() {
        int daysUntilReturn = (int) ChronoUnit.DAYS.between(LocalDate.now(), this.limitDate);
        this.daysUntilReturn = (this.returnedDate != null || daysUntilReturn < 0) ? null : daysUntilReturn;
    }

    private void setDaysOverdue() {
        int days = getDaysDelayed();
        this.daysOverdue = (days > 0) ? days : null;
    }

    public Optional<Integer> getDaysUntilReturn() {
        setDaysUntilReturn();
        return Optional.ofNullable(daysUntilReturn);
    }

    public Optional<Integer> getDaysOverdue() {
        setDaysOverdue();
        return Optional.ofNullable(daysOverdue);
    }

    public Optional<Integer> getFineValueInCents() {
        Optional<Integer> fineValueInCents = Optional.empty();
        int days = getDaysDelayed();
        if (days > 0) {
            fineValueInCents = Optional.of(fineValuePerDayInCents * days);
        }
        return fineValueInCents;
    }

    public String getTitle() {
        return this.book.getTitle().toString();
    }

    public String getLendingNumber() {
        return this.lendingNumber.toString();
    }



    /** Factory method for bootstrapping. */
    public static Lending newBootstrappingLending(Book book, ReaderDetails readerDetails,
                                                  int year, int seq, LocalDate startDate,
                                                  LocalDate returnedDate, int lendingDuration,
                                                  int fineValuePerDayInCents) {
        Lending lending = new Lending();

        try {
            lending.book = Objects.requireNonNull(book);
            //System.out.println("Book: " + book.getTitle());
            lending.readerDetails = Objects.requireNonNull(readerDetails);
            //System.out.println("Reader: " + readerDetails.getReaderNumber());
        } catch (NullPointerException e) {
            //System.out.println("Null objects passed to lending");
            throw new IllegalArgumentException("Null objects passed to lending");
        }
        //System.out.println("Creating lending");
        lending.lendingNumber = new LendingNumber(year, seq);
        //System.out.println("Lending number: " + lending.lendingNumber);
        lending.startDate = startDate;
        //System.out.println("Start date: " + startDate);
        lending.limitDate = startDate.plusDays(lendingDuration);
        //System.out.println("Limit date: " + lending.limitDate);
        lending.fineValuePerDayInCents = fineValuePerDayInCents;
        //System.out.println("Fine value per day: " + fineValuePerDayInCents);
        lending.returnedDate = returnedDate;
        //System.out.println("Returned date: " + returnedDate);
        lending.setGenId(null);
        return lending;
    }

    public void setBook(Book book) {
        this.book=book;
    }

    public void applyPatch(Book b, ReaderDetails r, LocalDate returnedDate, LocalDate limitDate, LocalDate returnedDate1) {

        if (!Objects.equals(this.book, b)) {
            this.book = b;
        }
        if (!Objects.equals(this.readerDetails, r)) {
            this.readerDetails = r;
        }
        if (!Objects.equals(this.returnedDate, returnedDate)) {
            this.returnedDate = returnedDate;
        }
        if (!Objects.equals(this.limitDate, limitDate)) {
            this.limitDate = limitDate;
        }
        if (!Objects.equals(this.returnedDate, returnedDate1)) {
            this.returnedDate = returnedDate1;
        }

    }
}
