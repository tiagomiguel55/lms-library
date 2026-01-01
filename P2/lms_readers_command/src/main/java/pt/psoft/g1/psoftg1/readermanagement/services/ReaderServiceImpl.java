package pt.psoft.g1.psoftg1.readermanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderUserRequestedEvent;
import pt.psoft.g1.psoftg1.readermanagement.model.PendingReaderUserRequest;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.repositories.PendingReaderUserRequestRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {
    private final ReaderRepository readerRepo;
    private final UserRepository userRepo;
    private final ReaderMapper readerMapper;
    private final GenreRepository genreRepo;
    private final ForbiddenNameRepository forbiddenNameRepository;
    private final PhotoRepository photoRepository;
    private final ReaderEventPublisher readerEventPublisher;
    private final ReaderViewAMQPMapper readerViewAMQPMapper;
    private final PendingReaderUserRequestRepository pendingReaderUserRequestRepository;


    @Override
    public ReaderDetails create(CreateReaderRequest request, String photoURI) {
        if (userRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists!");
        }

        Iterable<String> words = List.of(request.getFullName().split("\\s+"));
        for (String word : words){
            if(!forbiddenNameRepository.findByForbiddenNameIsContained(word).isEmpty()) {
                throw new IllegalArgumentException("Name contains a forbidden word");
            }
        }

        List<String> stringInterestList = request.getInterestList();

        List<Genre> interestList = this.getGenreListFromStringList(stringInterestList);
        /*if(stringInterestList != null && !stringInterestList.isEmpty()) {
            request.setInterestList(this.getGenreListFromStringList(stringInterestList));
        }*/

        /*
         * Since photos can be null (no photo uploaded) that means the URI can be null as well.
         * To avoid the client sending false data, photoURI has to be set to any value / null
         * according to the MultipartFile photo object
         *
         * That means:
         * - photo = null && photoURI = null -> photo is removed
         * - photo = null && photoURI = validString -> ignored
         * - photo = validFile && photoURI = null -> ignored
         * - photo = validFile && photoURI = validString -> photo is set
         * */

        MultipartFile photo = request.getPhoto();
        if(photo == null && photoURI != null || photo != null && photoURI == null) {
            request.setPhoto(null);
        }

        int count = readerRepo.getCountFromCurrentYear();
        Reader reader = readerMapper.createReader(request);
        System.out.println("Reader: " + reader);
        System.out.println("Request: " + request.getPhoneNumber());

        ReaderDetails rd = readerMapper.createReaderDetails(count+1, reader, request, photoURI, interestList);


        //userRepo.save(reader);
        ReaderDetails saved = readerRepo.save(rd);
        if (saved != null) {
            readerEventPublisher.sendReaderCreated(saved);
        }
        return saved;
    }

    @Override
    public ReaderDetails create(ReaderViewAMQP readerViewAMQP) {

        if (userRepo.findByUsername(readerViewAMQP.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists!");
        }

        Iterable<String> words = List.of(readerViewAMQP.getFullName().split("\\s+"));

        for (String word : words){
            if(!forbiddenNameRepository.findByForbiddenNameIsContained(word).isEmpty()) {
                throw new IllegalArgumentException("Name contains a forbidden word");
            }
        }

        List<String> stringInterestList = readerViewAMQP.getInterestList();

        System.out.println("Interest list: " + stringInterestList);

        List<Genre> interestList = getGenreListFromStringList(stringInterestList);

        System.out.println("Interest list: " + interestList);

        Reader reader = readerMapper.createReader(readerViewAMQP);

        String readerNumber = readerViewAMQP.getReaderNumber().split("/")[1];
        System.out.println("Reader number: " + readerNumber);

        ReaderDetails rd = readerMapper.createReaderDetails(readerNumber, reader, readerViewAMQP,null, interestList);
        System.out.println("Reader: " + reader);
        //userRepo.save(reader);
        System.out.println("ReaderDetails: " + rd);
        ReaderDetails saved = readerRepo.save(rd);
        System.out.println("Saved: " + saved);
        return saved;
    }

    @Override
    @Transactional
    public ReaderDetails createWithUser(ReaderUserRequestedEvent request) {
        final String username = request.getUsername();

        // 1. First check if Reader already exists by username (most important check)
        Optional<ReaderDetails> existingReader = readerRepo.findByUsername(username);
        if (existingReader.isPresent()) {
            System.out.println(" [x] Reader already exists with username: " + username);
            return existingReader.get();
        }

        // 2. Check if there's already a pending request for this username
        Optional<PendingReaderUserRequest> pendingByUsername = pendingReaderUserRequestRepository.findByUsername(username);
        if (pendingByUsername.isPresent()) {
            PendingReaderUserRequest pending = pendingByUsername.get();
            System.out.println(" [x] Found pending request for username: " + username + " (Status: " + pending.getStatus() + ")");

            // If already completed, find and return the reader
            if (pending.getStatus() == PendingReaderUserRequest.RequestStatus.READER_USER_CREATED) {
                existingReader = readerRepo.findByReaderNumber(pending.getReaderNumber());
                if (existingReader.isPresent()) {
                    // Clean up the completed pending request
                    try {
                        pendingReaderUserRequestRepository.delete(pending);
                        System.out.println(" [x] Cleaned up completed pending request");
                    } catch (Exception e) {
                        System.out.println(" [x] Could not clean up pending request: " + e.getMessage());
                    }
                    return existingReader.get();
                }
            }

            // If failed, clean up and allow retry
            if (pending.getStatus() == PendingReaderUserRequest.RequestStatus.FAILED) {
                System.out.println(" [x] Previous request failed, cleaning up and allowing retry");
                try {
                    pendingReaderUserRequestRepository.delete(pending);
                } catch (Exception e) {
                    System.out.println(" [x] Could not clean up failed request: " + e.getMessage());
                }
                // Continue to create new request
            } else {
                // Request is still in progress, return null to indicate async processing
                System.out.println(" [x] Request still in progress, returning 202");
                return null;
            }
        }

        // 3. Check if user exists in User table (created by lms_auth_users)
        if (userRepo.findByUsername(username).isPresent()) {
            System.out.println(" [x] User already exists with username: " + username);
            throw new ConflictException("Username already exists!");
        }

        // 4. Generate reader number
        String readerNumber = request.getReaderNumber();
        if (readerNumber == null || readerNumber.isEmpty()) {
            int count = readerRepo.getCountFromCurrentYear();
            readerNumber = String.format("%d/%d", java.time.LocalDate.now().getYear(), count + 1);
            request.setReaderNumber(readerNumber);
        }

        // 5. Check if readerNumber already exists (edge case)
        if (readerRepo.findByReaderNumber(readerNumber).isPresent()) {
            System.out.println(" [x] Reader number already exists: " + readerNumber + ", generating new one");
            int count = readerRepo.getCountFromCurrentYear();
            readerNumber = String.format("%d/%d", java.time.LocalDate.now().getYear(), count + 1);
            request.setReaderNumber(readerNumber);
        }

        // 6. Save pending request
        PendingReaderUserRequest newPendingRequest = new PendingReaderUserRequest(
                readerNumber, username, request.getPassword(), request.getFullName(),
                request.getBirthDate(), request.getPhoneNumber(), request.getPhotoURI(),
                request.isGdpr(), request.isMarketing(), request.isThirdParty()
        );
        pendingReaderUserRequestRepository.save(newPendingRequest);

        System.out.println(" [x] Saved pending reader-user request for reader number: " + readerNumber);

        // 7. Publish ReaderUserRequestedEvent
        System.out.println(" [x] Publishing Reader-User Requested event for reader number: " + readerNumber);
        readerEventPublisher.sendReaderUserRequestedEvent(request);

        // Return null - the controller will handle this by returning HTTP 202 Accepted
        return null;
    }


    @Override
    public ReaderDetails update(final String id, final UpdateReaderRequest request, final long desiredVersion, String photoURI){
        System.out.println("ID: " + id);
        final ReaderDetails readerDetails = readerRepo.findByUserId(id)
                .orElseThrow(() -> new NotFoundException("Cannot find reader"));

        List<String> stringInterestList = request.getInterestList();
        List<Genre> interestList = this.getGenreListFromStringList(stringInterestList);

         /*
         * Since photos can be null (no photo uploaded) that means the URI can be null as well.
         * To avoid the client sending false data, photoURI has to be set to any value / null
         * according to the MultipartFile photo object
         *
         * That means:
         * - photo = null && photoURI = null -> photo is removed
         * - photo = null && photoURI = validString -> ignored
         * - photo = validFile && photoURI = null -> ignored
         * - photo = validFile && photoURI = validString -> photo is set
         * */

        MultipartFile photo = request.getPhoto();
        if(photo == null && photoURI != null || photo != null && photoURI == null) {
            request.setPhoto(null);
        }

        System.out.println("Desired version: " + desiredVersion);

        System.out.println(readerDetails.getVersion());



        readerDetails.applyPatch(desiredVersion, request, photoURI, interestList);

        userRepo.save(readerDetails.getReader());
        ReaderDetails saved = readerRepo.save(readerDetails);
        if (saved != null) {
            readerEventPublisher.sendReaderUpdated(saved,saved.getVersion());
        }
        return saved;
    }

    @Override
    public ReaderDetails update(ReaderViewAMQP readerViewAMQP) {
        final ReaderDetails readerDetails = readerRepo.findByReaderNumber(readerViewAMQP.getReaderNumber())
                .orElseThrow(() -> new NotFoundException("Cannot find reader"));

        List<String> stringInterestList = readerViewAMQP.getInterestList();
        List<Genre> interestList = this.getGenreListFromStringList(stringInterestList);

        readerDetails.applyPatch(Long.parseLong(readerViewAMQP.getVersion()), readerViewAMQP, null, interestList);

        userRepo.save(readerDetails.getReader());
        ReaderDetails saved = readerRepo.save(readerDetails);
        return saved;
    }

    @Override
    public void delete(String id) {

    }

    @Override
    public void delete(ReaderViewAMQP readerViewAMQP) {

        ReaderDetails readerDetails = readerRepo.findByReaderNumber(readerViewAMQP.getReaderNumber())
                .orElseThrow(() -> new NotFoundException("Cannot find reader"));
        try{
        readerRepo.delete(readerDetails);
        }catch (Exception e){
            throw new NotFoundException("Cannot delete reader");
        }

    }


    @Override
    public Optional<ReaderDetails> findByReaderNumber(String readerNumber) {
        return this.readerRepo.findByReaderNumber(readerNumber);
    }

    @Override
    public List<ReaderDetails> findByPhoneNumber(String phoneNumber) {
        return this.readerRepo.findByPhoneNumber(phoneNumber);
    }

    @Override
    public Optional<ReaderDetails> findByUsername(final String username) {
        return this.readerRepo.findByUsername(username);
    }


    @Override
    public Iterable<ReaderDetails> findAll() {
        return this.readerRepo.findAll();
    }



    public List<Genre> getGenreListFromStringList(List<String> interestList) {
        if(interestList == null) {
            return null;
        }

        if(interestList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Genre> genreList = new ArrayList<>();
        for(String interest : interestList) {
            System.out.println(interest);
            Optional<Genre> optGenre = genreRepo.findByName(interest);
            System.out.println(optGenre);
            if(optGenre.isEmpty()) {
                throw new NotFoundException("Could not find genre with name " + interest);
            }

            genreList.add(optGenre.get());
        }

        return genreList;
    }

    @Override
    public Optional<ReaderDetails> removeReaderPhoto(String readerNumber, long desiredVersion) {
        ReaderDetails readerDetails = readerRepo.findByReaderNumber(readerNumber)
                .orElseThrow(() -> new NotFoundException("Cannot find reader"));

        String photoFile = readerDetails.getPhoto().getPhotoFile();
        readerDetails.removePhoto(desiredVersion);
        Optional<ReaderDetails> updatedReader = Optional.of(readerRepo.save(readerDetails));
        photoRepository.deleteByPhotoFile(photoFile);
        return updatedReader;
    }

    @Override
    public List<ReaderDetails> searchReaders(pt.psoft.g1.psoftg1.shared.services.Page page, SearchReadersQuery query) {
        if (page == null)
            page = new pt.psoft.g1.psoftg1.shared.services.Page(1, 10);

        if (query == null)
            query = new SearchReadersQuery("", "","");

        final var list = readerRepo.searchReaderDetails(page, query);

        if(list.isEmpty())
            throw new NotFoundException("No results match the search query");

        return list;
    }

    @Override
    public List<ReaderViewAMQP> getAllReaders() {
        List<ReaderViewAMQP> readerViewAMQPList = new ArrayList<>();

        Iterable<ReaderDetails> readerDetails = readerRepo.findAll();

        for (ReaderDetails readerDetail : readerDetails) {
            readerViewAMQPList.add(readerViewAMQPMapper.toReaderViewAMQP(readerDetail));
        }
        return readerViewAMQPList;
    }
}
