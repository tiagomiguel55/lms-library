package pt.psoft.g1.psoftg1.readermanagement.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.mongodb.ReaderDetailsMongoDB;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.mappers.ReaderMapperMongoDB;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderBookCountDTO;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class ReaderRepositoryMongoDBImpl implements ReaderRepository {

    private final ReaderRepositoryMongoDB readerRepositoryMongoDB;

    private final ReaderMapperMongoDB readerMapperMongoDB;

    @Autowired
    @Lazy
    public ReaderRepositoryMongoDBImpl(ReaderRepositoryMongoDB readerRepositoryMongoDB, ReaderMapperMongoDB readerMapperMongoDB) {
        this.readerRepositoryMongoDB = readerRepositoryMongoDB;
        this.readerMapperMongoDB = readerMapperMongoDB;
    }

    @Override
    public Optional<ReaderDetails> findByReaderNumber(String readerNumber) {

        Optional<ReaderDetailsMongoDB> readerDetailsMongoDB = readerRepositoryMongoDB.findByReaderNumber(readerNumber);
        if (readerDetailsMongoDB.isEmpty()) {
            return Optional.empty();
        }
        return readerDetailsMongoDB.map(readerMapperMongoDB::toDomain);
    }

    @Override
    public List<ReaderDetails> findByPhoneNumber(String phoneNumber) {
        return List.of();
    }

    @Override
    public Optional<ReaderDetails> findByUsername(String username) {
        if (readerRepositoryMongoDB.findByUsername(username).isEmpty()) {
            System.out.println("tou aqui");
            return Optional.empty();
        }
        return readerRepositoryMongoDB.findByUsername(username).map(readerMapperMongoDB::toDomain);
    }

    @Override
    public Optional<ReaderDetails> findByUserId(String userId) {
        if (readerRepositoryMongoDB.findByUserId(userId).isPresent()) {
            System.out.println("tou aqui");
            return readerRepositoryMongoDB.findByUserId(userId).map(readerMapperMongoDB::toDomain);
        }
        System.out.println("tou aqui");
        return Optional.empty();
    }

    @Override
    public int getCountFromCurrentYear() {
        return 0;
    }

    @Override
    public ReaderDetails save(ReaderDetails readerDetails) {
        // Pass Reader Details to MongoDB
        ReaderDetailsMongoDB readerDetailsMongoDB = readerMapperMongoDB.toMongoDB(readerDetails);
        ReaderDetailsMongoDB savedReaderDetailsMongoDB = readerRepositoryMongoDB.save(readerDetailsMongoDB);
        return readerMapperMongoDB.toDomain(savedReaderDetailsMongoDB);
    }

    @Override
    public ReaderDetails update(ReaderDetails readerDetails) {
        return null;
    }

    @Override
    public Iterable<ReaderDetails> findAll() {
        return null;
    }


    @Override
    public void delete(ReaderDetails readerDetails) {

    }

    @Override
    public List<ReaderDetails> searchReaderDetails(pt.psoft.g1.psoftg1.shared.services.Page page, SearchReadersQuery query) {
        return List.of();
    }
}
