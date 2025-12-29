package pt.psoft.g1.psoftg1.readermanagement.model.relational;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;


@Embeddable
public class ReaderNumberEntity implements Serializable {
    @Column(name = "READER_NUMBER")
    private String readerNumber;

    public ReaderNumberEntity(int year, int number) {
        this.readerNumber = year + "/" + number;
    }

    public ReaderNumberEntity(int number) {
        this.readerNumber = LocalDate.now().getYear() + "/" + number;
    }

    protected ReaderNumberEntity() {}

    public String toString() {
        return this.readerNumber;
    }
}
