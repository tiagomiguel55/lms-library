package pt.psoft.g1.psoftg1.readermanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.readermanagement.model.PendingReaderUserRequest;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.repositories.PendingReaderUserRequestRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderMapper;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.usermanagement.api.UserPendingCreated;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Centralized RabbitMQ Listener for all Reader-related events
 *
 * Handles:
 * 1. SAGA: Reader + User creation (reader.user.requested.reader)
 * 2. SAGA Coordination: User and Reader pending confirmations
 * 3. Synchronization: Reader created/updated/deleted events
 * 4. Lending SAGA: Reader validation for lending operations
 */
@Component
@RequiredArgsConstructor
public class ReaderRabbitmqController {

    private final PendingReaderUserRequestRepository pendingReaderUserRequestRepository;
    private final ReaderRepository readerRepository;
    private final UserRepository userRepository;
    private final ReaderEventPublisher readerEventPublisher;
    private final ReaderMapper readerMapper;
    private final ForbiddenNameRepository forbiddenNameRepository;
    private final ReaderService readerService;
    private final ReaderViewAMQPMapper readerViewAMQPMapper;

    // ========================================
    // SAGA: Reader + User Creation
    // ========================================

    @RabbitListener(queues = "reader.user.requested.reader", concurrency = "1")
    public void receiveReaderUserRequested(String jsonReceived) {
        try {
            System.out.println(" [x] [SAGA-Step1] Received Reader-User creation request");

            ObjectMapper objectMapper = new ObjectMapper();
            ReaderUserRequestedEvent event = objectMapper.readValue(jsonReceived, ReaderUserRequestedEvent.class);

            System.out.println("     - Reader Number: " + event.getReaderNumber());
            System.out.println("     - Username: " + event.getUsername());

            // Check if reader already exists
            if (readerRepository.findByReaderNumber(event.getReaderNumber()).isPresent()) {
                System.out.println(" [x] ❌ Reader already exists with reader number: " + event.getReaderNumber());
                return;
            }

            // Validate full name for forbidden words
            Iterable<String> words = List.of(event.getFullName().split("\\s+"));
            for (String word : words) {
                if (!forbiddenNameRepository.findByForbiddenNameIsContained(word).isEmpty()) {
                    System.out.println(" [x] ❌ Name contains forbidden word: " + word);
                    return;
                }
            }

            // Create temporary Reader user for authentication
            Reader user = new Reader(event.getUsername(), event.getPassword());

            // Create temporary ReaderDetails entity
            String[] readerNumberParts = event.getReaderNumber().split("/");
            int readerSequence = Integer.parseInt(readerNumberParts[1]);

            ReaderDetails readerDetails = readerMapper.createReaderDetails(
                    readerSequence,
                    user,
                    createTempCreateReaderRequest(event),
                    event.getPhotoURI(),
                    List.of() // Empty interest list for now
            );

            ReaderDetails savedReader = readerRepository.save(readerDetails);
            System.out.println(" [x] ✅ [SAGA-Step1] Created temporary Reader with number: " + savedReader.getReaderNumber());

            // Send ReaderPendingCreated event
            ReaderPendingCreated readerPendingEvent = new ReaderPendingCreated(
                    event.getReaderNumber(),
                    savedReader.getReader().getId(),
                    event.getUsername(),
                    false
            );

            readerEventPublisher.sendReaderPendingCreated(readerPendingEvent);
            System.out.println(" [x] ✅ [SAGA-Step1] Notified coordination layer");

        } catch (Exception e) {
            System.out.println(" [x] ❌ [SAGA-Step1] Error creating Reader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = "user.pending.created", concurrency = "1")
    public void receiveUserPendingCreated(String jsonReceived) {
        try {
            System.out.println(" [x] [SAGA-Step2] Received User creation confirmation");

            ObjectMapper objectMapper = new ObjectMapper();
            UserPendingCreated event = objectMapper.readValue(jsonReceived, UserPendingCreated.class);

            System.out.println("     - Reader Number: " + event.getReaderNumber());
            System.out.println("     - Username: " + event.getUsername());

            updatePendingRequestAndTryFinalize(event.getReaderNumber(), true, false);

        } catch (Exception e) {
            System.out.println(" [x] ❌ [SAGA-Step2] Error processing User confirmation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = "reader.pending.created", concurrency = "1")
    public void receiveReaderPendingCreated(String jsonReceived) {
        try {
            System.out.println(" [x] [SAGA-Step3] Received Reader creation confirmation");

            ObjectMapper objectMapper = new ObjectMapper();
            ReaderPendingCreated event = objectMapper.readValue(jsonReceived, ReaderPendingCreated.class);

            System.out.println("     - Reader Number: " + event.getReaderNumber());
            System.out.println("     - Username: " + event.getUsername());

            updatePendingRequestAndTryFinalize(event.getReaderNumber(), false, true);

        } catch (Exception e) {
            System.out.println(" [x] ❌ [SAGA-Step3] Error processing Reader confirmation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================
    // Synchronization: Reader Events
    // ========================================

    @RabbitListener(queues = "#{readerCreatedQueue.name}")
    public void receiveReaderCreated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] [Sync] Received Reader Created - Reader Number: " + readerViewAMQP.getReaderNumber());

            try {
                readerService.create(readerViewAMQP);
                System.out.println(" [x] [Sync] Reader synchronized successfully");
            } catch (Exception e) {
                System.out.println(" [x] [Sync] Reader already exists, no sync needed");
            }
        } catch (Exception ex) {
            System.out.println(" [x] [Sync] Error receiving Reader created event: " + ex.getMessage());
        }
    }

    @RabbitListener(queues = "#{readerUpdatedQueue.name}")
    public void receiveReaderUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] [Sync] Received Reader Updated - Reader Number: " + readerViewAMQP.getReaderNumber());

            try {
                readerService.update(readerViewAMQP);
                System.out.println(" [x] [Sync] Reader update synchronized successfully");
            } catch (Exception e) {
                System.out.println(" [x] [Sync] Reader does not exist or version mismatch");
            }
        } catch (Exception ex) {
            System.out.println(" [x] [Sync] Error receiving Reader updated event: " + ex.getMessage());
        }
    }

    @RabbitListener(queues = "#{readerDeletedQueue.name}")
    public void receiveReaderDeleted(String in) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(in.getBytes(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] [Sync] Received Reader Deleted - Reader Number: " + readerViewAMQP.getReaderNumber());

            try {
                readerService.delete(readerViewAMQP);
                System.out.println(" [x] [Sync] Reader deletion synchronized successfully");
            } catch (Exception e) {
                System.out.println(" [x] [Sync] Reader does not exist, no deletion needed");
            }
        } catch (Exception ex) {
            System.out.println(" [x] [Sync] Error receiving Reader deleted event: " + ex.getMessage());
        }
    }

    // ========================================
    // Lending SAGA: Reader Validation
    // ========================================

    @RabbitListener(queues = "#{readerLendingRequestQueue.name}")
    public void receiveReaderLendingRequest(Message msg) {
        SagaCreationResponse response = new SagaCreationResponse();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ReaderSagaViewAMQP readerSagaViewAMQP = objectMapper.readValue(jsonReceived, ReaderSagaViewAMQP.class);

            System.out.println(" [x] [Lending-SAGA] Received Reader validation request for lending: " + readerSagaViewAMQP.getLendingNumber());

            ReaderViewAMQP readerViewAMQP = readerViewAMQPMapper.toReaderViewAMQP(readerSagaViewAMQP);
            ReaderDetails readerDetails = null;

            try {
                readerDetails = readerService.create(readerViewAMQP);
                System.out.println(" [x] [Lending-SAGA] Reader created/validated for lending");

                response.setLendingNumber(readerSagaViewAMQP.getLendingNumber());
                response.setStatus("SUCCESS");
                readerEventPublisher.sendReaderLendingResponse(response);

                if (readerDetails != null) {
                    readerEventPublisher.sendReaderCreated(readerDetails);
                }
            } catch (Exception e) {
                System.out.println(" [x] [Lending-SAGA] Error validating Reader: " + e.getMessage());
                response.setStatus("ERROR");
                response.setLendingNumber(readerSagaViewAMQP.getLendingNumber());
                response.setError(e.getMessage());
                readerEventPublisher.sendReaderLendingResponse(response);
            }

        } catch (Exception ex) {
            System.out.println(" [x] [Lending-SAGA] Error processing lending request: " + ex.getMessage());
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private void updatePendingRequestAndTryFinalize(String readerNumber, boolean isUserPending, boolean isReaderPending) {
        int maxRetries = 5;  // Increased retries for better reliability
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                Optional<PendingReaderUserRequest> pendingRequestOpt =
                    pendingReaderUserRequestRepository.findByReaderNumber(readerNumber);

                if (pendingRequestOpt.isEmpty()) {
                    System.out.println(" [x] ⚠️ No pending request found for reader number: " + readerNumber);
                    return;
                }

                PendingReaderUserRequest pendingRequest = pendingRequestOpt.get();

                // Skip if already completed
                if (pendingRequest.getStatus() == PendingReaderUserRequest.RequestStatus.READER_USER_CREATED) {
                    System.out.println(" [x] ✅ [SAGA-Skip] Already completed for: " + readerNumber);
                    return;
                }

                // Mark what was received (using OR to preserve existing values)
                if (isUserPending) {
                    pendingRequest.setUserPendingReceived(true);
                    System.out.println(" [x] User pending ✓");
                }
                if (isReaderPending) {
                    pendingRequest.setReaderPendingReceived(true);
                    System.out.println(" [x] Reader pending ✓");
                }

                // CRITICAL: Only transition to BOTH_PENDING_CREATED if BOTH flags are actually true
                // Re-fetch current state to ensure we have latest values from DB
                boolean bothReceived = pendingRequest.isUserPendingReceived() && pendingRequest.isReaderPendingReceived();

                if (bothReceived) {
                    pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.BOTH_PENDING_CREATED);
                    System.out.println(" [x] ✅ Both User and Reader confirmed → BOTH_PENDING_CREATED");
                } else {
                    System.out.println(" [x] ⏳ Waiting for " +
                        (!pendingRequest.isUserPendingReceived() ? "User" : "Reader") + " confirmation...");
                }

                pendingReaderUserRequestRepository.save(pendingRequest);

                // Try to finalize ONLY if both are received
                if (bothReceived) {
                    tryCreateReaderAndUser(readerNumber);
                }
                break; // Success

            } catch (OptimisticLockingFailureException e) {
                if (attempt < maxRetries - 1) {
                    System.out.println(" [x] ⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                    try {
                        // Exponential backoff: 50ms, 100ms, 200ms, 400ms, 800ms
                        Thread.sleep(50 * (1L << attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                    // Don't throw - another replica might succeed
                    return;
                }
            }
        }
    }

    private void tryCreateReaderAndUser(String readerNumber) {
        try {
            Optional<PendingReaderUserRequest> pendingRequestOpt =
                pendingReaderUserRequestRepository.findByReaderNumber(readerNumber);

            if (pendingRequestOpt.isEmpty()) {
                return;
            }

            PendingReaderUserRequest pendingRequest = pendingRequestOpt.get();

            // Skip if already completed or failed
            if (pendingRequest.getStatus() == PendingReaderUserRequest.RequestStatus.READER_USER_CREATED) {
                System.out.println(" [x] [SAGA-Skip] Already completed for: " + readerNumber);
                return;
            }

            if (pendingRequest.getStatus() == PendingReaderUserRequest.RequestStatus.FAILED) {
                System.out.println(" [x] [SAGA-Skip] Already failed for: " + readerNumber);
                return;
            }

            if (pendingRequest.getStatus() == PendingReaderUserRequest.RequestStatus.BOTH_PENDING_CREATED) {
                System.out.println(" [x] [SAGA-Step4] Finalizing Reader+User creation for: " + readerNumber);

                Optional<ReaderDetails> existingReader = readerRepository.findByReaderNumber(readerNumber);
                if (existingReader.isPresent()) {
                    // Mark SAGA as completed FIRST
                    pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.READER_USER_CREATED);
                    pendingReaderUserRequestRepository.save(pendingRequest);
                    System.out.println(" [x] ✅ [SAGA-Step4] Marked as completed");

                    // Notify other services
                    ReaderDetails reader = existingReader.get();
                    readerEventPublisher.sendReaderCreated(reader);
                    System.out.println(" [x] ✅ [SAGA-Complete] Reader and User successfully created: " + readerNumber);

                    // Cleanup in a separate try-catch to prevent cleanup errors from affecting the SAGA
                    try {
                        pendingReaderUserRequestRepository.delete(pendingRequest);
                        System.out.println(" [x] ✅ [SAGA-Cleanup] Removed pending request");
                    } catch (Exception cleanupEx) {
                        System.out.println(" [x] ⚠️ [SAGA-Cleanup] Could not delete pending request (may have been deleted already): " + cleanupEx.getMessage());
                    }
                } else {
                    System.out.println(" [x] ❌ [SAGA-Error] Reader entity not found: " + readerNumber);
                    pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.FAILED);
                    pendingRequest.setErrorMessage("Reader entity not found after both pending created");
                    pendingReaderUserRequestRepository.save(pendingRequest);
                }
            }
        } catch (Exception e) {
            System.out.println(" [x] ❌ [SAGA-Error] Finalization failed: " + e.getMessage());
            e.printStackTrace();

            // Only try to update status if it's not an optimistic locking error on cleanup
            if (!(e instanceof org.springframework.orm.ObjectOptimisticLockingFailureException)) {
                try {
                    Optional<PendingReaderUserRequest> pendingRequestOpt =
                        pendingReaderUserRequestRepository.findByReaderNumber(readerNumber);
                    if (pendingRequestOpt.isPresent()) {
                        PendingReaderUserRequest pendingRequest = pendingRequestOpt.get();
                        if (pendingRequest.getStatus() != PendingReaderUserRequest.RequestStatus.READER_USER_CREATED) {
                            pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.FAILED);
                            pendingRequest.setErrorMessage("Error during finalization: " + e.getMessage());
                            pendingReaderUserRequestRepository.save(pendingRequest);
                        }
                    }
                } catch (Exception cleanupEx) {
                    System.out.println(" [x] ⚠️ Error during error handling: " + cleanupEx.getMessage());
                }
            } else {
                System.out.println(" [x] ⚠️ Optimistic locking conflict (likely cleanup race condition), ignoring");
            }
        }
    }

    private pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest createTempCreateReaderRequest(
            ReaderUserRequestedEvent event) {
        pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest request =
            new pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest();
        request.setUsername(event.getUsername());
        request.setPassword(event.getPassword());
        request.setFullName(event.getFullName());
        request.setBirthDate(event.getBirthDate());
        request.setPhoneNumber(event.getPhoneNumber());
        request.setGdpr(event.isGdpr());
        request.setMarketing(event.isMarketing());
        request.setThirdParty(event.isThirdParty());
        return request;
    }
}
