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
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Centralized RabbitMQ Listener for Reader-related events
 *
 * Handles:
 * 1. SAGA: Reader + User creation (reader.user.requested.reader)
 * 2. SAGA Coordination: User and Reader pending confirmations
 * 3. Lending SAGA: Reader validation for lending operations
 *
 * NOTE: Handles sync events (reader.created, reader.updated, reader.deleted) for
 * cross-service data synchronization and validation requests.
 */
@Component
@RequiredArgsConstructor
public class ReaderRabbitmqController {

    private final PendingReaderUserRequestRepository pendingReaderUserRequestRepository;
    private final ReaderRepository readerRepository;
    private final ReaderEventPublisher readerEventPublisher;
    private final ReaderMapper readerMapper;
    private final ForbiddenNameRepository forbiddenNameRepository;
    private final ReaderService readerService;
    private final ReaderViewAMQPMapper readerViewAMQPMapper;
    private final UserService userService;

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
            System.out.println("     - Full Name: " + event.getFullName());

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

            // Create Reader user with name using factory method
            Reader user = Reader.newReader(event.getUsername(), event.getPassword(), event.getFullName());

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
    // Synchronization Events
    // ========================================

    /**
     * Listens to reader.created events from other microservices
     * Synchronizes reader creation across the distributed system
     */
    @RabbitListener(queues = "#{readerCreatedQueue.name}")
    public void receiveReaderCreated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] [READERS-COMMAND] Received Reader Created by AMQP: " + jsonReceived);
            try {
                // Process reader creation event using ReaderService
                readerService.create(readerViewAMQP);
                System.out.println(" [x] [READERS-COMMAND] ✅ Reader created event processed successfully");
            } catch (Exception e) {
                System.out.println(" [x] [READERS-COMMAND] ❌ Error processing reader created event: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(" [x] [READERS-COMMAND] ❌ Exception receiving reader created event from AMQP: " + ex.getMessage());
        }
    }

    /**
     * Listens to reader.updated events from other microservices
     * Synchronizes reader updates across the distributed system
     */
    @RabbitListener(queues = "#{readerUpdatedQueue.name}")
    public void receiveReaderUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] [READERS-COMMAND] Received Reader Updated by AMQP: " + jsonReceived);
            try {
                // Process reader update event using ReaderService
                readerService.update(readerViewAMQP);
                System.out.println(" [x] [READERS-COMMAND] ✅ Reader updated event processed successfully");
            } catch (Exception e) {
                System.out.println(" [x] [READERS-COMMAND] ❌ Error processing reader updated event: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(" [x] [READERS-COMMAND] ❌ Exception receiving reader updated event from AMQP: " + ex.getMessage());
        }
    }

    /**
     * Listens to reader.deleted events from other microservices
     * Synchronizes reader deletions across the distributed system
     */
    @RabbitListener(queues = "#{readerDeletedQueue.name}")
    public void receiveReaderDeleted(String jsonReceived) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ReaderViewAMQP readerViewAMQP = objectMapper.readValue(jsonReceived, ReaderViewAMQP.class);

            System.out.println(" [x] [READERS-COMMAND] Received Reader Deleted by AMQP: " + jsonReceived);
            try {
                // Process reader deletion event using ReaderService
                readerService.delete(readerViewAMQP);
                System.out.println(" [x] [READERS-COMMAND] ✅ Reader deleted event processed successfully");
            } catch (Exception e) {
                System.out.println(" [x] [READERS-COMMAND] ❌ Error processing reader deleted event: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(" [x] [READERS-COMMAND] ❌ Exception receiving reader deleted event from AMQP: " + ex.getMessage());
        }
    }

    // ========================================
    // User Synchronization Events
    // ========================================

    /**
     * Listens to user.created events from other microservices
     * Synchronizes user creation across the distributed system
     */
    @RabbitListener(queues = "#{userCreatedQueue.name}")
    public void receiveUserCreated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            UserViewAMQP userViewAMQP = objectMapper.readValue(jsonReceived, UserViewAMQP.class);

            System.out.println(" [x] [READERS-COMMAND] Received User Created by AMQP: " + jsonReceived);
            try {
                // Process user creation event using UserService
                userService.handleUserCreated(userViewAMQP);
                System.out.println(" [x] [READERS-COMMAND] ✅ User created event processed successfully");
            } catch (Exception e) {
                System.out.println(" [x] [READERS-COMMAND] ❌ Error processing user created event: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(" [x] [READERS-COMMAND] ❌ Exception receiving user created event from AMQP: " + ex.getMessage());
        }
    }

    /**
     * Listens to user.updated events from other microservices
     * Synchronizes user updates across the distributed system
     */
    @RabbitListener(queues = "#{userUpdatedQueue.name}")
    public void receiveUserUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            UserViewAMQP userViewAMQP = objectMapper.readValue(jsonReceived, UserViewAMQP.class);

            System.out.println(" [x] [READERS-COMMAND] Received User Updated by AMQP: " + jsonReceived);
            try {
                // Process user update event using UserService
                userService.handleUserUpdated(userViewAMQP);
                System.out.println(" [x] [READERS-COMMAND] ✅ User updated event processed successfully");
            } catch (Exception e) {
                System.out.println(" [x] [READERS-COMMAND] ❌ Error processing user updated event: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(" [x] [READERS-COMMAND] ❌ Exception receiving user updated event from AMQP: " + ex.getMessage());
        }
    }

    /**
     * Listens to user.deleted events from other microservices
     * Synchronizes user deletions across the distributed system
     */
    @RabbitListener(queues = "#{userDeletedQueue.name}")
    public void receiveUserDeleted(String jsonReceived) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            UserViewAMQP userViewAMQP = objectMapper.readValue(jsonReceived, UserViewAMQP.class);

            System.out.println(" [x] [READERS-COMMAND] Received User Deleted by AMQP: " + jsonReceived);
            try {
                // For user deletion, we might want to deactivate related readers
                // The exact implementation depends on business requirements
                System.out.println(" [x] [READERS-COMMAND] User deleted event for: " + userViewAMQP.getUsername());
                System.out.println(" [x] [READERS-COMMAND] ✅ User deleted event processed successfully");
            } catch (Exception e) {
                System.out.println(" [x] [READERS-COMMAND] ❌ Error processing user deleted event: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(" [x] [READERS-COMMAND] ❌ Exception receiving user deleted event from AMQP: " + ex.getMessage());
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private void updatePendingRequestAndTryFinalize(String readerNumber, boolean isUserPending, boolean isReaderPending) {
        int maxRetries = 5;
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

                // Mark what was received
                if (isUserPending) {
                    pendingRequest.setUserCreatedReceived(true);
                    System.out.println(" [x] User confirmed ✓");
                }
                if (isReaderPending) {
                    pendingRequest.setReaderCreatedReceived(true);
                    System.out.println(" [x] Reader confirmed ✓");
                }

                boolean bothReceived = pendingRequest.isUserCreatedReceived() && pendingRequest.isReaderCreatedReceived();

                if (bothReceived) {
                    pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.READER_USER_CREATED);
                    System.out.println(" [x] ✅ Both User and Reader confirmed → READER_USER_CREATED");
                } else {
                    System.out.println(" [x] ⏳ Waiting for " +
                        (!pendingRequest.isUserCreatedReceived() ? "User" : "Reader") + " confirmation...");
                }

                pendingReaderUserRequestRepository.save(pendingRequest);

                if (bothReceived) {
                    tryCreateReaderAndUser(readerNumber);
                }
                break;

            } catch (OptimisticLockingFailureException e) {
                if (attempt < maxRetries - 1) {
                    System.out.println(" [x] ⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                    try {
                        Thread.sleep(50 * (1L << attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
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

            if (pendingRequest.getStatus() == PendingReaderUserRequest.RequestStatus.READER_USER_CREATED) {
                System.out.println(" [x] [SAGA-Complete] Reader and User successfully created: " + readerNumber);

                // Verify reader exists in database
                Optional<ReaderDetails> existingReader = readerRepository.findByReaderNumber(readerNumber);
                if (existingReader.isPresent()) {
                    // Send reader.created event to notify other services (e.g., lms_lendings_command)
                    ReaderDetails reader = existingReader.get();
                    readerEventPublisher.sendReaderCreated(reader);
                    System.out.println(" [x] ✅ [SAGA-Sync] Sent reader.created event for: " + readerNumber);

                    // Cleanup pending request
                    try {
                        pendingReaderUserRequestRepository.delete(pendingRequest);
                        System.out.println(" [x] ✅ [SAGA-Cleanup] Removed pending request");
                    } catch (Exception cleanupEx) {
                        System.out.println(" [x] ⚠️ [SAGA-Cleanup] Could not delete pending request: " + cleanupEx.getMessage());
                    }
                } else {
                    System.out.println(" [x] ❌ [SAGA-Error] Reader entity not found: " + readerNumber);
                    pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.FAILED);
                    pendingRequest.setErrorMessage("Reader entity not found after confirmations received");
                    pendingReaderUserRequestRepository.save(pendingRequest);
                }
            }

            if (pendingRequest.getStatus() == PendingReaderUserRequest.RequestStatus.FAILED) {
                System.out.println(" [x] [SAGA-Skip] Already failed for: " + readerNumber);
            }

        } catch (Exception e) {
            System.out.println(" [x] ❌ [SAGA-Error] Finalization failed: " + e.getMessage());
            e.printStackTrace();

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
