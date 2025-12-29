package pt.psoft.g1.psoftg1.bookmanagement.repositories.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.TitleMongoDB;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.mongodb.GenreMongoDB;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.model.mongodb.PhotoMongoDB;

@Mapper(componentModel = "spring")
public interface BookMapperMongoDB {

    @Mapping(target = "version", source = "bookMongoDB.version")
    Book toDomain(BookMongoDB bookMongoDB);


    BookMongoDB toMongoDB(Book book);

    default String map(Genre Value){
        System.out.println("Genre: " + Value);
        if (Value == null){
            System.out.println("Genre is null");
            return null;
        }
        System.out.println("Genre is not null: " + Value.getGenre());
        return Value.getGenre(); // Exemplo para Genre
    }

    default String map(GenreMongoDB Value){
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
