package pt.psoft.g1.psoftg1.lendingmanagement.model.relational;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.StaleObjectStateException;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.BookEntity;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@code LendingEntity} class defines the persistence model for the Lending system.
 * It extends the {@link Lending} class and adds JPA-specific annotations for database operations.
 */
@Getter
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"LENDING_NUMBER"})})
@NoArgsConstructor
public class LendingEntity {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long pk;

    @Embedded
    private LendingNumberEntity lendingNumberEntity; // Reference to the embedded LendingNumberEntity

    @Setter
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional =false)
    private BookEntity book;

    @Setter
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private ReaderDetailsEntity readerDetails;


    @NotNull
    @Column(nullable = false, updatable = false)
    private LocalDate startDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate limitDate;

    @Temporal(TemporalType.DATE)
    private LocalDate returnedDate;

    @Version
    private long version;

    @Size(min = 0, max = 1024)
    @Column(length = 1024)
    private String commentary = null;

    @Transient
    private int fineValuePerDayInCents;
    @Transient
    private Integer daysUntilReturn;


    private Integer daysOverdue;

    @Setter
    @Column
    private boolean readerValid;

    @Setter
    @Column
    private String lendingStatus;

    @Setter
    @Column
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
    public LendingEntity(long pk,BookEntity book, ReaderDetailsEntity readerDetails, LendingNumberEntity lendingNumber, LocalDate startDate, LocalDate limitDate, LocalDate returnedDate, int fineValuePerDayInCents, boolean readerValid, String lendingStatus, boolean bookValid, long version, String commentary) {
        try {
            this.book = Objects.requireNonNull(book);
            this.readerDetails = Objects.requireNonNull(readerDetails);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Null objects passed to lending");
        }
        this.lendingNumberEntity = lendingNumber;
        this.startDate = startDate;
        this.limitDate = limitDate;
        this.returnedDate = returnedDate;
        this.fineValuePerDayInCents = fineValuePerDayInCents;
        setDaysUntilReturn();
        setDaysOverdue();
        setLendingStatus(lendingStatus);
        setReaderValid(readerValid);
        setBookValid(bookValid);
        this.version = version;
        this.commentary = commentary;
        this.pk = pk;
    }

    public void setReturned(final long desiredVersion, final String commentary) {
        if (this.returnedDate != null) {
            throw new IllegalArgumentException("Book has already been returned!");
        }

        // Check current version
        if (this.version != desiredVersion) {
            throw new StaleObjectStateException("Object was already modified by another user", this.lendingNumberEntity);
        }

        if (commentary != null) {
            this.commentary = commentary;
        }

        this.returnedDate = LocalDate.now();
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
        return this.lendingNumberEntity.toString();
    }




}
