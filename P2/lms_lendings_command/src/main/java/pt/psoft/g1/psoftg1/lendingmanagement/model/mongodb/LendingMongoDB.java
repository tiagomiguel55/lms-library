package pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.StaleObjectStateException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;
import pt.psoft.g1.psoftg1.readermanagement.model.mongodb.ReaderDetailsMongoDB;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

@Document(collection = "lendings")
@EnableMongoAuditing
public class LendingMongoDB {

    @Id
    @Getter
    @Setter
    private String id;

    @Field("lending_number")
    private LendingNumberMongoDB lendingNumber;

    @Field("genId")
    @Getter
    @Setter
    private String genId;

    @Field("book")
    @Getter
    @Setter
    private BookMongoDB book;

    @Field("reader_details")
    @Getter
    private ReaderDetailsMongoDB readerDetails;

    @Field("start_date")
    @Getter
    private LocalDate startDate;

    @Field("limit_date")
    @Getter
    private LocalDate limitDate;

    @Field("returned_date")
    @Getter
    private LocalDate returnedDate;

    @Field("version")
    @Version
    @Getter
    @Setter
    private long version;

    @Field("commentary")
    private String commentary = null;

    @Field("fineValuePerDayInCents")
    @Getter
    private int fineValuePerDayInCents;

    @Field("days_untul_return")
    private Integer daysUntilReturn;

    @Field("days_overdue")
    private Integer daysOverdue;

    @Field("reader_valid")
    @Setter
    @Getter
    private  boolean readerValid;

    @Field("lending_status")
    @Setter
    @Getter
    private String lendingStatus;

    @Field("book_valid")
    @Setter
    @Getter
    private boolean bookValid;

    /**
     * Constructs a new {@code Lending} object.
     *
     * @param book             {@code Book} object, which should be retrieved from the database.
     * @param readerDetails    {@code Reader} object, which should be retrieved from the database.
     * @param fineValuePerDayInCents fine value per day in cents.
     * @throws NullPointerException if any of the arguments is {@code null}
     */

    @Builder
    public LendingMongoDB(String id, BookMongoDB book, ReaderDetailsMongoDB readerDetails, LendingNumberMongoDB lendingNumber, LocalDate startDate, LocalDate limitDate, LocalDate returnedDate, int fineValuePerDayInCents, String genId, boolean readerValid, boolean bookValid, String lendingStatus, long version, String commentary) {
        try {
            this.book = Objects.requireNonNull(book);
            this.readerDetails = Objects.requireNonNull(readerDetails);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Null objects passed to lending");
        }
        this.id = id;
        this.lendingNumber = lendingNumber;
        this.startDate = startDate;
        this.limitDate = limitDate;
        this.returnedDate = returnedDate;
        this.fineValuePerDayInCents = fineValuePerDayInCents;
        this.readerValid = readerValid;
        this.bookValid = bookValid;
        this.lendingStatus = lendingStatus;
        this.version = version;
        this.commentary = commentary;
        setDaysUntilReturn();
        setDaysOverdue();
        setGenId(genId);
    }

    public void setReturned(final long desiredVersion, final String comment) {
        if (this.returnedDate != null) {
            throw new IllegalArgumentException("Book has already been returned!");
        }

        // Check current version
        if (this.version != desiredVersion) {
            throw new StaleObjectStateException("Object was already modified by another user", this.lendingNumber);
        }

        if (comment != null) {
            this.commentary = comment;
        }

        this.returnedDate = LocalDate.now();
        this.lendingStatus = "DELIVERED";
    }

    public int getDaysDelayed() {
        if (this.returnedDate != null) {
            return Math.max((int) ChronoUnit.DAYS.between(this.limitDate, this.returnedDate), 0);
        } else {
            return Math.max((int) ChronoUnit.DAYS.between(this.limitDate, LocalDate.now()), 0);
        }
    }

    private void setDaysUntilReturn() {
        int daysUntilReturn = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), this.limitDate);
        this.daysUntilReturn = (this.returnedDate != null || daysUntilReturn < 0) ? null : daysUntilReturn;
    }

    private void setDaysOverdue() {
        int days = getDaysDelayed();
        this.daysUntilReturn = (days > 0) ? days : null;
    }

    public Optional<Integer> getDaysUntilReturn() {
        setDaysUntilReturn();
        return Optional.ofNullable(this.daysUntilReturn);
    }

    public Optional<Integer> getDaysOverdue() {
        setDaysOverdue();
        return Optional.ofNullable(this.daysOverdue);
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


    public String getBookId() {
        return this.book.getBookId();
    }
}
