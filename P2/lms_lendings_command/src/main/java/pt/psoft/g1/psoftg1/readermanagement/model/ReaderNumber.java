package pt.psoft.g1.psoftg1.readermanagement.model;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;



@Getter

public class ReaderNumber implements Serializable {

    private String readerNumber;

    public ReaderNumber(int year, int number) {
        this.readerNumber = year + "/" + number;
    }

    public ReaderNumber(int number) {
        this.readerNumber = LocalDate.now().getYear() + "/" + number;
    }

    protected ReaderNumber() {}

    public String toString() {
        return this.readerNumber;
    }


}
