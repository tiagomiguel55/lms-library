package pt.psoft.g1.psoftg1.shared.repositories.relational;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.model.relational.ForbiddenNameEntity;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.repositories.mappers.ForbiddenNameEntityMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Profile("sqlServer")
@Qualifier("forbiddenNameSqlServerRepo")
@Component
public class ForbiddenNameRepositorySqlServerImpl implements ForbiddenNameRepository {

    private final ForbiddenNameRepositorySqlServer forbiddenNameRepositorySqlServer;

    private final ForbiddenNameEntityMapper forbiddenNameEntityMapper;

    @Autowired
    @Lazy
    public ForbiddenNameRepositorySqlServerImpl(ForbiddenNameRepositorySqlServer forbiddenNameRepositorySqlServer, ForbiddenNameEntityMapper forbiddenNameEntityMapper) {
        this.forbiddenNameRepositorySqlServer = forbiddenNameRepositorySqlServer;
        this.forbiddenNameEntityMapper = forbiddenNameEntityMapper;
    }
    @Override
    public Iterable<ForbiddenName> findAll() {
        ArrayList<ForbiddenName> forbiddenNames = new ArrayList<>();

        for (ForbiddenNameEntity forbiddenNameEntity : forbiddenNameRepositorySqlServer.findAll()) {
            forbiddenNames.add(forbiddenNameEntityMapper.toModel(forbiddenNameEntity));
        }
        return forbiddenNames;
    }

    @Override
    public List<ForbiddenName> findByForbiddenNameIsContained(String pat) {
        List<ForbiddenName> forbiddenNames = new ArrayList<>();

        for (ForbiddenNameEntity forbiddenNameEntity : forbiddenNameRepositorySqlServer.findByForbiddenNameIsContained(pat)) {
            forbiddenNames.add(forbiddenNameEntityMapper.toModel(forbiddenNameEntity));
        }
        return forbiddenNames;
    }

    @Override
    public ForbiddenName save(ForbiddenName forbiddenName) {
        return forbiddenNameEntityMapper.toModel(forbiddenNameRepositorySqlServer.save(forbiddenNameEntityMapper.toEntity(forbiddenName)));
    }

    @Override
    public Optional<ForbiddenName> findByForbiddenName(String forbiddenName) {
        if (forbiddenNameRepositorySqlServer.findByForbiddenName(forbiddenName).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(forbiddenNameEntityMapper.toModel(forbiddenNameRepositorySqlServer.findByForbiddenName(forbiddenName).get()));
    }

    @Override
    public int deleteForbiddenName(String forbiddenName) {
        return forbiddenNameRepositorySqlServer.deleteForbiddenName(forbiddenName);
    }
}
