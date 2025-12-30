package pt.psoft.g1.psoftg1.lendingmanagement.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * The LendingNumber class handles business logic related to the identifier of a Lending.
 * It contains year and sequential number validation and string formatting rules.
 */
@NoArgsConstructor
public class LendingNumber {

    private  String lendingNumber;


    /**
     * Constructs a new LendingNumber object based on a year and a given sequential number.
     *
     * @param year       Year component of the LendingNumber
     * @param sequential Sequential component of the LendingNumber
     */
    public LendingNumber(int year, int sequential) {
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
    public LendingNumber(String lendingNumber) {
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
    public LendingNumber(int sequential) {
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
