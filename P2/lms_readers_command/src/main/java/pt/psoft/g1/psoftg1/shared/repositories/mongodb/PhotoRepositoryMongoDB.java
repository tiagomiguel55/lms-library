package pt.psoft.g1.psoftg1.shared.repositories.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.shared.model.mongodb.PhotoMongoDB;

import java.util.Optional;

public interface PhotoRepositoryMongoDB extends MongoRepository<PhotoMongoDB, String> {

    @Query("{ 'id': ?0 }")
    Optional<PhotoMongoDB> findById(String photoId);

    @Query("{ 'photoFile': ?0 }")
    void deleteByPhotoFile(String photoFile);
}
