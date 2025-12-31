package pt.psoft.g1.psoftg1.readermanagement.repositories.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;


import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.relational.GenreEntity;
import pt.psoft.g1.psoftg1.readermanagement.model.BirthDate;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

import pt.psoft.g1.psoftg1.readermanagement.model.relational.BirthDateEntity;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;

import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.model.relational.NameEntity;
import pt.psoft.g1.psoftg1.shared.model.relational.PhotoEntity;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface ReaderEntityMapper {

    @Mapping(target = "readerNumber", source = "readerNumber", qualifiedByName = "map1")
    @Mapping(target="birthDate", source="birthDate", qualifiedByName = "mapbde")
    @Mapping(target="gdpr", source="gdprConsent")
    @Mapping(target="marketing", source="marketingConsent")
    @Mapping(target="thirdParty", source="thirdPartySharingConsent")
    @Mapping(target="version", source="version")
    @Mapping(target="reader.version", source="reader.version")
    @Mapping(target="reader.name", source="reader.name", qualifiedByName = "nameEntityToString")
    ReaderDetails toModel(ReaderDetailsEntity readerDetails);
    @Mapping(target = "readerNumber", source = "readerNumber", qualifiedByName = "map1")
    @Mapping(target="birthDate", source="birthDate", qualifiedByName = "mapbd")
    @Mapping(target="gdpr", source="gdprConsent")
    @Mapping(target="marketing", source="marketingConsent")
    @Mapping(target="thirdParty", source="thirdPartySharingConsent")
    @Mapping(target="version", source="version")
    @Mapping(target="reader.version", source="reader.version")
    @Mapping(target="reader.name", source="reader.name", qualifiedByName = "nameToNameEntity")
    ReaderDetailsEntity toEntity(ReaderDetails readerDetails);

    @Named("map1")
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
    @Named("mapbde")
    default  String map(BirthDateEntity birthDateEntity){
        if (birthDateEntity == null){
            return null;
        }
        return birthDateEntity.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Named("mapbd")
    default String map(BirthDate birthDate){
        if (birthDate == null){
            return null;
        }

        return birthDate.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    default String map(GenreEntity Value){
        if (Value == null){
            return null;
        }
        return Value.getGenre(); // Exemplo para Genre
    }

    default String map(Photo photo) {
        if (photo == null) {
            return null;
        }
        return photo.getPhotoFile();
    }

    default String map(PhotoEntity photoEntity) {
        if (photoEntity == null) {
            return null;
        }
        return photoEntity.getPhotoFile();
    }



    default String map(Name value) {
        if (value == null) {
            return null;
        }
        return value.getName(); // Exemplo para BioEntity
    }

    default String map(NameEntity value) {
        if (value == null) {
            return null;
        }
        return value.getName(); // Exemplo para BioEntity
    }

    @Named("nameToNameEntity")
    default NameEntity nameToNameEntity(Name name) {
        if (name == null) {
            return null;
        }
        return new NameEntity(name.getName());
    }

    @Named("nameEntityToString")
    default String nameEntityToString(NameEntity nameEntity) {
        if (nameEntity == null) {
            return null;
        }
        return nameEntity.getName();
    }


}
