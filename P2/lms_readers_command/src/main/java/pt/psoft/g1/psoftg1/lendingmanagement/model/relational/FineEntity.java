package pt.psoft.g1.psoftg1.lendingmanagement.model.relational;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;

import java.util.Objects;

/**
 * The {@code FineEntity} class defines the data model for the Fine in the database.
 * It extends the {@link Fine} class and adds persistence logic through JPA.
 * */
@Getter
@Entity
@Table(name = "fine")
public class FineEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long pk;

    /** Fine value per day in cents is persisted but not updatable */
    @PositiveOrZero
    @Column(updatable = false)
    private int fineValuePerDayInCents;

    /** Fine value in Euro cents */
    @PositiveOrZero
    private int centsValue;

    @Setter
    @OneToOne(optional = false, orphanRemoval = true)
    @JoinColumn(name = "lending_pk", nullable = false, unique = true)
    private LendingEntity lendingEntity;

    /**
     * Constructs a new {@code Fine} object. Sets the current value of the fine,
     * as well as the fine value per day at the time of creation.
     *
     * @param lending transaction which generates this fine.
     */
    public FineEntity(LendingEntity lending) {
        if (lending.getDaysDelayed() <= 0) {
            throw new IllegalArgumentException("Lending is not overdue");
        }
        this.fineValuePerDayInCents = lending.getFineValuePerDayInCents();
        this.centsValue = fineValuePerDayInCents * lending.getDaysDelayed();
        this.lendingEntity = Objects.requireNonNull(lending);
    }

    /** Protected empty constructor for ORM only. */
    protected FineEntity() {
        this.fineValuePerDayInCents = 0;
    }


    /**
     * Recalculates the fine based on the current delayed days.
     */
    public void recalculateFine() {
        this.centsValue = fineValuePerDayInCents * lendingEntity.getDaysDelayed();
    }
}
