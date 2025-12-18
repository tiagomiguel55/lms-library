package pt.psoft.g1.psoftg1.lendingmanagement.repositories.relational.sqlServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;

import pt.psoft.g1.psoftg1.lendingmanagement.model.relational.FineEntity;

import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.mappers.FineEntityMapper;


import java.util.Optional;

@Profile("sqlServer")
@Qualifier("fineSqlServerRepo")
@Component
public class FineRepositorySqlServerImpl  implements FineRepository {

    private final FineRepositorySqlServer fineRepositorySqlServer;

    private final FineEntityMapper fineEntityMapper;


    @Autowired
    @Lazy
    public FineRepositorySqlServerImpl(FineRepositorySqlServer fineRepositorySqlServer,FineEntityMapper fineEntityMapper) {
        this.fineRepositorySqlServer = fineRepositorySqlServer;
        this.fineEntityMapper = fineEntityMapper;
    }
    @Override
    public Optional<Fine> findByLendingNumber(String lendingNumber) {
        return fineRepositorySqlServer.findByLendingNumber(lendingNumber);
    }

    @Override
    public Fine save(Fine fine) {


        FineEntity fineEntity = fineEntityMapper.modelToSqlServer(fine);

        FineEntity savedEntity = fineRepositorySqlServer.save(fineEntity);

        return fineEntityMapper.sqlServerToModel(savedEntity);
    }
}
