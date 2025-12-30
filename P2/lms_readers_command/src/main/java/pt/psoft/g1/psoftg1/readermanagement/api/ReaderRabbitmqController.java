package pt.psoft.g1.psoftg1.readermanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.readermanagement.model.PendingReaderUserRequest;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.repositories.PendingReaderUserRequestRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.usermanagement.api.UserPendingCreated;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReaderRabbitmqController {

    private final PendingReaderUserRequestRepository pendingReaderUserRequestRepository;
    private final ReaderRepository readerRepository;
    private final ReaderService readerService;
    private final ReaderEventPublisher readerEventPublisher;

    @RabbitListener(queues = "user.pending.created")
    public void receiveUserPendingCreated(String jsonReceived) {
        try {
            System.out.println(" [x] Received message from user.pending.created: " + jsonReceived);

            ObjectMapper objectMapper = new ObjectMapper();
            UserPendingCreated event = objectMapper.readValue(jsonReceived, UserPendingCreated.class);

            System.out.println(" [x] Received User Pending Created by AMQP:");
            System.out.println("     - Reader Number: " + event.getReaderNumber());
            System.out.println("     - User ID: " + event.getUserId());
            System.out.println("     - Username: " + event.getUsername());

            // Retry logic to handle race conditions
            int maxRetries = 3;
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    // Update pending request status
                    Optional<PendingReaderUserRequest> pendingRequestOpt = pendingReaderUserRequestRepository.findByReaderNumber(event.getReaderNumber());
                    if (pendingRequestOpt.isPresent()) {
                        PendingReaderUserRequest pendingRequest = pendingRequestOpt.get();

                        // Mark user as received (order-independent)
                        pendingRequest.setUserPendingReceived(true);

                        // Check if BOTH are now received
                        if (pendingRequest.isUserPendingReceived() && pendingRequest.isReaderPendingReceived()) {
                            pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.BOTH_PENDING_CREATED);
                            System.out.println("Both User and Reader pending received → BOTH_PENDING_CREATED");
                        } else {
                            System.out.println("User pending received, waiting for Reader pending...");
                        }

                        pendingReaderUserRequestRepository.save(pendingRequest);
                        System.out.println(" [x] Updated pending request status to " + pendingRequest.getStatus() + " for reader number: " + event.getReaderNumber());

                        // Try to create reader and user if both are ready
                        tryCreateReaderAndUser(event.getReaderNumber());
                    } else {
                        System.out.println("No pending request found for reader number: " + event.getReaderNumber());
                    }

                    // Success, break out of retry loop
                    break;

                } catch (OptimisticLockingFailureException e) {
                    if (attempt < maxRetries - 1) {
                        System.out.println("⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                        Thread.sleep(50); // Small delay before retry
                    } else {
                        System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                        throw e;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(" [x] ❌ Error processing user pending created event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = "reader.pending.created")
    public void receiveReaderPendingCreated(String jsonReceived) {
        try {
            System.out.println(" [x] Received message from reader.pending.created: " + jsonReceived);

            ObjectMapper objectMapper = new ObjectMapper();
            ReaderPendingCreated event = objectMapper.readValue(jsonReceived, ReaderPendingCreated.class);

            System.out.println(" [x] Received Reader Pending Created by AMQP:");
            System.out.println("     - Reader Number: " + event.getReaderNumber());
            System.out.println("     - Username: " + event.getUsername());

            // Retry logic to handle race conditions
            int maxRetries = 3;
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    // Update pending request status
                    Optional<PendingReaderUserRequest> pendingRequestOpt = pendingReaderUserRequestRepository.findByReaderNumber(event.getReaderNumber());
                    if (pendingRequestOpt.isPresent()) {
                        PendingReaderUserRequest pendingRequest = pendingRequestOpt.get();

                        // Mark reader as received (order-independent)
                        pendingRequest.setReaderPendingReceived(true);

                        // Check if BOTH are now received
                        if (pendingRequest.isUserPendingReceived() && pendingRequest.isReaderPendingReceived()) {
                            pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.BOTH_PENDING_CREATED);
                            System.out.println("Both User and Reader pending received → BOTH_PENDING_CREATED");
                        } else {
                            System.out.println("Reader pending received, waiting for User pending...");
                        }

                        pendingReaderUserRequestRepository.save(pendingRequest);
                        System.out.println(" [x] Updated pending request status to " + pendingRequest.getStatus() + " for reader number: " + event.getReaderNumber());

                        // Try to create reader and user if both are ready
                        tryCreateReaderAndUser(event.getReaderNumber());
                    } else {
                        System.out.println("No pending request found for reader number: " + event.getReaderNumber());
                    }

                    // Success, break out of retry loop
                    break;

                } catch (OptimisticLockingFailureException e) {
                    if (attempt < maxRetries - 1) {
                        System.out.println("⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                        Thread.sleep(50); // Small delay before retry
                    } else {
                        System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                        throw e;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(" [x] ❌ Error processing reader pending created event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void tryCreateReaderAndUser(String readerNumber) {
        try {
            Optional<PendingReaderUserRequest> pendingRequestOpt = pendingReaderUserRequestRepository.findByReaderNumber(readerNumber);
            if (pendingRequestOpt.isPresent()) {
                PendingReaderUserRequest pendingRequest = pendingRequestOpt.get();

                // Check if both temporary entities are created and ready for finalization
                if (pendingRequest.getStatus() == PendingReaderUserRequest.RequestStatus.BOTH_PENDING_CREATED) {
                    System.out.println(" [x] ✅ Both User and Reader pending created for reader number: " + readerNumber + " - Ready to proceed to finalization");

                    // Since we already have the temporary entities created, we just need to mark them as finalized
                    // and update the status to completed
                    Optional<ReaderDetails> existingReader = readerRepository.findByReaderNumber(readerNumber);
                    if (existingReader.isPresent()) {
                        // Mark the SAGA as completed
                        pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.READER_USER_CREATED);
                        pendingReaderUserRequestRepository.save(pendingRequest);

                        // Send final events to notify other services
                        ReaderDetails reader = existingReader.get();
                        readerEventPublisher.sendReaderCreated(reader);

                        System.out.println(" [x] ✅ SAGA completed successfully - Reader and User created with reader number: " + readerNumber);

                        // Cleanup - remove the pending request after successful completion
                        pendingReaderUserRequestRepository.delete(pendingRequest);
                        System.out.println(" [x] ✅ Cleaned up pending request for reader number: " + readerNumber);
                    } else {
                        System.out.println(" [x] ❌ Reader entity not found for reader number: " + readerNumber);
                        // Mark as failed
                        pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.FAILED);
                        pendingRequest.setErrorMessage("Reader entity not found after both pending created");
                        pendingReaderUserRequestRepository.save(pendingRequest);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(" [x] ❌ Error in tryCreateReaderAndUser: " + e.getMessage());
            e.printStackTrace();

            // Try to mark as failed if we can find the pending request
            try {
                Optional<PendingReaderUserRequest> pendingRequestOpt = pendingReaderUserRequestRepository.findByReaderNumber(readerNumber);
                if (pendingRequestOpt.isPresent()) {
                    PendingReaderUserRequest pendingRequest = pendingRequestOpt.get();
                    pendingRequest.setStatus(PendingReaderUserRequest.RequestStatus.FAILED);
                    pendingRequest.setErrorMessage("Error during finalization: " + e.getMessage());
                    pendingReaderUserRequestRepository.save(pendingRequest);
                }
            } catch (Exception cleanupEx) {
                System.out.println(" [x] ❌ Error during cleanup: " + cleanupEx.getMessage());
            }
        }
    }
}
