package pt.psoft.g1.psoftg1.readermanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {

    private final ReaderRepository readerRepo;
    private final UserRepository userRepo;

    @Override
    public ReaderDetails create(ReaderViewAMQP reader) {
        // Check if reader already exists
        Optional<ReaderDetails> existingReader = readerRepo.findByReaderNumber(reader.getReaderNumber());
        if (existingReader.isPresent()) {
            System.out.println(" [x] Reader already exists with number: " + reader.getReaderNumber());
            return existingReader.get();
        }

        // The User was already created by lms_auth_users in the SAGA
        // We just need to find it or create a minimal reference
        Reader r = userRepo.findByUsername(reader.getUsername())
                .map(user -> (Reader) user)
                .orElseGet(() -> {
                    // Create a minimal Reader reference without persisting User
                    // This should ideally not happen if SAGA worked correctly
                    System.out.println(" [x] Warning: User not found for username: " + reader.getUsername() + ". Creating minimal reference.");
                    Reader newReader = new Reader(reader.getUsername(), reader.getPassword());
                    newReader.setName(reader.getFullName());
                    return userRepo.save(newReader);
                });

        // Extract the sequential number from readerNumber (format: YYYY/###)
        String readerNumber = reader.getReaderNumber().split("/")[1];
        System.out.println(" [x] Creating reader with number: " + reader.getReaderNumber());

        ReaderDetails rd = new ReaderDetails(Integer.parseInt(readerNumber), r);
        ReaderDetails saved = readerRepo.save(rd);
        System.out.println(" [x] Reader saved successfully in lendings_command: " + saved.getReaderNumber());
        return saved;
    }

    @Override
    public ReaderDetails update(ReaderViewAMQP reader) {
        Optional<ReaderDetails> existingReader = readerRepo.findByReaderNumber(reader.getReaderNumber());
        if (existingReader.isEmpty()) {
            System.out.println(" [x] Reader does not exist with number: " + reader.getReaderNumber());
            return null;
        }

        ReaderDetails rd = existingReader.get();
        // Update reader information if needed
        Reader r = rd.getReader();
        r.setName(reader.getFullName());

        ReaderDetails updated = readerRepo.save(rd);
        System.out.println(" [x] Reader updated successfully in lendings_command: " + updated.getReaderNumber());
        return updated;
    }

    @Override
    public void delete(ReaderViewAMQP reader) {

        ReaderDetails rd = readerRepo.findByReaderNumber(reader.getReaderNumber()).orElseThrow();

        try {
            readerRepo.delete(rd);
            System.out.println(" [x] Reader deleted successfully in lendings_command: " + reader.getReaderNumber());
        } catch (Exception e) {
            System.out.println(" [x] Error deleting reader: " + e.getMessage());
        }
    }

    @Override
    public Optional<ReaderDetails> findByUsername(String username) {
        return this.readerRepo.findByUsername(username);
    }
}
