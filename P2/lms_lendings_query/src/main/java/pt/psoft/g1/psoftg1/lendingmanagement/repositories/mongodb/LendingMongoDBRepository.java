package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mongodb;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.LendingMongoDB;

import java.util.List;
import java.util.Optional;
@Profile("mongodb")
public interface LendingMongoDBRepository extends MongoRepository<LendingMongoDB, String> {

    @Query("{ 'lending_number.lendingNumber' : ?0 }")
    Optional<LendingMongoDB> findByLendingNumber(String lendingNumber);

    @Query("{ 'readerNumber' : ?0, 'isbn' : ?1 }")
    List<LendingMongoDB> listByReaderNumberAndIsbn(String readerNumber, String isbn);

    int getCountFromCurrentYear();

    @Query("{ 'readerNumber' : ?0, 'returnedDate' : null }")
    List<LendingMongoDB> listOutstandingByReaderNumber(String readerNumber);

    @Aggregation(pipeline = {
            "{ $match: { 'returned_date': { $ne: null } } }",
            "{ $group: { _id: null, avgDuration: { $avg: { $subtract: ['$returned_date', '$start_date'] } } } }",
            "{ $project: { _id: 0, avgDuration: '$avgDuration' } }"
    })
    Double getAverageDuration();

    @Query("{ 'isbn' : ?0 }")
    Double getAvgLendingDurationByIsbn(String isbn);
}


