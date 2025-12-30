package pt.psoft.g1.psoftg1.bookmanagement.repositories;

import org.springframework.data.repository.query.Param;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;

import java.util.Optional;


/**
 *
 */
public interface BookRepository {

    Optional<Book> findByIsbn(@Param("isbn") String isbn);
    Book save(Book book);
    void delete(Book book);
}
