package pt.psoft.g1.psoftg1.bookmanagement.repositories;

import org.springframework.data.repository.CrudRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.PendingBookRequest;

import java.util.Optional;

public interface PendingBookRequestRepository extends CrudRepository<PendingBookRequest, Long> {

    Optional<PendingBookRequest> findByBookId(String bookId);

    void deleteByBookId(String bookId);
}

