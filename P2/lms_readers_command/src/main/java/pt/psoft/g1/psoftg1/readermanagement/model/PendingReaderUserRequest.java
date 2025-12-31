package pt.psoft.g1.psoftg1.readermanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PendingReaderUserRequest")
@NoArgsConstructor
@Getter
@Setter
public class PendingReaderUserRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String readerNumber;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String birthDate;

    @Column(nullable = false)
    private String phoneNumber;

    @Column
    private String photoURI;

    @Column(nullable = false)
    private boolean gdpr;

    @Column(nullable = false)
    private boolean marketing;

    @Column(nullable = false)
    private boolean thirdParty;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    // Flags to track received events (order-independent)
    @Column(nullable = false)
    private boolean userPendingReceived = false;

    @Column(nullable = false)
    private boolean readerPendingReceived = false;

    @Column(nullable = false)
    private boolean userFinalizedReceived = false;

    @Column(nullable = false)
    private boolean readerFinalizedReceived = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(length = 1000)
    private String errorMessage;

    @Version
    private Long version;

    public enum RequestStatus {
        PENDING_USER_CREATION,         // Initial state: waiting for user pending event
        PENDING_READER_CREATION,       // User pending received, waiting for reader pending
        USER_CREATED,                  // Only used if user comes before reader (edge case)
        BOTH_PENDING_CREATED,          // Both temporary entities created (finalized=false) - READY TO TRIGGER FINALIZATION
        USER_FINALIZED,                // User finalized, waiting for reader finalization
        READER_FINALIZED,              // Reader finalized, waiting for user finalization
        BOTH_FINALIZED,                // Both entities finalized (finalized=true) - READY TO COMPLETE CREATION
        READER_USER_CREATED,           // Reader and User successfully created with finalized entities
        FAILED                         // Saga compensation - creation aborted
    }

    public PendingReaderUserRequest(String readerNumber, String username, String password, String fullName,
                                   String birthDate, String phoneNumber, String photoURI,
                                   boolean gdpr, boolean marketing, boolean thirdParty) {
        this.readerNumber = readerNumber;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.photoURI = photoURI;
        this.gdpr = gdpr;
        this.marketing = marketing;
        this.thirdParty = thirdParty;
        this.requestedAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING_USER_CREATION;
    }
}
