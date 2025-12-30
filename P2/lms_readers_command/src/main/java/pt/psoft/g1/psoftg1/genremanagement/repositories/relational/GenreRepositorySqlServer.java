package pt.psoft.g1.psoftg1.genremanagement.repositories.relational;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pt.psoft.g1.psoftg1.genremanagement.model.relational.GenreEntity;

import java.util.List;
import java.util.Optional;

public interface GenreRepositorySqlServer extends JpaRepository<GenreEntity, Long> {
    @Query("SELECT g FROM GenreEntity g")
    List<GenreEntity> findAll();


    @Query("SELECT g FROM GenreEntity g WHERE g.genre = :genre" )
    Optional<GenreEntity> findByString(@Param("genre")@NotNull String genre);

}
