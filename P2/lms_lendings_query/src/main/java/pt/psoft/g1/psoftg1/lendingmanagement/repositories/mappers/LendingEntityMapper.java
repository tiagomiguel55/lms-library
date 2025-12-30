package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.TitleEntity;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.LendingNumber;
import pt.psoft.g1.psoftg1.lendingmanagement.model.relational.LendingEntity;
import pt.psoft.g1.psoftg1.lendingmanagement.model.relational.LendingNumberEntity;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.model.relational.NameEntity;
import pt.psoft.g1.psoftg1.shared.model.relational.PhotoEntity;


@Mapper(componentModel = "spring")
public interface LendingEntityMapper {

    // Mapear de LendingEntity para Lending
    @Mapping(target = "readerValid", source = "readerValid")
    @Mapping(target = "lendingStatus", source = "lendingStatus")
    @Mapping(target = "bookValid", source = "bookValid")
    Lending sqlServerToModel(LendingEntity lendingEntity);

    // Mapear de Lending para LendingEntity

    @Mapping(target = "readerValid", source = "readerValid")
    @Mapping(target = "lendingStatus", source = "lendingStatus")
    @Mapping(target = "bookValid", source = "bookValid")
    LendingEntity modelToSqlServer(Lending lending);
    @Mapping(target = "lendingNumber", source = "value")
    LendingNumberEntity stringToLne (String value);
    @Mapping(target = "lendingNumber", source = "value")
    LendingNumber stringToLn (String value);



    ReaderDetails toModel(ReaderDetailsEntity readerDetails);

    ReaderDetailsEntity toEntity(ReaderDetails readerDetails);

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

    default String map(PhotoEntity value) {
        if (value == null) {
            return null;
        }
        return value.getPhotoFile(); // Exemplo para PhotoEntity
    }

    default String map(TitleEntity value) {
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

    default String map(NameEntity value) {
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



}
