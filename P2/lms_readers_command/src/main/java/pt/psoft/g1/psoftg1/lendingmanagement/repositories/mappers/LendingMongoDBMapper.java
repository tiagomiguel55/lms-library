package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.TitleMongoDB;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.LendingNumber;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.LendingMongoDB;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.LendingNumberMongoDB;
import pt.psoft.g1.psoftg1.readermanagement.model.BirthDate;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.mongodb.BirthDateMongoDB;
import pt.psoft.g1.psoftg1.readermanagement.model.mongodb.ReaderDetailsMongoDB;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.model.mongodb.NameMongoDB;
import pt.psoft.g1.psoftg1.shared.model.mongodb.PhotoMongoDB;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface LendingMongoDBMapper {

    // Mapear de LendingMongoDB para Lending
    @Mapping(target = "limitDate", source = "limitDate")
    @Mapping(target = "returnedDate", source = "returnedDate")
    @Mapping(target = "startDate", source = "startDate")
    Lending toDomain(LendingMongoDB lendingMongoDB);

    // Mapear de Lending para LendingEntity
    LendingMongoDB toMongoDB(Lending lending);

    @Mapping(target = "lendingNumber", source = "value")
    LendingNumberMongoDB stringToLnMDB (String value);
    @Mapping(target = "lendingNumber", source = "value")
    LendingNumber stringToLn (String value);

    @Mapping(target="gdpr", source="gdprConsent")
    @Mapping(target="marketing", source="marketingConsent")
    @Mapping(target="thirdParty", source="thirdPartySharingConsent")
    ReaderDetails toDomain(ReaderDetailsMongoDB readerDetailsMongoDB);

    @Mapping(target="gdpr", source="gdprConsent")
    @Mapping(target="marketing", source="marketingConsent")
    @Mapping(target="thirdParty", source="thirdPartySharingConsent")
    ReaderDetailsMongoDB toMongoDB(ReaderDetails readerDetails);

    default int map(String value) {
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.split("/")[1]); // Exemplo para ReaderNumberEntity
    }
    default String map(Photo value) {
        if (value == null) {
            return null;
        }
        return value.getPhotoFile(); // Exemplo para Photo
    }

    default String map(PhotoMongoDB value) {
        if (value == null) {
            return null;
        }
        return value.getPhotoFile(); // Exemplo para PhotoEntity
    }

    default String map(TitleMongoDB value) {
        if (value == null) {
            return null;
        }
        return value.getTitle(); // Exemplo para TitleEntity
    }

    default String map(Title value) {
        if (value == null) {
            return null;
        }
        return value.getTitle();  // Exemplo para Title
    }

    default String map(NameMongoDB value) {
        if (value == null) {
            return null;
        }
        return value.getName(); // Exemplo para NameEntity
    }

    default String map(Name value) {
        if (value == null) {
            return null;
        }
        return value.getName(); // Exemplo para LendingNumberEntity
    }

    default  String map(BirthDateMongoDB birthDateMongoDB){
        if (birthDateMongoDB == null){
            return null;
        }
        return birthDateMongoDB.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    default String map(BirthDate birthDate){
        if (birthDate == null){
            return null;
        }

        return birthDate.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    default String map(LendingNumberMongoDB lendingNumberMongoDB){
        if (lendingNumberMongoDB == null){
            return null;
        }
        return lendingNumberMongoDB.getLendingNumber();
    }

    default String map(LendingNumber lendingNumber){
        if (lendingNumber == null){
            return null;
        }
        return lendingNumber.getLendingNumber();
    }
}
