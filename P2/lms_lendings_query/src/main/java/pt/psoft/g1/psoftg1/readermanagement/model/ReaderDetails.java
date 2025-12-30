package pt.psoft.g1.psoftg1.readermanagement.model;

import lombok.Getter;
import lombok.Setter;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;



public class ReaderDetails extends EntityWithPhoto {

    @Getter
    @Setter
    private Reader reader;

    private ReaderNumber readerNumber;


    @Getter
    @Setter
    private Long version;


    public ReaderDetails(int readerNumber, Reader reader) {
        if(reader == null ) {
            throw new IllegalArgumentException("Provided argument resolves to null object");
        }

        setReader(reader);
        setReaderNumber(new ReaderNumber(readerNumber));
    }


    private void setReaderNumber(ReaderNumber readerNumber) {
        if(readerNumber != null) {
            this.readerNumber = readerNumber;
        }
    }


    public void removePhoto(long desiredVersion) {
        if(desiredVersion != this.version) {
            throw new ConflictException("Provided version does not match latest version of this object");
        }

        setPhotoInternal(null);
    }

    public String getReaderNumber(){
        return this.readerNumber.toString();
    }


    protected ReaderDetails() {
        // for ORM only
    }
}
