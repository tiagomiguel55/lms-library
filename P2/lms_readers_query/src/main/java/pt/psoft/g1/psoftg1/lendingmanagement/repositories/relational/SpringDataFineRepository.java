package pt.psoft.g1.psoftg1.lendingmanagement.repositories.relational;
/*
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.relationalDataModel.FineEntity;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;

import java.util.Optional;


public interface SpringDataFineRepository extends FineRepository, CrudRepository<FineEntity, Long> {

    @Override
    @Query("SELECT f " +
            "FROM FineEntity f " +
            "JOIN LendingEntity l ON f.lendingEntity.pk = l.pk " +
            "WHERE l.lendingNumberEntity.lendingNumber = :lendingNumber")
    Optional<Fine> findByLendingNumber(String lendingNumber);

}
*/