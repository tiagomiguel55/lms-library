package pt.psoft.g1.psoftg1.readermanagement.model.mongodb;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

@Document(collection = "reader_details")
@EnableMongoAuditing
public class ReaderDetailsMongoDB extends EntityWithPhoto {

    @Id
    @Getter
    private String id;

    @Getter
    @Setter
    private Reader reader;

    @Getter
    private ReaderNumberMongoDB readerNumber;


    @Getter
    @Setter
    @org.springframework.data.annotation.Version
    private Long version;



    public ReaderDetailsMongoDB(int readerNumber, Reader reader) {
        if(reader == null ) {
            throw new IllegalArgumentException("Provided argument resolves to null object");
        }


        setReader(reader);
        setReaderNumber(new ReaderNumberMongoDB(readerNumber));
    }


    private void setReaderNumber(ReaderNumberMongoDB readerNumber) {
        if(readerNumber != null) {
            this.readerNumber = readerNumber;
        }
    }



    public String getReaderNumber(){
        return this.readerNumber.toString();
    }

    protected ReaderDetailsMongoDB() {
        // for ORM only
    }
}

