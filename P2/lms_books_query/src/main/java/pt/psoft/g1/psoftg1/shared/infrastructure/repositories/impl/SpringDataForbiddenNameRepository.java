package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.impl;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataForbiddenNameRepository
        extends ForbiddenNameRepository, MongoRepository<ForbiddenName, String> {
    
    @Query("{ 'forbiddenName': { $regex: ?0, $options: 'i' } }")
    List<ForbiddenName> findByForbiddenNameIsContained(String pat);

    @Override
    @Query("{ 'forbiddenName': ?0 }")
    Optional<ForbiddenName> findByForbiddenName(String forbiddenName);

    @Override
    default int deleteForbiddenName(String forbiddenName) {
        Optional<ForbiddenName> fn = findByForbiddenName(forbiddenName);
        if (fn.isPresent()) {
            delete(fn.get());
            return 1;
        }
        return 0;
    }
}
