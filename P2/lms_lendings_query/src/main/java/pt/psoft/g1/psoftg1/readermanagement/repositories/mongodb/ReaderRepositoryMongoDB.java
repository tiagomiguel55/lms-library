package pt.psoft.g1.psoftg1.readermanagement.repositories.mongodb;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.readermanagement.model.mongodb.ReaderDetailsMongoDB;

import java.util.Optional;

@Profile("mongodb")
public interface ReaderRepositoryMongoDB extends MongoRepository<ReaderDetailsMongoDB, String> {

    @Query("{ 'readerNumber.readerNumber': ?0 }")
    Optional<ReaderDetailsMongoDB> findByReaderNumber(String readerNumber);

    @Query("{ 'reader.username': ?0 }")
    Optional<ReaderDetailsMongoDB> findByUsername(String username);

}
