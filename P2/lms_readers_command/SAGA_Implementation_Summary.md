# SAGA Implementation for Reader and User Creation

## Overview
This SAGA pattern implementation creates both a Reader and User entity in a coordinated, asynchronous manner using **choreography-based SAGA**. Services communicate through RabbitMQ events, with the `lms_readers_command` service acting as the coordinator.

## Architecture

### Services Involved
1. **lms_readers_command**: SAGA coordinator + Reader entity creation
2. **lms_auth_users**: User entity creation

### Key Principle
**All replicas share the same database**, so sync events (reader.created, user.created, etc.) are NOT needed for replication. Events are only used for SAGA coordination.

## Components Created/Modified

### 1. Event Classes
- `ReaderUserRequestedEvent.java` - Initial SAGA trigger event
- `UserPendingCreated.java` - Confirmation that User was created
- `ReaderPendingCreated.java` - Confirmation that Reader was created

### 2. Request/Response Classes
- `CreateReaderWithUserRequest.java` - HTTP request for SAGA endpoint
- `PendingReaderUserRequest.java` - Entity to track SAGA state with optimistic locking

### 3. Repository
- `PendingReaderUserRequestRepository.java` - Repository for SAGA state management

### 4. Controllers
- **ReaderController.java** - Added `/create-complete` endpoint (HTTP POST)
- **ReaderRabbitmqController.java** - SAGA coordination + Reader creation listener
- **UserRabbitmqController.java** (in lms_auth_users) - User creation listener

### 5. Publishers
- `ReaderEventPublisher.java` - Publishes SAGA events (request, pending, created)
- `UserEventPublisher.java` - Publishes User pending event

### 6. Services
- `ReaderService.java` - Added `createWithUser()` method
- `ReaderServiceImpl.java` - Implemented SAGA initiation logic with duplicate detection

### 7. Configuration
- `RabbitMQConfig.java` (lms_readers_command) - Durable named queues for SAGA with load balancing
- `RabbitMQConfig.java` (lms_auth_users) - User request queue binding

## SAGA Flow

### Step 1: HTTP Request Initiation
```
POST /api/readers/create-complete
Content-Type: application/json
{
  "username": "john.doe@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "birthDate": "1990-01-01",
  "phoneNumber": "+1234567890",
  "gdpr": true,
  "marketing": false,
  "thirdParty": false
}
```

**ReaderController** receives request and:
- Creates `ReaderUserRequestedEvent` (without reader number yet)
- Calls `readerService.createWithUser(event)`

### Step 2: SAGA Initiation (ReaderServiceImpl)

**Pre-checks** (to avoid duplicate work):
1. Check if Reader already exists by username → return existing reader (201 Created)
2. Check if pending request exists for username → return 202 if still processing
3. Check if User already exists in User table → throw ConflictException
4. Check if reader number already exists → generate new one

**If all checks pass**:
- Generate reader number: `YYYY/sequence` (e.g., "2026/1")
- Create and save `PendingReaderUserRequest` with status `WAITING_CONFIRMATIONS`
- Publish `ReaderUserRequestedEvent` to TWO queues:
  - `reader.user.requested.user` (consumed by lms_auth_users)
  - `reader.user.requested.reader` (consumed by lms_readers_command)
- Return `null` → Controller returns **HTTP 202 Accepted**

### Step 3: Parallel User Creation (lms_auth_users)

**UserRabbitmqController** listens to `reader.user.requested.user`:
1. Check if User already exists → send event anyway to allow SAGA to handle it
2. Create `Reader` (User type) with encoded password using `Reader.newReader()`
3. Save User to database
4. Publish `UserPendingCreated` event with:
   - readerNumber
   - userId
   - username
   - finalized = false

### Step 4: Parallel Reader Creation (lms_readers_command)

**ReaderRabbitmqController** listens to `reader.user.requested.reader`:
1. Check if ReaderDetails already exists → skip if found
2. Validate full name for forbidden words → abort if found
3. Create `Reader` (User) using `Reader.newReader()`
4. Create `ReaderDetails` entity with readerNumber, user, and other details
5. Save ReaderDetails to database
6. Publish `ReaderPendingCreated` event with:
   - readerNumber
   - userId
   - username
   - finalized = false

### Step 5: SAGA Coordination (ReaderRabbitmqController)

**Two listeners** receive confirmation events:

#### 5a. User Confirmation
Listens to `user.pending.created` queue:
- Calls `updatePendingRequestAndTryFinalize(readerNumber, isUserPending=true, isReaderPending=false)`

#### 5b. Reader Confirmation
Listens to `reader.pending.created` queue:
- Calls `updatePendingRequestAndTryFinalize(readerNumber, isUserPending=false, isReaderPending=true)`

**updatePendingRequestAndTryFinalize() logic**:
- Retry up to 5 times with exponential backoff (handles optimistic locking conflicts)
- Load `PendingReaderUserRequest` by readerNumber
- Skip if already `READER_USER_CREATED`
- Set flags: `userCreatedReceived` and/or `readerCreatedReceived`
- Check if **BOTH** confirmations received
- If both received → set status to `READER_USER_CREATED`
- If only one → wait for the other
- Save updated request
- If both received → call `tryCreateReaderAndUser()`

