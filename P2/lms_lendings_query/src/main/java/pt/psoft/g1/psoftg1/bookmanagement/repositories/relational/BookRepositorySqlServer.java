package pt.psoft.g1.psoftg1.bookmanagement.repositories.relational;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.BookEntity;

import java.util.Optional;

public interface BookRepositorySqlServer extends CrudRepository<BookEntity, Long> {
    @Query("SELECT b " +
            "FROM BookEntity b " +
            "WHERE b.isbn.isbn = :isbn")
    Optional<BookEntity> findByIsbn(@Param("isbn") String isbn);




}
