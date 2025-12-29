package pt.psoft.g1.psoftg1.readermanagement.repositories.mongodb;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.readermanagement.model.mongodb.ReaderDetailsMongoDB;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderBookCountDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Profile("mongodb")
public interface ReaderRepositoryMongoDB extends MongoRepository<ReaderDetailsMongoDB, String> {

    @Query("{ 'readerNumber.readerNumber': ?0 }")
    Optional<ReaderDetailsMongoDB> findByReaderNumber(String readerNumber);

    @Query("{ 'phoneNumber.phoneNumber': ?0 }")
    List<ReaderDetailsMongoDB> findByPhoneNumber(String phoneNumber);

    @Query("{ 'reader.id': ?0 }")
    Optional<ReaderDetailsMongoDB> findByUserId(String userId);

    @Query("{ 'reader.username': ?0 }")
    Optional<ReaderDetailsMongoDB> findByUsername(String username);

    @Aggregation(pipeline = {
            "{ '$lookup': { 'from': 'user', 'localField': 'reader.id', 'foreignField': '_id', 'as': 'user' }}",
            "{ '$unwind': '$user' }",
            "{ '$match': { '$expr': { '$eq': [ { '$year': '$user.createdAt' }, { '$year': '$$NOW' }] }}}",
            "{ '$count': 'count' }"
    })
    int getCountFromCurrentYear();

//    @Aggregation(pipeline = {
//            "{ '$lookup': { 'from': 'lendingEntity', 'localField': 'pk', 'foreignField': 'readerDetails.pk', 'as': 'lendings' }}",
//            "{ '$group': { '_id': '$_id', 'count': { '$size': '$lendings' }, 'readerDetails': { '$first': '$$ROOT' } }}",
//            "{ '$sort': { 'count': -1 }}",
//            "{ '$project': { 'readerDetails': 1 }}",
//            "{ '$limit': ?0 }"
//    })
//    Page<ReaderDetailsMongoDB> findTopReaders(Pageable pageable);

//    @Aggregation(pipeline = {
//            "{ '$lookup': { 'from': 'lendingEntity', 'localField': 'pk', 'foreignField': 'readerDetails.pk', 'as': 'lendings' }}",
//            "{ '$unwind': '$lendings' }",
//            "{ '$lookup': { 'from': 'bookEntity', 'localField': 'lendings.book.pk', 'foreignField': '_id', 'as': 'book' }}",
//            "{ '$unwind': '$book' }",
//            "{ '$lookup': { 'from': 'genreEntity', 'localField': 'book.genre.pk', 'foreignField': '_id', 'as': 'genre' }}",
//            "{ '$unwind': '$genre' }",
//            "{ '$match': { 'genre.genre': ?0, 'lendings.startDate': { '$gte': ?1, '$lte': ?2 } }}",
//            "{ '$group': { '_id': '$_id', 'count': { '$sum': 1 }, 'readerDetails': { '$first': '$$ROOT' } }}",
//            "{ '$sort': { 'count': -1 }}",
//            "{ '$project': { 'readerDetails': 1, 'count': 1 }}",
//            "{ '$limit': ?3 }"
//    })
//    Page<ReaderBookCountDTO> findTopByGenre(String genre, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
