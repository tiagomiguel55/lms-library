package pt.psoft.g1.psoftg1.readermanagement.repositories.relational;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import org.springframework.data.repository.query.Param;

import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderBookCountDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReaderRepositorySqlServer  extends JpaRepository<ReaderDetailsEntity, Long> {

    @Query("SELECT r FROM ReaderDetailsEntity r")
    List<ReaderDetailsEntity> findAll();

    @Query("SELECT r " +
            "FROM ReaderDetailsEntity r " +
            "WHERE r.readerNumber.readerNumber = :readerNumber")
    Optional<ReaderDetailsEntity> findByReaderNumber(@Param("readerNumber") @NotNull String readerNumber);


    @Query("SELECT r " +
            "FROM ReaderDetailsEntity r " +
            "WHERE r.phoneNumber.phoneNumber = :phoneNumber")
    List<ReaderDetailsEntity> findByPhoneNumber(@Param("phoneNumber") @NotNull String phoneNumber);


    @Query("SELECT r " +
            "FROM ReaderDetailsEntity r " +
            "JOIN UserEntity u ON r.reader.id = u.id " +
            "WHERE u.username = :username")
    Optional<ReaderDetailsEntity> findByUsername(@Param("username") @NotNull String username);


    @Query("SELECT r " +
            "FROM ReaderDetailsEntity r " +
            "JOIN UserEntity u ON r.reader.username = u.username " +
            "WHERE u.username = :userId")
    Optional<ReaderDetailsEntity> findByUserId(@Param("userId") @NotNull String userId);



    @Query("SELECT COUNT (rd) " +
            "FROM ReaderDetailsEntity rd " +
            "JOIN UserEntity u ON rd.reader.id = u.id " +
            "WHERE YEAR(u.createdAt) = YEAR(CURRENT_DATE)")
    int getCountFromCurrentYear();


}
