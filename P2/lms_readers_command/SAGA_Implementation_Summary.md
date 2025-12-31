# SAGA Implementation for Reader and User Creation

## Overview
This SAGA pattern implementation creates both a Reader and User entity in a coordinated, asynchronous manner. It follows the choreography-based SAGA pattern where services communicate through events.

## Components Created/Modified

### 1. Event Classes
- `ReaderUserRequestedEvent.java` - Initial request event
- `UserPendingCreated.java` - Event when User is temporarily created
- `ReaderPendingCreated.java` - Event when Reader is temporarily created

### 2. Request/Response Classes
- `CreateReaderWithUserRequest.java` - HTTP request for SAGA endpoint
- `PendingReaderUserRequest.java` - Entity to track SAGA state

### 3. Repository
- `PendingReaderUserRequestRepository.java` - Repository for SAGA state management

### 4. Controllers
- `ReaderController.java` - Added `/create-complete` endpoint
- `ReaderRabbitmqController.java` - Handles SAGA orchestration
- `UserRabbitmqController.java` - Handles User creation in SAGA

### 5. Listeners
- `ReaderCreationListener.java` - Handles Reader creation in SAGA

### 6. Publishers
- `ReaderEventPublisher.java` - Enhanced to support SAGA events
- `UserEventPublisher.java` - New publisher for User events

### 7. Services
- `ReaderService.java` - Added `createWithUser()` method
- `ReaderServiceImpl.java` - Implemented SAGA logic

### 8. Configuration
- `RabbitMQConfig.java` - Added queues and bindings for SAGA

## SAGA Flow

### Step 1: Initial Request
```
POST /api/readers/create-complete
```
- Controller receives `CreateReaderWithUserRequest`
- Generates reader number
- Creates `ReaderUserRequestedEvent`
- Calls `readerService.createWithUser()`

### Step 2: SAGA Initiation
- `ReaderServiceImpl.createWithUser()`:
  - Saves `PendingReaderUserRequest` with status `PENDING_USER_CREATION`
  - Publishes `ReaderUserRequestedEvent` to both user and reader queues
  - Returns null (HTTP 202 Accepted)

### Step 3: Parallel Processing
**User Side (`UserRabbitmqController`):**
- Receives event from `reader.user.requested.user` queue
- Creates temporary User entity
- Publishes `UserPendingCreated` event

**Reader Side (`ReaderCreationListener`):**
- Receives event from `reader.user.requested.reader` queue  
- Validates forbidden names
- Creates temporary ReaderDetails entity
- Publishes `ReaderPendingCreated` event

### Step 4: SAGA Coordination
`ReaderRabbitmqController` receives both pending events:
- Updates `PendingReaderUserRequest` flags
- When both received, sets status to `BOTH_PENDING_CREATED`
- Calls `tryCreateReaderAndUser()` for finalization

### Step 5: Finalization
- Verifies both entities exist
- Sets status to `READER_USER_CREATED`
- Sends final `ReaderCreated` event
- Cleans up pending request

## States
```
PENDING_USER_CREATION → PENDING_READER_CREATION → BOTH_PENDING_CREATED → READER_USER_CREATED
                                                                       ↓
                                                                    FAILED
```

## Error Handling
- Optimistic locking with retry logic
- Failed states for compensation
- Error messages stored in pending requests
- Automatic cleanup after success

## Usage
```bash
curl -X POST /api/readers/create-complete \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe@example.com",
    "password": "password123",
    "fullName": "John Doe",
    "birthDate": "1990-01-01",
    "phoneNumber": "+1234567890",
    "gdpr": true,
    "marketing": false,
    "thirdParty": false
  }'
```

## Response
- **202 Accepted**: SAGA initiated, processing asynchronously
- **201 Created**: Reader already exists, returns immediately
- **409 Conflict**: Username already exists or other conflict

This implementation provides a robust, distributed transaction mechanism for creating coordinated Reader and User entities across the microservice architecture.
