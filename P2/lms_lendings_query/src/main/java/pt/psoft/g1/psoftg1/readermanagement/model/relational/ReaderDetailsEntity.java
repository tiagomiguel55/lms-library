package pt.psoft.g1.psoftg1.readermanagement.model.relational;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pt.psoft.g1.psoftg1.shared.model.relational.EntityWithPhotoEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.ReaderEntity;

@Entity
@Table(name = "READER_DETAILS")
public class ReaderDetailsEntity extends EntityWithPhotoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long pk;

    @Getter
    @Setter
    @OneToOne
    private ReaderEntity reader;

    private ReaderNumberEntity readerNumber;


    @Version
    @Getter
    private Long version;



    public ReaderDetailsEntity(int readerNumber, ReaderEntity reader) {
        if(reader == null ) {
            throw new IllegalArgumentException("Provided argument resolves to null object");
        }

        setReader(reader);
        setReaderNumber(new ReaderNumberEntity(readerNumber));

    }


    private void setReaderNumber(ReaderNumberEntity readerNumber) {
        if(readerNumber != null) {
            this.readerNumber = readerNumber;
        }
    }


    public String getReaderNumber(){
        return this.readerNumber.toString();
    }


    protected ReaderDetailsEntity() {
        // for ORM only
    }
}
