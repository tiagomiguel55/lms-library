package pt.psoft.g1.psoftg1.shared.repositories.relational;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.model.relational.ForbiddenNameEntity;

import java.util.List;
import java.util.Optional;

public interface ForbiddenNameRepositorySqlServer extends CrudRepository<ForbiddenNameEntity, Long> {

    @Query("SELECT fn FROM ForbiddenNameEntity fn" +
            " WHERE :pat LIKE CONCAT('%', fn.forbiddenName, '%') ")
    List<ForbiddenNameEntity> findByForbiddenNameIsContained(String pat);


    @Query("SELECT fn " +
            "FROM ForbiddenNameEntity fn " +
            "WHERE fn.forbiddenName = :forbiddenName")
    Optional<ForbiddenNameEntity> findByForbiddenName(String forbiddenName);


    @Modifying
    @Query("DELETE FROM ForbiddenNameEntity fn WHERE fn.forbiddenName = :forbiddenName")
    int deleteForbiddenName(String forbiddenName);

}
