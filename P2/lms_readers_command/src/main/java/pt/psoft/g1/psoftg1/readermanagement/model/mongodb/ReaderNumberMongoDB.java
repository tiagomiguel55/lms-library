package pt.psoft.g1.psoftg1.readermanagement.model.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDate;

@Document
public class ReaderNumberMongoDB implements Serializable {

    @Id
    private String id;

    @Field("reader_number")
    private String readerNumber;

    public ReaderNumberMongoDB(int year, int number) {
        this.readerNumber = year + "/" + number;
    }

    public ReaderNumberMongoDB(int number) {
        this.readerNumber = LocalDate.now().getYear() + "/" + number;
    }

    protected ReaderNumberMongoDB() {}

    public String toString() {
        return this.readerNumber;
    }



}
