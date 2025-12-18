package pt.psoft.g1.psoftg1.usermanagement.repositories.mongodb;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.usermanagement.model.mongodb.UserMongoDB;

import java.util.List;
import java.util.Optional;

@Profile("mongodb")
public interface UserRepositoryMongoDB extends MongoRepository<UserMongoDB, String> {

    @Query("{ 'id': ?0 }")
    Optional<UserMongoDB> findById(Long id);

    @Query("{ 'username': ?0 }")
    Optional<UserMongoDB> findByUsername(String username);

    // Query to find by if it contains the username
    @Query("{ 'name.name': { $regex: ?0 } }")
    List<UserMongoDB> findByNameName(String name);



}
