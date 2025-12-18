package pt.psoft.g1.psoftg1.lendingmanagement.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * The {@code Fine} class models a fine applied when a lending is past its due date.
 * <p>It stores its current value, and the associated {@code Lending}.
 * */
@Getter

public class Fine {


    private final int fineValuePerDayInCents;

    /** Fine value in Euro cents */

    private int centsValue;

    @Setter
    private Lending lending;

    /**
     * Constructs a new {@code Fine} object. Sets the current value of the fine,
     * as well as the fine value per day at the time of creation.
     *
     * @param lending transaction which generates this fine.
     */
    public Fine(Lending lending) {
        if (lending.getDaysDelayed() <= 0) {
            throw new IllegalArgumentException("Lending is not overdue");
        }
        this.fineValuePerDayInCents = lending.getFineValuePerDayInCents();
        this.centsValue = fineValuePerDayInCents * lending.getDaysDelayed();
        this.lending = Objects.requireNonNull(lending);
    }

    /** Protected empty constructor for ORM only. */
    protected Fine() {
        this.fineValuePerDayInCents = 0;
    }


    /**
     * Recalculates the fine based on the current delayed days.
     */
    public void recalculateFine() {
        this.centsValue = fineValuePerDayInCents * lending.getDaysDelayed();
    }


}
