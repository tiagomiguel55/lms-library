package pt.psoft.g1.psoftg1.readermanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookView;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewMapper;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;
import pt.psoft.g1.psoftg1.readermanagement.services.UpdateReaderRequest;
import pt.psoft.g1.psoftg1.shared.api.ListResponse;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.shared.services.SearchRequest;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Tag(name = "Readers", description = "Endpoints to manage readers")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/readers")
public class ReaderController {
    private final ReaderService readerService;
    private final UserService userService;
    private final BookService bookService;
    private final ReaderViewMapper readerViewMapper;
    private final BookViewMapper bookViewMapper;

    private final ConcurrencyService concurrencyService;
    private final FileStorageService fileStorageService;

    private final ApiNinjasService apiNinjasService;



    @Operation(summary = "Creates a reader")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ReaderView> createReader(@Valid @RequestBody CreateReaderRequest readerRequest) throws ValidationException {
        MultipartFile file = readerRequest.getPhoto();

        String fileName = fileStorageService.getRequestPhoto(file);

        ReaderDetails readerDetails = readerService.create(readerRequest, fileName);

        final var newReaderUri = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(readerDetails.getReaderNumber())
                .build().toUri();

        return ResponseEntity.created(newReaderUri)
                .eTag(Long.toString(readerDetails.getVersion()))
                .body(readerViewMapper.toReaderView(readerDetails));
    }

    @Operation(summary = "Deletes a reader photo")
    @DeleteMapping("/photo")
    public ResponseEntity<Void> deleteReaderPhoto(Authentication authentication) {
        User loggedUser = userService.getAuthenticatedUser(authentication);

        Optional<ReaderDetails> optReaderDetails = readerService.findByUsername(loggedUser.getUsername());
        if(optReaderDetails.isEmpty()) {
            throw new AccessDeniedException("Could not find a valid reader from current auth");
        }

        ReaderDetails readerDetails = optReaderDetails.get();

        if(readerDetails.getPhoto() == null) {
            throw new NotFoundException("Reader has no photo to delete");
        }

        this.fileStorageService.deleteFile(readerDetails.getPhoto().getPhotoFile());
        readerService.removeReaderPhoto(readerDetails.getReaderNumber(), readerDetails.getVersion());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Updates a reader")
    @RolesAllowed(Role.READER)
    @PatchMapping
    public ResponseEntity<ReaderView> updateReader(
            @Valid UpdateReaderRequest readerRequest,
            Authentication authentication,
            final WebRequest request) {

        final String ifMatchValue = request.getHeader(ConcurrencyService.IF_MATCH);
        if (ifMatchValue == null || ifMatchValue.isEmpty() || ifMatchValue.equals("null")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You must issue a conditional PATCH using 'if-match'");
        }

        MultipartFile file = readerRequest.getPhoto();

        String fileName = this.fileStorageService.getRequestPhoto(file);

        User loggedUser = userService.getAuthenticatedUser(authentication);
        ReaderDetails readerDetails = readerService
                .update(loggedUser.getUsername(), readerRequest, concurrencyService.getVersionFromIfMatchHeader(ifMatchValue), fileName);

        return ResponseEntity.ok()
                .eTag(Long.toString(readerDetails.getVersion()))
                .body(readerViewMapper.toReaderView(readerDetails));
    }

    @PostMapping("/search")
    public ListResponse<ReaderView> searchReaders(
            @RequestBody final SearchRequest<SearchReadersQuery> request) {
        final var readerList = readerService.searchReaders(request.getPage(), request.getQuery());
        return new ListResponse<>(readerViewMapper.toReaderView(readerList));
    }
}
