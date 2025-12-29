package pt.psoft.g1.psoftg1.bookmanagement.repositories;

import pt.psoft.g1.psoftg1.bookmanagement.model.Book;

import java.util.List;
import java.util.Optional;


/**
 *
 */
public interface BookRepository {

    Optional<Book> findByIsbn(String isbn);
    List<Book> findMostLentBooksByGenre(int maxBooks, String genre);
    Book save(Book book);
    void delete(Book book);
    List<Book> findAll();
}
