package pt.psoft.g1.psoftg1.readermanagement.repositories;

import org.springframework.data.repository.CrudRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.PendingReaderUserRequest;

import java.util.Optional;

public interface PendingReaderUserRequestRepository extends CrudRepository<PendingReaderUserRequest, Long> {

    Optional<PendingReaderUserRequest> findByReaderNumber(String readerNumber);
}
