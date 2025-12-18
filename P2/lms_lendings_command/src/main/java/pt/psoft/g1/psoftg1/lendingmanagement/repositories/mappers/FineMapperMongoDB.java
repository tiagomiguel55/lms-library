package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.TitleEntity;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.LendingNumber;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.FineMongoDB;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.LendingNumberMongoDB;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.model.mongodb.NameMongoDB;
import pt.psoft.g1.psoftg1.shared.model.mongodb.PhotoMongoDB;

@Mapper(componentModel = "spring")
public interface FineMapperMongoDB {

    Fine toDomain(FineMongoDB fineMongoDB);

    FineMongoDB toMongoDB(Fine fine);

    @Mapping(target = "lendingNumber", source = "value")
    LendingNumberMongoDB stringToLne (String value);
    @Mapping(target = "lendingNumber", source = "value")
    LendingNumber stringToLn (String value);

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


}
