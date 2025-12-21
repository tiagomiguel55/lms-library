package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.mongodb.BookRepositoryMongoDB;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.LendingMongoDB;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.mappers.LendingMongoDBMapper;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class LendingMongoDBRepositoryImpl implements LendingRepository {

    private final LendingMongoDBRepository lendingMongoDBRepository;
    private final LendingMongoDBMapper lendingMapperMongoDB;
    private final BookRepositoryMongoDB bookRepositoryMongoDB;
    private final MongoTemplate mongoTemplate;

    @Autowired
    @Lazy
    public LendingMongoDBRepositoryImpl(LendingMongoDBRepository lendingMongoDBRepository, LendingMongoDBMapper lendingMapperMongoDB, BookRepositoryMongoDB bookRepositoryMongoDB, MongoTemplate mongoTemplate) {
        this.lendingMongoDBRepository = lendingMongoDBRepository;
        this.lendingMapperMongoDB = lendingMapperMongoDB;
        this.bookRepositoryMongoDB = bookRepositoryMongoDB;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Lending> findByLendingNumber(String lendingNumber) {
        if(lendingMongoDBRepository.findByLendingNumber(lendingNumber).isEmpty()){
            return Optional.empty();
        } else {
            LendingMongoDB lendingMongoDB = lendingMongoDBRepository.findByLendingNumber(lendingNumber).get();
            System.out.println("Loading lending from MongoDB:");
            System.out.println("  MongoDB ID: " + lendingMongoDB.getId());
            System.out.println("  MongoDB Version: " + lendingMongoDB.getVersion());
            System.out.println("  Lending Number: " + lendingMongoDB.getLendingNumber());
            System.out.println("  Returned Date: " + lendingMongoDB.getReturnedDate());
            
            Lending lending = lendingMapperMongoDB.toDomain(lendingMongoDB);
            System.out.println("After mapping to domain:");
            System.out.println("  Domain Version: " + lending.getVersion());
            System.out.println("  Domain Returned Date: " + lending.getReturnedDate());
            System.out.println(lending);
            return Optional.of(lending);
        }
    }

    @Override
    public List<Lending> listByReaderNumberAndIsbn(String readerNumber, String isbn) {
        List<Lending> lendings = new ArrayList<>();

        for(LendingMongoDB lendingMongoDB : lendingMongoDBRepository.listByReaderNumberAndIsbn(readerNumber, isbn)){
            lendings.add(lendingMapperMongoDB.toDomain(lendingMongoDB));
        }

        return lendings;
    }

    @Override
    public int getCountFromCurrentYear() {
        try {
            int year = java.time.LocalDate.now().getYear();
            System.out.println("Getting next sequence for year " + year);
            
            // Use MongoDB's findAndModify to atomically get and increment the counter
            Query query = Query.query(Criteria.where("_id").is("lending_seq_" + year));
            Update update = new Update().inc("seq", 1);
            
            LendingSequenceCounter counter = mongoTemplate.findAndModify(
                query,
                update,
                new org.springframework.data.mongodb.core.FindAndModifyOptions().returnNew(true).upsert(true),
                LendingSequenceCounter.class
            );
            
            if (counter == null) {
                System.out.println("Counter was null, creating new one");
                counter = new LendingSequenceCounter("lending_seq_" + year, 1);
                mongoTemplate.save(counter, "lending_sequence");
                return 1;
            }
            
            System.out.println("Next sequence number: " + counter.getSeq());
            return counter.getSeq();
        } catch (Exception e) {
            System.err.println("Error getting next sequence: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public List<Lending> listOutstandingByReaderNumber(String readerNumber) {
        List<Lending> lendings = new ArrayList<>();

        System.out.println("Starting to retrieve outstanding lendings for reader: " + readerNumber);
        List<LendingMongoDB> mongoDbLendings = lendingMongoDBRepository.listOutstandingByReaderNumber(readerNumber);
        System.out.println("Found " + mongoDbLendings.size() + " MongoDB lendings");
        
        int count = 0;
        for(LendingMongoDB lendingMongoDB : mongoDbLendings){
            count++;
            System.out.println("Converting lending " + count + " to domain object...");
            try {
                Lending domainLending = lendingMapperMongoDB.toDomain(lendingMongoDB);
                System.out.println("Successfully converted lending " + count);
                lendings.add(domainLending);
            } catch (Exception e) {
                System.err.println("Error converting lending " + count + ": " + e.getClass().getName());
                throw e;
            }
        }

        return lendings;
    }

    @Override
    public Double getAverageDuration() {
        return this.lendingMongoDBRepository.getAverageDuration();
    }

    @Override
    public Double getAvgLendingDurationByIsbn(String isbn) {
        return this.lendingMongoDBRepository.getAvgLendingDurationByIsbn(isbn);
    }

    @Override
    public List<Lending> getOverdue(Page page) {
        return List.of();
    }

    @Override
    public List<Lending> searchLendings(Page page, String readerNumber, String isbn, Boolean returned, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public Lending save(Lending lending) {
        System.out.println("Saving lending");
        System.out.println("LendingNumber on the beginning before the mapping: " + lending.getLendingNumber());
        System.out.println("Reader Details on the beginning before the mapping: " + lending.getReaderDetails());
        System.out.println("Limit Date on the beginning before the mapping: " + lending.getLimitDate());
        System.out.println(lending.getBook());
        LendingMongoDB lendingMongoDB = lendingMapperMongoDB.toMongoDB(lending);
        System.out.println("Get lending number: " + lendingMongoDB.getLendingNumber());
        System.out.println("Get reader details after mapping: " + lendingMongoDB.getReaderDetails());
        System.out.println("Limit Date on the beginning after the mapping: " + lendingMongoDB.getLimitDate());
        if(lendingMongoDB.getBook() != null && lendingMongoDB.getBook().getIsbn() != null){
            System.out.println("Get book first: " + lendingMongoDB.getBook());
            System.out.println("Book ISBN: " + lendingMongoDB.getBook().getIsbn());
            Optional<BookMongoDB> existingBookOptional = bookRepositoryMongoDB.findByIsbn(lendingMongoDB.getBook().getIsbn());
            System.out.println(existingBookOptional.get().getTitle());
            if(existingBookOptional.isPresent()){
                System.out.println("Book already exists so its present");
                lendingMongoDB.setBook(existingBookOptional.get());
                System.out.println("Get book second: " + lendingMongoDB.getBook());
            } else {
                System.out.println("Book does not exist so its not present");
                BookMongoDB savedBook = bookRepositoryMongoDB.save(lendingMongoDB.getBook());
                bookRepositoryMongoDB.save(savedBook);
            }
        }

        System.out.println("Lending Number: " + lendingMongoDB.getLendingNumber());
        System.out.println("Book ISBN: " + (lendingMongoDB.getBook() != null ? lendingMongoDB.getBook().getIsbn() : "null"));
        System.out.println("Reader Number: " + (lendingMongoDB.getReaderDetails() != null ? lendingMongoDB.getReaderDetails().getReaderNumber() : "null"));
        System.out.println("Start Date: " + lendingMongoDB.getStartDate());
        System.out.println("Limit Date: " + lendingMongoDB.getLimitDate());
        System.out.println("Returned Date: " + lendingMongoDB.getReturnedDate());
        System.out.println("Fine Value Per Day In Cents: " + lendingMongoDB.getFineValuePerDayInCents());
        System.out.println("Days Until Return: " + lendingMongoDB.getDaysUntilReturn());
        System.out.println("Days Overdue: " + lendingMongoDB.getDaysOverdue());
        
        // Find existing lending by lending_number to preserve the _id and version
        Optional<LendingMongoDB> existingLending = lendingMongoDBRepository.findByLendingNumber(lendingMongoDB.getLendingNumber().toString());
        if (existingLending.isPresent()) {
            // Preserve the existing _id and version so MongoDB updates instead of inserting
            lendingMongoDB.setId(existingLending.get().getId());
            lendingMongoDB.setVersion(existingLending.get().getVersion());
            System.out.println("Found existing lending with ID: " + existingLending.get().getId() + " and version: " + existingLending.get().getVersion());
            System.out.println("Updating existing lending with returned date: " + lendingMongoDB.getReturnedDate());
        } else {
            System.out.println("Creating new lending document");
        }
        
        LendingMongoDB savedEntity = lendingMongoDBRepository.save(lendingMongoDB);
        System.out.println("Lending saved successfully with ID: " + savedEntity.getId());
        System.out.println("Saved Lending Number: " + savedEntity.getLendingNumber());
        System.out.println("Saved Book ISBN: " + (savedEntity.getBook() != null ? savedEntity.getBook().getIsbn() : "null"));
        System.out.println("Saved Reader Number: " + (savedEntity.getReaderDetails() != null ? savedEntity.getReaderDetails().getReaderNumber() : "null"));
        System.out.println("Saved Start Date: " + savedEntity.getStartDate());
        System.out.println("Saved Limit Date: " + savedEntity.getLimitDate());
        System.out.println("Saved Returned Date: " + savedEntity.getReturnedDate());
        System.out.println("Saved Fine Value Per Day In Cents: " + savedEntity.getFineValuePerDayInCents());
        System.out.println("Saved Days Until Return: " + savedEntity.getDaysUntilReturn());
        System.out.println("Saved Days Overdue: " + savedEntity.getDaysOverdue());
        return lendingMapperMongoDB.toDomain(savedEntity);
    }

    @Override
    public void delete(Lending lending) {
        lendingMongoDBRepository.delete(lendingMapperMongoDB.toMongoDB(lending));
    }

    @Override
    public List<Lending> findAll() {
        List<Lending> lendings = new ArrayList<>();
        for(LendingMongoDB lendingMongoDB : lendingMongoDBRepository.findAll()){
            lendings.add(lendingMapperMongoDB.toDomain(lendingMongoDB));
        }
        return lendings;
    }
}
