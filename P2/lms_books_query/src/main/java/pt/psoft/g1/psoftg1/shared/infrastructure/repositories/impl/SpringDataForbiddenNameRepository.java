package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataForbiddenNameRepository
        extends ForbiddenNameRepository, ForbiddenNameRepoCustom, MongoRepository<ForbiddenName, String> {

    @Override
    Optional<ForbiddenName> findByForbiddenName(String forbiddenName);

    @Override
    default int deleteForbiddenName(String forbiddenName) {
        // Will be handled by custom implementation
        return 0;
    }
}

interface ForbiddenNameRepoCustom {
    List<ForbiddenName> findByForbiddenNameIsContainedCustom(String pat);
    int deleteForbiddenNameCustom(String forbiddenName);
}

@RequiredArgsConstructor
class ForbiddenNameRepoCustomImpl implements ForbiddenNameRepoCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<ForbiddenName> findByForbiddenNameIsContainedCustom(String pat) {
        Query query = new Query();
        query.addCriteria(Criteria.where("forbiddenName").regex(".*" + pat + ".*", "i"));
        return mongoTemplate.find(query, ForbiddenName.class);
    }

    @Override
    public int deleteForbiddenNameCustom(String forbiddenName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("forbiddenName").is(forbiddenName));
        return (int) mongoTemplate.remove(query, ForbiddenName.class).getDeletedCount();
    }
}
