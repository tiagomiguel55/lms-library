package pt.psoft.g1.psoftg1.bookmanagement.repositories.mongodb;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;

import java.util.List;
import java.util.Optional;
@Profile("mongodb")
public interface BookRepositoryMongoDB extends MongoRepository<BookMongoDB, String> {

    @Query("{ 'isbn.isbn': ?0 }")
    Optional<BookMongoDB> findByIsbn(String isbn);

    @Query("{ 'genre.genre': { $regex: ?0, $options: 'i' } }") // Case-insensitive regex search
    List<BookMongoDB> findByGenre(String genre);

    @Query("{ 'title.title': { $regex: ?0, $options: 'i' } }") // Case-insensitive regex search
    List<BookMongoDB> findByTitle(String title);

    @Query(value = "{ 'authors.name.name': { $regex: ?0, $options: 'i' } }") // Case-insensitive regex search
    List<BookMongoDB> findByAuthorName(String authorName);

    @Query(value = "{ 'authors.authorNumber': ?0 }")
    List<BookMongoDB> findBooksByAuthorNumber(String authorNumber);

    // This method would need a custom implementation to count loans and return a paginated result


}
