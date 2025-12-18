package pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;

import java.util.Objects;

@Document(collection = "fines")
public class FineMongoDB {

    @Id
    @Getter
    private String id;

    @Field("fineValuePerDayInCents")
    private int fineValuePerDayInCents;

    @Field("centsValue")
    private int centsValue;

    @Setter
    @Field("lending")
    private LendingMongoDB lendingMongoDB;

    /**
     * Constructs a new {@code Fine} object. Sets the current value of the fine,
     * as well as the fine value per day at the time of creation.
     *
     * @param lending transaction which generates this fine.
     */

    public FineMongoDB(LendingMongoDB lending){
        if(lending.getDaysDelayed() <= 0){
            throw new IllegalArgumentException("Lending is not overdue");
        }
        this.fineValuePerDayInCents = lending.getFineValuePerDayInCents();
        this.centsValue = fineValuePerDayInCents * lending.getDaysDelayed();
        this.lendingMongoDB = Objects.requireNonNull(lending);
    }

    /** Protected empty constructor for ORM only. */
    protected FineMongoDB() {
        this.fineValuePerDayInCents = 0;
    }

    /**
     * Recalculates the fine based on the current delayed days.
     */
    public void recalculateFine() {
        this.centsValue = fineValuePerDayInCents * lendingMongoDB.getDaysDelayed();
    }

}
