package pt.psoft.g1.psoftg1.readermanagement.repositories.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.TitleMongoDB;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.mongodb.ReaderDetailsMongoDB;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.model.mongodb.NameMongoDB;
import pt.psoft.g1.psoftg1.shared.model.mongodb.PhotoMongoDB;

@Mapper(componentModel = "spring")
public interface ReaderMapperMongoDB {

    @Mapping(target = "readerNumber", source = "readerNumber", qualifiedByName = "map")
    ReaderDetails toDomain(ReaderDetailsMongoDB readerDetails);

    @Mapping(target = "readerNumber", source = "readerNumber", qualifiedByName = "map")
    ReaderDetailsMongoDB toMongoDB(ReaderDetails readerDetails);

    @Named("map")
    default int map(String readerNumber) {
        if (readerNumber == null) {
            return 0;
        }
        String readerNumberString = readerNumber.split("/")[1];
        return Integer.parseInt(readerNumberString);
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
