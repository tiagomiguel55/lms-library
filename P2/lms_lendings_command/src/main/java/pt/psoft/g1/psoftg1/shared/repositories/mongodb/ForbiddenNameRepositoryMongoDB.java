package pt.psoft.g1.psoftg1.shared.repositories.mongodb;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.model.mongodb.ForbiddenNameMongoDB;

import java.util.List;
import java.util.Optional;

public interface ForbiddenNameRepositoryMongoDB extends MongoRepository<ForbiddenNameMongoDB, String> {

    @Query("{ 'forbiddenName': ?0 }")
    List<ForbiddenNameMongoDB> findByForbiddenNameIsContained(String pat);

    @Query("{ 'forbiddenName': ?0 }")
    Optional<ForbiddenNameMongoDB> findByForbiddenName(String forbiddenName);

    @Query("{ 'forbiddenName': ?0 }")
    int deleteForbiddenName(String forbiddenName);
}
