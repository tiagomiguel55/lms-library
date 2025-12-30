package pt.psoft.g1.psoftg1.shared.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.model.mongodb.ForbiddenNameMongoDB;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.repositories.mappers.ForbiddenNameMapperMongoDB;

import java.util.List;
import java.util.Optional;

@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class ForbiddenNameRepositoryMongoDBImpl implements ForbiddenNameRepository {

    private final ForbiddenNameRepositoryMongoDB forbiddenNameRepositoryMongoDB;
    private final ForbiddenNameMapperMongoDB forbiddenNameMapperMongoDB;

    @Autowired
    @Lazy
    public ForbiddenNameRepositoryMongoDBImpl(ForbiddenNameRepositoryMongoDB forbiddenNameRepositoryMongoDB, ForbiddenNameMapperMongoDB forbiddenNameMapperMongoDB) {
        this.forbiddenNameRepositoryMongoDB = forbiddenNameRepositoryMongoDB;
        this.forbiddenNameMapperMongoDB = forbiddenNameMapperMongoDB;
    }

    @Override
    public Iterable<ForbiddenName> findAll() {
        return null;
    }

    @Override
    public List<ForbiddenName> findByForbiddenNameIsContained(String pat) {
        return null;
    }

    @Override
    public ForbiddenName save(ForbiddenName forbiddenName) {
        ForbiddenNameMongoDB forbiddenNameMongoDB = forbiddenNameMapperMongoDB.toMongoDB(forbiddenName);
        forbiddenNameRepositoryMongoDB.save(forbiddenNameMongoDB);
        ForbiddenNameMongoDB savedForbiddenNameMongoDB = forbiddenNameRepositoryMongoDB.save(forbiddenNameMongoDB);
        return forbiddenNameMapperMongoDB.toDomain(savedForbiddenNameMongoDB);
    }

    @Override
    public Optional<ForbiddenName> findByForbiddenName(String forbiddenName) {
        ForbiddenNameMongoDB forbiddenNameMongoDB = forbiddenNameRepositoryMongoDB.findByForbiddenName(forbiddenName).orElse(null);
        if(forbiddenNameMongoDB == null) {
            return Optional.empty();
        }
        return Optional.of(forbiddenNameMapperMongoDB.toDomain(forbiddenNameMongoDB));
    }

    @Override
    public int deleteForbiddenName(String forbiddenName) {
        return 0;
    }
}
