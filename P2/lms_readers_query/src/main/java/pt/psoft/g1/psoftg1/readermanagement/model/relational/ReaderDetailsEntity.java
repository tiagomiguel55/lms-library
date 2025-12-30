package pt.psoft.g1.psoftg1.readermanagement.model.relational;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.relational.GenreEntity;
import pt.psoft.g1.psoftg1.readermanagement.model.BirthDate;
import pt.psoft.g1.psoftg1.readermanagement.model.PhoneNumber;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderNumber;
import pt.psoft.g1.psoftg1.readermanagement.services.UpdateReaderRequest;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;
import pt.psoft.g1.psoftg1.shared.model.relational.EntityWithPhotoEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.ReaderEntity;

import java.nio.file.InvalidPathException;
import java.util.List;

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

    @Embedded
    @Getter
    private BirthDateEntity birthDate;

    @Embedded
    private PhoneNumberEntity phoneNumber;

    @Setter
    @Getter
    @Basic
    private boolean gdprConsent;

    @Setter
    @Basic
    @Getter
    private boolean marketingConsent;

    @Setter
    @Basic
    @Getter
    private boolean thirdPartySharingConsent;

    @Version
    @Getter
    @Setter
    private Long version;

    @Getter
    @Setter
    @ManyToMany(cascade = CascadeType.ALL)
    private List<GenreEntity> interestList;

    public ReaderDetailsEntity(int readerNumber, ReaderEntity reader, String birthDate, String phoneNumber, boolean gdpr, boolean marketing, boolean thirdParty, String photoURI, List<GenreEntity> interestList) {
        if(reader == null || phoneNumber == null) {
            throw new IllegalArgumentException("Provided argument resolves to null object");
        }

        if(!gdpr) {
            throw new IllegalArgumentException("Readers must agree with the GDPR rules");
        }

        setReader(reader);
        setReaderNumber(new ReaderNumberEntity(readerNumber));
        setPhoneNumber(new PhoneNumberEntity(phoneNumber));
        setBirthDate(new BirthDateEntity(birthDate));
        //By the client specifications, gdpr can only have the value of true. A setter will be created anyways in case we have accept no gdpr consent later on the project
        setGdprConsent(true);

        setPhotoInternal(photoURI);
        setMarketingConsent(marketing);
        setThirdPartySharingConsent(thirdParty);
        setInterestList(interestList);
    }

    private void setPhoneNumber(PhoneNumberEntity number) {
        if(number != null) {
            this.phoneNumber = number;
        }
    }

    private void setReaderNumber(ReaderNumberEntity readerNumber) {
        if(readerNumber != null) {
            this.readerNumber = readerNumber;
        }
    }

    private void setBirthDate(BirthDateEntity date) {
        if(date != null) {
            this.birthDate = date;
        }
    }

    public void applyPatch(final long currentVersion, final UpdateReaderRequest request, String photoURI, List<GenreEntity> interestList) {
        if(currentVersion != this.version) {
            throw new ConflictException("Provided version does not match latest version of this object");
        }

        String birthDate = request.getBirthDate();
        String phoneNumber = request.getPhoneNumber();
        boolean marketing = request.getMarketing();
        boolean thirdParty = request.getThirdParty();
        String fullName = request.getFullName();
        String username = request.getUsername();
        String password = request.getPassword();

        if(username != null) {
            this.reader.setUsername(username);
        }

        if(password != null) {
            this.reader.setPassword(password);
        }

        if(fullName != null) {
            this.reader.setName(fullName);
        }

        if(birthDate != null) {
            setBirthDate(new BirthDateEntity(birthDate));
        }

        if(phoneNumber != null) {
            setPhoneNumber(new PhoneNumberEntity(phoneNumber));
        }

        if(marketing != this.marketingConsent) {
            setMarketingConsent(marketing);
        }

        if(thirdParty != this.thirdPartySharingConsent) {
            setThirdPartySharingConsent(thirdParty);
        }

        if(photoURI != null) {
            try {
                setPhotoInternal(photoURI);
            } catch(InvalidPathException ignored) {}
        }

        if(interestList != null) {
            this.interestList = interestList;
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


    public String getPhoneNumber() { return this.phoneNumber.toString();}

    protected ReaderDetailsEntity() {
        // for ORM only
    }
}
