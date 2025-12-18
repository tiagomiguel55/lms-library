package pt.psoft.g1.psoftg1.readermanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {

    private final ReaderRepository readerRepo;
    private final UserRepository userRepo;
    private final ForbiddenNameRepository forbiddenNameRepository;
    private final PhotoRepository photoRepository;




    @Override
    public ReaderDetails create(ReaderViewAMQP reader) {
        if (userRepo.findByUsername(reader.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists!");
        }


        Iterable<String> words = List.of(reader.getFullName().split("\\s+"));

        for (String word : words){
            if(!forbiddenNameRepository.findByForbiddenNameIsContained(word).isEmpty()) {
                throw new IllegalArgumentException("Name contains a forbidden word");
            }
        }




        Reader r = new Reader(reader.getUsername(), reader.getPassword());

        r.setName(reader.getFullName());

        String readerNumber = reader.getReaderNumber().split("/")[1];
        System.out.println("Reader number: " + readerNumber);
        ReaderDetails rd = new ReaderDetails(Integer.parseInt(readerNumber), r);
        ReaderDetails saved = readerRepo.save(rd);
        System.out.println("Saved: " + saved);
        return saved;
    }

    @Override
    public ReaderDetails update(ReaderViewAMQP reader) {
        return null;
    }

    @Override
    public void delete(ReaderViewAMQP reader) {

        ReaderDetails rd = readerRepo.findByReaderNumber(reader.getReaderNumber()).orElseThrow();

        try {
            readerRepo.delete(rd);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public Optional<ReaderDetails> findByUsername(String username) {
        return this.readerRepo.findByUsername(username);
    }
}
