package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
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

    @Autowired
    @Lazy
    public LendingMongoDBRepositoryImpl(LendingMongoDBRepository lendingMongoDBRepository, LendingMongoDBMapper lendingMapperMongoDB, BookRepositoryMongoDB bookRepositoryMongoDB) {
        this.lendingMongoDBRepository = lendingMongoDBRepository;
        this.lendingMapperMongoDB = lendingMapperMongoDB;
        this.bookRepositoryMongoDB = bookRepositoryMongoDB;
    }

    @Override
    public Optional<Lending> findByLendingNumber(String lendingNumber) {
        if(lendingMongoDBRepository.findByLendingNumber(lendingNumber).isEmpty()){
            return Optional.empty();
        } else {
            Lending lending = lendingMapperMongoDB.toDomain(lendingMongoDBRepository.findByLendingNumber(lendingNumber).get());
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
        return this.lendingMongoDBRepository.getCountFromCurrentYear();
    }

    @Override
    public List<Lending> listOutstandingByReaderNumber(String readerNumber) {
        List<Lending> lendings = new ArrayList<>();

        for(LendingMongoDB lendingMongoDB : lendingMongoDBRepository.listOutstandingByReaderNumber(readerNumber)){
            lendings.add(lendingMapperMongoDB.toDomain(lendingMongoDB));
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
            existingBookOptional.ifPresent(existing -> System.out.println(existing.getTitle()));

            if (existingBookOptional.isPresent()) {
                System.out.println("Book already exists so its present");
                lendingMongoDB.setBook(existingBookOptional.get());
                System.out.println("Get book second: " + lendingMongoDB.getBook());
            } else {
                System.out.println("Book does not exist so its not present");
                BookMongoDB savedBook = bookRepositoryMongoDB.save(lendingMongoDB.getBook());
                lendingMongoDB.setBook(savedBook);
            }
        }

        System.out.println("Lending Number: " + lendingMongoDB.getLendingNumber());
        System.out.println("Book: " + lendingMongoDB.getBook().toString());
        System.out.println("Reader Details: " + lendingMongoDB.getReaderDetails().toString());
        System.out.println("Start Date: " + lendingMongoDB.getStartDate());
        System.out.println("Limit Date: " + lendingMongoDB.getLimitDate());
        System.out.println("Returned Date: " + lendingMongoDB.getReturnedDate());
        System.out.println("Fine Value Per Day In Cents: " + lendingMongoDB.getFineValuePerDayInCents());
        System.out.println("Days Until Return: " + lendingMongoDB.getDaysUntilReturn());
        System.out.println("Days Overdue: " + lendingMongoDB.getDaysOverdue());
        LendingMongoDB savedEntity = lendingMongoDBRepository.save(lendingMongoDB);
        System.out.println("Saved entity: " + savedEntity);
        System.out.println("Lending Number: " + savedEntity.getLendingNumber());
        System.out.println("Book: " + savedEntity.getBook().toString());
        System.out.println("Reader Details: " + savedEntity.getReaderDetails().toString());
        System.out.println("Start Date: " + savedEntity.getStartDate());
        System.out.println("Limit Date: " + savedEntity.getLimitDate());
        System.out.println("Returned Date: " + savedEntity.getReturnedDate());
        System.out.println("Fine Value Per Day In Cents: " + savedEntity.getFineValuePerDayInCents());
        System.out.println("Days Until Return: " + savedEntity.getDaysUntilReturn());
        System.out.println("Days Overdue: " + savedEntity.getDaysOverdue());
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
