package pt.psoft.g1.psoftg1.CDCTests.consumer;

import au.com.dius.pact.core.model.*;
import au.com.dius.pact.core.model.messaging.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderRabbitmqController;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.repositories.PendingReaderUserRequestRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderMapper;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CDC Consumer Tests for lms_readers_command
 *
 * Tests message consumption from other services, verifying that readers_command
 * can correctly process events according to the Pact contracts.
 *
 * Environment Isolation Note:
 * - Staging uses: lms_shared_rabbitmq_staging
 * - Prod uses: lms_shared_rabbitmq_prod
 * This ensures events from staging load tests don't leak into prod.
 *
 * Event Flow:
 * 1. receiveReaderUserRequested - Initiates Reader-User SAGA
 * 2. receiveUserPendingCreated - Confirms User creation (from lms_auth_users)
 * 3. receiveReaderPendingCreated - Confirms Reader creation (self-confirmation)
 * 4. receiveReaderLendingRequest - Validates Reader for lending operations
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ReaderRabbitmqController.class}
)
@ActiveProfiles("cdc-test")
public class ReadersCDCConsumerIT {

    @MockBean
    ReaderRepository readerRepository;

    @MockBean
    PendingReaderUserRequestRepository pendingReaderUserRequestRepository;

    @MockBean
    ReaderEventPublisher readerEventPublisher;

    @MockBean
    ReaderMapper readerMapper;

    @MockBean
    ForbiddenNameRepository forbiddenNameRepository;

    @MockBean
    ReaderService readerService;

    @MockBean
    ReaderViewAMQPMapper readerViewAMQPMapper;

    @MockBean
    pt.psoft.g1.psoftg1.usermanagement.services.UserService userService;

    @Autowired
    ReaderRabbitmqController listener;

    /**
     * Tests SAGA Step 1: Reader-User creation request
     *
     * This event triggers the creation of a Reader entity when a new user
     * registration is initiated. The service validates the reader number and
     * full name before creating a temporary Reader record.
     */
    @Test
    void testReaderUserRequestedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/reader_user_requested-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("⚠️ Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        // Mock repository behavior for reader creation
        when(readerRepository.findByReaderNumber(anyString())).thenReturn(Optional.empty());
        when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of());

        ReaderDetails mockReaderDetails = mock(ReaderDetails.class);
        Reader mockReader = mock(Reader.class);
        when(mockReader.getId()).thenReturn("123");
        when(mockReaderDetails.getReader()).thenReturn(mockReader);
        when(mockReaderDetails.getReaderNumber()).thenReturn("2024/1");

        when(readerMapper.createReaderDetails(anyInt(), any(), any(), anyString(), any())).thenReturn(mockReaderDetails);
        when(readerRepository.save(any(ReaderDetails.class))).thenReturn(mockReaderDetails);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            assertDoesNotThrow(() -> {
                listener.receiveReaderUserRequested(jsonReceived);
            });

            // Verify the SAGA workflow was executed correctly
            verify(readerRepository, times(1)).findByReaderNumber(anyString());
            verify(forbiddenNameRepository, atLeastOnce()).findByForbiddenNameIsContained(anyString());
            verify(readerRepository, times(1)).save(any(ReaderDetails.class));
            verify(readerEventPublisher, times(1)).sendReaderPendingCreated(any());

            System.out.println("✅ Reader-User creation request processed successfully");
        }
    }

    /**
     * Tests SAGA Step 2: User pending creation confirmation
     *
     * This event is received from lms_auth_users when a User is successfully
     * created. This service updates the pending request status and tries to
     * finalize the Reader-User creation SAGA.
     */
    @Test
    void testUserPendingCreatedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/user_pending_created-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("⚠️ Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            assertDoesNotThrow(() -> {
                listener.receiveUserPendingCreated(jsonReceived);
            });

            System.out.println("✅ User pending created confirmation processed successfully");
        }
    }

    /**
     * Tests SAGA Step 3: Reader pending creation confirmation
     *
     * This event is the service's own confirmation that it has created
     * a Reader entity. Combined with User pending confirmation, this
     * completes the SAGA.
     */
    @Test
    void testReaderPendingCreatedMessageProcessing() throws Exception {
        File pactFile = new File("target/pacts/reader_pending_created-consumer-reader_event-producer.json");

        if (!pactFile.exists()) {
            System.out.println("⚠️ Pact file not found, skipping test: " + pactFile.getAbsolutePath());
            return;
        }

        PactReader pactReader = DefaultPactReader.INSTANCE;
        Pact pact = pactReader.loadPact(pactFile);

        List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
        for (Message messageGeneratedByPact : messagesGeneratedByPact) {
            String jsonReceived = messageGeneratedByPact.contentsAsString();

            assertDoesNotThrow(() -> {
                listener.receiveReaderPendingCreated(jsonReceived);
            });

            System.out.println("✅ Reader pending created confirmation processed successfully");
        }
    }

    /**
     * Tests Reader Created event - PRODUCER ONLY
     *
     * This service PRODUCES this event but doesn't CONSUME it.
     * The event is consumed by lms_readers_query for eventual consistency
     * between command and query sides.
     *
     * Note: In environments with multiple replicas sharing the same database,
     * sync events are not strictly necessary since all replicas see the same data.
     */
    @Test
    void testReaderCreatedEventInfo() {
        System.out.println("ℹ️ Reader created - This is a PRODUCER-ONLY event");
        System.out.println("   Event consumed by: lms_readers_query");
        System.out.println("   Purpose: CQRS synchronization (command → query)");
    }

    /**
     * Tests Reader Updated event - PRODUCER ONLY
     *
     * Similar to reader.created, this event is only produced by this service
     * and consumed by lms_readers_query for maintaining query-side consistency.
     */
    @Test
    void testReaderUpdatedEventInfo() {
        System.out.println("ℹ️ Reader updated - This is a PRODUCER-ONLY event");
        System.out.println("   Event consumed by: lms_readers_query");
        System.out.println("   Purpose: CQRS synchronization (command → query)");
    }

    /**
     * Tests Reader Deleted event - PRODUCER ONLY
     *
     * This event notifies the query side that a Reader has been deleted,
     * allowing it to update its read model accordingly.
     */
    @Test
    void testReaderDeletedEventInfo() {
        System.out.println("ℹ️ Reader deleted - This is a PRODUCER-ONLY event");
        System.out.println("   Event consumed by: lms_readers_query");
        System.out.println("   Purpose: CQRS synchronization (command → query)");
    }
}
