package pt.psoft.g1.psoftg1.readermanagement.services;

import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface ReaderService {
    ReaderDetails create(CreateReaderRequest request, String photoURI);
    ReaderDetails create(ReaderViewAMQP readerViewAMQP);
    ReaderDetails update(String id, UpdateReaderRequest request, long desireVersion, String photoURI);
    ReaderDetails update(ReaderViewAMQP readerViewAMQP);
    void delete(String id);
    void delete(ReaderViewAMQP readerViewAMQP);
    Optional<ReaderDetails> findByUsername(final String username);
    Optional<ReaderDetails> findByReaderNumber(String readerNumber);
    List<ReaderDetails> findByPhoneNumber(String phoneNumber);
    Iterable<ReaderDetails> findAll();

    Optional<ReaderDetails> removeReaderPhoto(String readerNumber, long desiredVersion);
    List<ReaderDetails> searchReaders(Page page, SearchReadersQuery query);
    List<ReaderViewAMQP> getAllReaders();
}
