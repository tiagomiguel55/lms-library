package pt.psoft.g1.psoftg1.readermanagement.repositories.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.TitleEntity;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.model.relational.NameEntity;
import pt.psoft.g1.psoftg1.shared.model.relational.PhotoEntity;

@Mapper(componentModel = "spring")
public interface ReaderEntityMapper {

    @Mapping(target = "readerNumber", source = "readerNumber", qualifiedByName = "map1")
    ReaderDetails toModel(ReaderDetailsEntity readerDetails);
    @Mapping(target = "readerNumber", source = "readerNumber", qualifiedByName = "map1")
    ReaderDetailsEntity toEntity(ReaderDetails readerDetails);

    @Named("map1")
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

    default String map(PhotoEntity photoEntity) {
        if (photoEntity == null) {
            return null;
        }
        return photoEntity.getPhotoFile();
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


}
