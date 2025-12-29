package pt.psoft.g1.psoftg1.bookmanagement.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.bookmanagement.model.PendingBookEvent;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingBookEventRepository extends MongoRepository<PendingBookEvent, String> {
    Optional<PendingBookEvent> findByBookId(String bookId);
    List<PendingBookEvent> findByGenreName(String genreName);
}
