package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.FineMongoDB;

import java.util.Optional;

public interface FineRepositoryMongoDB extends MongoRepository<FineMongoDB, String> {

    @Query("{ 'lendingNumber' : ?0 }")
    Optional<FineMongoDB> findByLendingNumber(String lendingNumber);
}
