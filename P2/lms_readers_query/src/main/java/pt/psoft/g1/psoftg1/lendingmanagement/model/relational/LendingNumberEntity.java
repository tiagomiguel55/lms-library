package pt.psoft.g1.psoftg1.lendingmanagement.model.relational;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * The LendingNumberEntity class is the data model responsible for persisting LendingNumber in the database.
 * It extends the LendingNumber model, inheriting the business logic while adding database mapping annotations.
 */
@Embeddable
@NoArgsConstructor
public class LendingNumberEntity  {

    @Column(name = "LENDING_NUMBER", length = 32)
    @NotNull
    @NotBlank
    @Size(min = 6, max = 32)
    private String lendingNumber;


    /**
     * Default constructor for JPA/ORM use.
     * Required for ORM frameworks.
     */
    public LendingNumberEntity(int year, int sequential) {
        if (year < 1970 || LocalDate.now().getYear() < year)
            throw new IllegalArgumentException("Invalid year component");
        if (sequential < 0)
            throw new IllegalArgumentException("Sequential component cannot be negative");
        this.lendingNumber = year + "/" + sequential;
    }

    /**
     * Constructs a new LendingNumber object based on a string.
     * Initialization may fail if the format is not as expected.
     *
     * @param lendingNumber String containing the lending number.
     */
    @Builder
    public LendingNumberEntity(String lendingNumber) {
        if (lendingNumber == null)
            throw new IllegalArgumentException("Lending number cannot be null");

        int year, sequential;
        try {
            year = Integer.parseInt(lendingNumber.substring(0, 4));
            sequential = Integer.parseInt(lendingNumber.substring(5));
            if (lendingNumber.charAt(4) != '/')
                throw new IllegalArgumentException("Lending number has wrong format. It should be \"{year}/{sequential}\"");
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Lending number has wrong format. It should be \"{year}/{sequential}\"");
        }
        this.lendingNumber = year + "/" + sequential;
    }

    /**
     * Constructs a new LendingNumber object based on a given sequential number.
     * The year is automatically set to the current year.
     *
     * @param sequential Sequential component of the LendingNumber
     */
    public LendingNumberEntity(int sequential) {
        if (sequential < 0)
            throw new IllegalArgumentException("Sequential component cannot be negative");
        this.lendingNumber = LocalDate.now().getYear() + "/" + sequential;
    }

    public String getLendingNumber() {
        return lendingNumber;
    }

    @Override
    public String toString() {
        return this.lendingNumber;
    }
}
