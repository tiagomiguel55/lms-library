package pt.psoft.g1.psoftg1.genremanagement.repositories.mongodb;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.genremanagement.model.mongodb.GenreMongoDB;

import java.util.Optional;

@Profile("mongodb")
public interface GenreRepositoryMongoDB extends MongoRepository<GenreMongoDB, String> {

    @Query("{ 'genre': ?0 }")
    Optional<GenreMongoDB> findByGenre(String genreName);


}
