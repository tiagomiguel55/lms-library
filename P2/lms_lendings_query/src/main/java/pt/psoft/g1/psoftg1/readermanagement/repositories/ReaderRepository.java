package pt.psoft.g1.psoftg1.readermanagement.repositories;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.repository.query.Param;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

import java.util.Optional;

/**
 *
 */
public interface ReaderRepository {
    Optional<ReaderDetails> findByReaderNumber(@Param("readerNumber") @NotNull String readerNumber);

    Optional<ReaderDetails> findByUsername(@Param("username") @NotNull String username);
    ReaderDetails save(ReaderDetails readerDetails);

    void delete(ReaderDetails rd);
}
