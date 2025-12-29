package pt.psoft.g1.psoftg1.readermanagement.repositories.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.mongodb.GenreMongoDB;
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
public interface ReaderMapperMongoDB {

    @Mapping(target = "readerNumber", source = "readerNumber", qualifiedByName = "map")
    @Mapping(target="gdpr", source="gdprConsent")
    @Mapping(target="marketing", source="marketingConsent")
    @Mapping(target="thirdParty", source="thirdPartySharingConsent")
    ReaderDetails toDomain(ReaderDetailsMongoDB readerDetails);

    @Mapping(target = "readerNumber", source = "readerNumber", qualifiedByName = "map")
    @Mapping(target="gdpr", source="gdprConsent")
    @Mapping(target="marketing", source="marketingConsent")
    @Mapping(target="thirdParty", source="thirdPartySharingConsent")
    ReaderDetailsMongoDB toMongoDB(ReaderDetails readerDetails);

    @Named("map")
    default int map(String readerNumber) {
        if (readerNumber == null) {
            return 0;
        }
        String readerNumberString = readerNumber.split("/")[1];
        return Integer.parseInt(readerNumberString);
    }


    default String map(Genre Value){
        if (Value == null){
            return null;
        }
        return Value.getGenre(); // Exemplo para Genre
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

    default String map(GenreMongoDB value){
        if (value == null){
            return null;
        }
        return value.getGenre(); // Exemplo para Genre
    }

    default String map(Photo photo) {
        if (photo == null) {
            return null;
        }
        return photo.getPhotoFile();
    }

    default String map(PhotoMongoDB photoMongoDB) {
        if (photoMongoDB == null) {
            return null;
        }
        return photoMongoDB.getPhotoFile();
    }


    default String map(Name value) {
        if (value == null) {
            return null;
        }
        return value.getName(); // Exemplo para BioEntity
    }

    default String map(NameMongoDB value) {
        if (value == null) {
            return null;
        }
        return value.getName(); // Exemplo para BioEntity
    }
}
