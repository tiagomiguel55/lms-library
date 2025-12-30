package pt.psoft.g1.psoftg1.lendingmanagement.repositories.relational.sqlServer;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.relational.FineEntity;

import java.util.Optional;

@Repository
public interface FineRepositorySqlServer extends CrudRepository<FineEntity, Long> {

    @Query("SELECT f " +
            "FROM FineEntity f " +
            "JOIN LendingEntity l ON f.lendingEntity.pk = l.pk " +
            "WHERE l.lendingNumberEntity.lendingNumber = :lendingNumber")
    Optional<Fine> findByLendingNumber(String lendingNumber);

}
