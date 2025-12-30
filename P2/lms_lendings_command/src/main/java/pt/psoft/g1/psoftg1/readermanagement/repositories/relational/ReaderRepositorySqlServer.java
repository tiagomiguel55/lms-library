package pt.psoft.g1.psoftg1.readermanagement.repositories.relational;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;

import java.util.Optional;

public interface ReaderRepositorySqlServer  extends CrudRepository<ReaderDetailsEntity, Long> {

    @Query("SELECT r " +
            "FROM ReaderDetailsEntity r " +
            "WHERE r.readerNumber.readerNumber = :readerNumber")
    Optional<ReaderDetailsEntity> findByReaderNumber(@Param("readerNumber") @NotNull String readerNumber);

    @Query("SELECT r " +
            "FROM ReaderDetailsEntity r " +
            "JOIN UserEntity u ON r.reader.id = u.id " +
            "WHERE u.username = :username")
    Optional<ReaderDetailsEntity> findByUsername(@Param("username") @NotNull String username);

}
