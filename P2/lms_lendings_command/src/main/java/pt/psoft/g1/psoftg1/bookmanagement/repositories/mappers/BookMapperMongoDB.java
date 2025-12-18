package pt.psoft.g1.psoftg1.bookmanagement.repositories.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.TitleMongoDB;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.model.mongodb.PhotoMongoDB;

@Mapper(componentModel = "spring")
public interface BookMapperMongoDB {

    @Mapping(target = "version", source = "bookMongoDB.version")
    Book toDomain(BookMongoDB bookMongoDB);


    BookMongoDB toMongoDB(Book book);



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
}
