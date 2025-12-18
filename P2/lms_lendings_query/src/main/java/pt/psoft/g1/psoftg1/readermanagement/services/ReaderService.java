package pt.psoft.g1.psoftg1.readermanagement.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.shared.services.Page;

/**
 *
 */
public interface ReaderService {

    ReaderDetails create(ReaderViewAMQP reader);

    ReaderDetails update(ReaderViewAMQP reader);

    void delete(ReaderViewAMQP reader);

    Optional<ReaderDetails> findByUsername(String readerNumber);

}
