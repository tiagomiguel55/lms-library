package pt.psoft.g1.psoftg1.lendingmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SearchLendingQuery;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SetLendingReturnedRequest;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.api.ListResponse;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.shared.services.SearchRequest;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.util.List;

@Tag(name = "Lendings", description = "Endpoints for managing Lendings")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lendings")
public class LendingController {
    private final LendingService lendingService;
    private final ReaderService readerService;
    private final UserService userService;
    private final ConcurrencyService concurrencyService;

    private final LendingViewMapper lendingViewMapper;

    @Operation(summary = "Creates a new Lending")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<LendingView> create(@Valid @RequestBody final CreateLendingRequest resource) {
        try {

            System.out.println("Creating Lending");

            final var lending = lendingService.create(resource);

            System.out.println("Lending created");

            final var newLendingUri = ServletUriComponentsBuilder.fromCurrentRequestUri()
                    .pathSegment(lending.getLendingNumber())
                    .build().toUri();

            return ResponseEntity.created(newLendingUri)
                    .contentType(MediaType.parseMediaType("application/hal+json"))
                    .eTag(Long.toString(lending.getVersion()))
                    .body(lendingViewMapper.toLendingView(lending));
        } catch (Exception ex) {
            System.out.println(ex.getMessage());

            // Return 400 Bad Request with the exception message
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(null);  // Optionally, return a custom error body
        }
    }

    @Operation(summary = "Creates a new Lending with detailed information")
    @PostMapping("/details")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<LendingDetailsView> createWithDetails(@Valid @RequestBody final LendingDetailsView lendingDetails) {
        try {
            System.out.println("Creating Lending with detailed information");

            // Criar o Lending com base nos detalhes fornecidos
            final var lending = lendingService.createWithDetails(lendingDetails);

            final var newLendingUri = ServletUriComponentsBuilder.fromCurrentRequestUri()
                    .pathSegment(lending.getLendingNumber())
                    .build().toUri();

            return ResponseEntity.created(newLendingUri)
                    .contentType(MediaType.parseMediaType("application/hal+json"))
                    .eTag(Long.toString(lending.getVersion()))
                    .body(lendingDetails);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(null);
        }
    }

    //@Operation(summary = "Gets a specific Lending")
    //@GetMapping(value = "/{year}/{seq}")
    //public ResponseEntity<LendingView> findByLendingNumber(
    //        Authentication authentication,
    //        @PathVariable("year")
    //            @Parameter(description = "The year of the Lending to find")
    //            final Integer year,
    //        @PathVariable("seq")
    //            @Parameter(description = "The sequencial of the Lending to find")
    //            final Integer seq) {
    //
    //    String ln = year + "/" + seq;
    //    final var lending = lendingService.findByLendingNumber(ln)
    //            .orElseThrow(() -> new NotFoundException(Lending.class, ln));
    //
    //    User loggedUser = userService.getAuthenticatedUser(authentication);
    //
    //    System.out.println("Logged User: " + loggedUser.getUsername());
    //    System.out.println(loggedUser.getAuthorities());
    //    System.out.println(loggedUser.getClass());
    //
    //    //if Librarian is logged in, skip ahead
    //    if (!(loggedUser.getAuthorities().contains(new Role("LIBRARIAN")))) {
    //        final var loggedReaderDetails = readerService.findByUsername(loggedUser.getUsername())
    //                .orElseThrow(() -> new NotFoundException(ReaderDetails.class, loggedUser.getUsername()));
    //
    //        //if logged Reader matches the one associated with the lending, skip ahead
    //        if (!Objects.equals(loggedReaderDetails.getReaderNumber(), lending.getReaderDetails().getReaderNumber())) {
    //            throw new AccessDeniedException("Reader does not have permission to view this lending");
    //        }
    //    }
    //    final var lendingUri = ServletUriComponentsBuilder.fromCurrentRequestUri()
    //            .build().toUri();
    //
    //    return ResponseEntity.ok().location(lendingUri)
    //            .contentType(MediaType.parseMediaType("application/hal+json"))
    //            .eTag(Long.toString(lending.getVersion()))
    //            .body(lendingViewMapper.toLendingView(lending));
    //}

    @Operation(summary = "Sets a lending as returned")
    @PatchMapping(value = "/{year}/{seq}")
    public ResponseEntity<LendingView> setLendingReturned(
            final WebRequest request,
            final Authentication authentication,
            @PathVariable("year")
                @Parameter(description = "The year component of the Lending to find")
                final Integer year,
            @PathVariable("seq")
                @Parameter(description = "The sequential component of the Lending to find")
                final Integer seq,
            @Valid @RequestBody final SetLendingReturnedRequest resource) {
        final String ifMatchValue = request.getHeader(ConcurrencyService.IF_MATCH);
        if (ifMatchValue == null || ifMatchValue.isEmpty() || ifMatchValue.equals("null")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You must issue a conditional PATCH using 'if-match'");
        }
        String ln = year + "/" + seq;
        final var maybeLending = lendingService.findByLendingNumber(ln)
                .orElseThrow(() -> new NotFoundException(Lending.class, ln));

        // Authorization temporarily disabled while auth microservice is unavailable
        // User loggedUser = userService.getAuthenticatedUser(authentication);
        // final var loggedReaderDetails = readerService.findByUsername(loggedUser.getUsername())
        //         .orElseThrow(() -> new NotFoundException(ReaderDetails.class, loggedUser.getUsername()));
        // //if logged Reader matches the one associated with the lending, skip ahead
        // if (!Objects.equals(loggedReaderDetails.getReaderNumber(), maybeLending.getReaderDetails().getReaderNumber())) {
        //     throw new AccessDeniedException("Reader does not have permission to edit this lending");
        // }
        System.out.println("Authorization disabled - skipping permission checks (temporary)");

        final var lending = lendingService.setReturned(ln, resource, concurrencyService.getVersionFromIfMatchHeader(ifMatchValue));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/hal+json"))
                .eTag(Long.toString(lending.getVersion()))
                .body(lendingViewMapper.toLendingView(lending));
    }

    //@Operation(summary = "Get average lendings duration")
    //@GetMapping(value = "/avgDuration")
    //public @ResponseBody ResponseEntity<LendingsAverageDurationView> getAvgDuration() {
    //
    //    return ResponseEntity.ok().body(lendingViewMapper.toLendingsAverageDurationView(lendingService.getAverageDuration()));
    //}
    //
    //@Operation(summary = "Get list of overdue lendings")
    //@GetMapping(value = "/overdue")
    //public ListResponse<LendingView> getOverdueLendings(@Valid @RequestBody Page page) {
    //    final List<Lending> overdueLendings = lendingService.getOverdue(page);
    //    if(overdueLendings.isEmpty())
    //        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
    //                "No lendinds to show.");
    //    return new ListResponse<>(lendingViewMapper.toLendingView(overdueLendings));
    //}

    @PostMapping("/search")
    public ListResponse<LendingView> searchReaders(
            @RequestBody final SearchRequest<SearchLendingQuery> request) {
        final var readerList = lendingService.searchLendings(request.getPage(), request.getQuery());
        return new ListResponse<>(lendingViewMapper.toLendingView(readerList));
    }
}
