package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.FineMongoDB;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.mappers.FineMapperMongoDB;

import java.util.Optional;


@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class FineRepositoryMongoDBImpl implements FineRepository {

    private final FineRepositoryMongoDB fineRepositoryMongoDB;

    private final FineMapperMongoDB fineMapperMongoDB;

    @Autowired
    @Lazy
    public FineRepositoryMongoDBImpl(FineRepositoryMongoDB fineRepositoryMongoDB, FineMapperMongoDB fineMapperMongoDB) {
        this.fineRepositoryMongoDB = fineRepositoryMongoDB;
        this.fineMapperMongoDB = fineMapperMongoDB;
    }

    @Override
    public Optional<Fine> findByLendingNumber(String lendingNumber) {
        Optional<FineMongoDB> fineMongoDB = fineRepositoryMongoDB.findByLendingNumber(lendingNumber);
        if(fineMongoDB.isEmpty()){
            return Optional.empty();
        } else {
            return Optional.of(fineMapperMongoDB.toDomain(fineMongoDB.get()));
        }
    }

    @Override
    public Fine save(Fine fine) {
        FineMongoDB fineMonghoDB = fineMapperMongoDB.toMongoDB(fine);
        FineMongoDB savedEntity = fineRepositoryMongoDB.save(fineMonghoDB);
        return fineMapperMongoDB.toDomain(savedEntity);
    }
}
