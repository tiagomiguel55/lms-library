package pt.psoft.g1.psoftg1.bookmanagement.repositories.mongodb;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;

import java.util.Optional;
@Profile("mongodb")
public interface BookRepositoryMongoDB extends MongoRepository<BookMongoDB, String> {

    @Query("{ 'isbn.isbn': ?0 }")
    Optional<BookMongoDB> findByIsbn(String isbn);




}
