package pt.psoft.g1.psoftg1.bookmanagement.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.mappers.BookMapperMongoDB;

import java.util.Optional;

@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class BookRepositoryMongoDBImpl implements BookRepository {

    private final BookRepositoryMongoDB bookRepositoryMongoDB;

    private final BookMapperMongoDB bookMapperMongoDB;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    @Lazy
    public BookRepositoryMongoDBImpl(BookRepositoryMongoDB bookRepositoryMongoDB,
                                     BookMapperMongoDB bookMapperMongoDB, MongoTemplate mongoTemplate) {
        this.bookRepositoryMongoDB = bookRepositoryMongoDB;

        this.bookMapperMongoDB = bookMapperMongoDB;

        this.mongoTemplate = mongoTemplate;
    }





    @Override
    public Optional<Book> findByIsbn(String isbn) {

        Optional<BookMongoDB> bookMongoDBFound = bookRepositoryMongoDB.findByIsbn(isbn);
        if (bookMongoDBFound.isEmpty()) {

            return Optional.empty();
        }


        Book bookDomainFound = bookMapperMongoDB.toDomain(bookMongoDBFound.get());

        return Optional.of(bookDomainFound);
    }



    @Override
    public Book save(Book book) {
        BookMongoDB bookMongoDB = bookMapperMongoDB.toMongoDB(book);

        BookMongoDB savedEntity = bookRepositoryMongoDB.save(bookMongoDB);
        return bookMapperMongoDB.toDomain(savedEntity);
    }

    @Override
    public void delete(Book book) {
        // Assuming you have a method to delete by ISBN or ID
        //bookRepositoryMongoDB.deleteByIsbn(book.getIsbn());
    }


}
