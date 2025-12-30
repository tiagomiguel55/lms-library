package pt.psoft.g1.psoftg1.bookmanagement.repositories.relational;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.BookEntity;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.mappers.BookEntityMapper;

import java.util.Optional;

@Profile("sqlServer")
@Qualifier("bookSqlServerRepo")
@Component
public class BookRepositorySqlServerImpl implements BookRepository {

    private final BookRepositorySqlServer bookRepositorySqlServer;
    private final BookEntityMapper bookEntityMapper;


    @Autowired
    @Lazy
    public BookRepositorySqlServerImpl(BookRepositorySqlServer bookRepositorySqlServer, BookEntityMapper bookEntityMapper) {
        this.bookRepositorySqlServer = bookRepositorySqlServer;
        this.bookEntityMapper = bookEntityMapper;
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        if (bookRepositorySqlServer.findByIsbn(isbn).isEmpty()) {
            return Optional.empty();
        }else{
            BookEntity book = bookRepositorySqlServer.findByIsbn(isbn).get();

            return Optional.of(bookEntityMapper.toModel(book)) ;
        }

    }

    @Override
    public Book save(Book book) {

        BookEntity bookEntity = bookEntityMapper.toEntity(book);


        BookEntity savedEntity = bookRepositorySqlServer.save(bookEntity);

        return bookEntityMapper.toModel(savedEntity);

    }

    @Override
    public void delete(Book book) {

    }


}
