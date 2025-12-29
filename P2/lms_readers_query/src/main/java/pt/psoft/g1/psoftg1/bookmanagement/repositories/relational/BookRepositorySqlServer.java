package pt.psoft.g1.psoftg1.bookmanagement.repositories.relational;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.BookEntity;


import java.util.List;
import java.util.Optional;

public interface BookRepositorySqlServer extends JpaRepository<BookEntity, Long> {
    @Query("SELECT b " +
            "FROM BookEntity b " +
            "WHERE b.isbn.isbn = :isbn")
    Optional<BookEntity> findByIsbn(@Param("isbn") String isbn);

    @Query("SELECT b FROM BookEntity b ")
    List<BookEntity> findAll();
}