### Step 6: Finalization

**tryCreateReaderAndUser()** executes when both confirmations received:
1. Load pending request by readerNumber
2. Verify status is `READER_USER_CREATED`
3. Verify ReaderDetails exists in database
4. Publish `reader.created` event (to notify other services like lms_lendings_command)
5. Delete `PendingReaderUserRequest` (cleanup)

**Result**: Both User and ReaderDetails exist in database, SAGA complete!

## State Machine

```
WAITING_CONFIRMATIONS
         ↓
    [User confirms]  or  [Reader confirms]
         ↓                      ↓
  (userCreatedReceived)  (readerCreatedReceived)
         ↓                      ↓
         └──────────┬───────────┘
                    ↓
        BOTH confirmations received
                    ↓
         READER_USER_CREATED
                    ↓
            [Finalization]
                    ↓
          [Cleanup pending request]
```

### States (PendingReaderUserRequest.RequestStatus)
- `WAITING_CONFIRMATIONS` - Initial state, waiting for both User and Reader
- `READER_USER_CREATED` - Both confirmations received, entities created successfully
- `FAILED` - SAGA compensation/abort (currently only on errors)

**Note**: The intermediate states `USER_CONFIRMED` and `READER_CONFIRMED` are NOT used. Instead, boolean flags `userCreatedReceived` and `readerCreatedReceived` track confirmations independently.

## Concurrency & Resilience

### Optimistic Locking
- `PendingReaderUserRequest` has `@Version` field
- Prevents race conditions when multiple replicas process confirmations
- Retry logic (5 attempts) with exponential backoff handles conflicts

### Load Balancing
- **Named durable queues** ensure only ONE replica processes each message:
  - `reader.user.requested.user`
  - `reader.user.requested.reader`
  - `user.pending.created`
  - `reader.pending.created`
- Prevents duplicate work and reduces optimistic locking conflicts

### Idempotency
- Duplicate detection at multiple levels:
  - Check if Reader exists by username
  - Check if User exists by username
  - Check if pending request exists for username
  - Check if reader number already exists
- Safe to retry failed requests

### Error Handling
- Forbidden name validation → silent abort
- User already exists → ConflictException
- Optimistic locking failures → automatic retry
- Missing entities during finalization → mark as FAILED
- Failed states allow compensation logic

## Database Sharing

**Critical Detail**: All replicas of `lms_readers_command` and `lms_auth_users` share the same PostgreSQL database.

**Implications**:
- ✅ No need for `reader.created/updated/deleted` sync events between replicas
- ✅ No need for `user.created/updated/deleted` sync events between replicas
- ✅ When one replica saves a Reader, all replicas see it immediately
- ✅ When one replica saves a User, all replicas see it immediately
- ⚠️ Only use events for SAGA coordination and cross-service communication

## Response Codes

| Status Code | Meaning |
|-------------|---------|
| **202 Accepted** | SAGA initiated, processing asynchronously |
| **201 Created** | Reader already exists, returned immediately |
| **409 Conflict** | Username already exists or other conflict |

## Usage Example

```bash
curl -X POST http://localhost:8080/api/readers/create-complete \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane.smith@example.com",
    "password": "securePass456",
    "fullName": "Jane Smith",
    "birthDate": "1995-06-15",
    "phoneNumber": "+351912345678",
    "gdpr": true,
    "marketing": true,
    "thirdParty": false
  }'
```

**Expected Response (202 Accepted)**:
```json
{
  "message": "Reader and User creation request accepted",
  "status": "PROCESSING",
  "username": "jane.smith@example.com",
  "details": "The reader and user will be created asynchronously. Please check back later."
}
```

## Monitoring & Debugging

### Log Prefixes
- `[SAGA-Step1]` - Reader entity creation
- `[SAGA-Step2]` - User confirmation received
- `[SAGA-Step3]` - Reader confirmation received
- `[SAGA-Complete]` - Finalization successful
- `[SAGA-Cleanup]` - Pending request deletion
- `[SAGA-Error]` - Error during SAGA execution
- `[SAGA-Skip]` - Already completed or failed
- `[AUTH-USERS]` - User service logs

### Key Metrics to Track
- Pending requests waiting > 5 seconds
- Failed SAGA requests
- Optimistic locking retry counts
- Orphaned pending requests (neither confirmed)

## Future Enhancements

1. **Compensation Logic**: Currently minimal, could add rollback for partial failures
2. **Timeout Handling**: Add TTL to pending requests, auto-fail after X minutes
3. **Dead Letter Queue**: Handle permanently failed messages
4. **Saga Orchestration Dashboard**: Monitor pending/failed SAGAs in real-time
5. **Photo Upload**: Currently photoURI is handled but photo processing could be added

---

This choreography-based SAGA provides a robust, distributed transaction mechanism for creating coordinated Reader and User entities across the microservice architecture with proper handling of concurrency, idempotency, and resilience.
