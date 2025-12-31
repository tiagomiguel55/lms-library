# LMS Readers Command - Enhanced AMQP Implementation

This document describes the enhanced AMQP components implemented for the `lms_readers_command` module to enable comprehensive communication with other LMS microservices, particularly `lms_auth_users`.

## Components Enhanced/Created

### 1. UserViewAMQP.java (NEW)
- **Location**: `pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP`
- **Purpose**: DTO for user data received via AMQP from lms_auth_users
- **Fields**:
  - `username`: User's username
  - `fullName`: User's full name
  - `password`: User's password (encrypted)
  - `version`: Version for optimistic locking
  - `_links`: HATEOAS links (map)

### 2. UserEventListener.java (NEW)
- **Location**: `pt.psoft.g1.psoftg1.usermanagement.listeners.UserEventListener`
- **Purpose**: Listens for user-related AMQP messages from lms_auth_users
- **Listeners**:
  - `receiveUserCreated()`: Handles user creation events from lms_auth_users
  - `receiveUserUpdated()`: Handles user update events from lms_auth_users
  - `receiveUserDeleted()`: Handles user deletion events from lms_auth_users

### 3. UserEvents.java (NEW)
- **Location**: `pt.psoft.g1.psoftg1.shared.model.UserEvents`
- **Purpose**: Constants for user event routing keys
- **Constants**:
  - `USER_CREATED = "user.created"`
  - `USER_UPDATED = "user.updated"`
  - `USER_DELETED = "user.deleted"`
  - `USER_REQUESTED = "reader.user.requested.user"`

### 4. UserService.java (ENHANCED)
- **Location**: `pt.psoft.g1.psoftg1.usermanagement.services.UserService`
- **New Methods**:
  - `handleUserCreated(UserViewAMQP)`: Processes user creation events
  - `handleUserUpdated(UserViewAMQP)`: Processes user update events
  - `handleUserDeleted(String)`: Processes user deletion events

### 5. RabbitMQConfig.java (ENHANCED)
- **Location**: `pt.psoft.g1.psoftg1.configuration.RabbitMQConfig`
- **New Configuration**:
  - **Queues**: `userCreatedQueue`, `userUpdatedQueue`, `userDeletedQueue`
  - **Bindings**: Links user event queues to exchange with UserEvents constants
  - **Bean**: `UserEventListener` for handling user events

### 6. ReaderEventPublisher.java (ENHANCED)
- **Location**: `pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher`
- **Enhancement**: Now uses `UserEvents.USER_REQUESTED` constant for user request events

## Existing AMQP Components (Already Present)

### Reader Management
- **ReaderViewAMQP**: DTO for reader data in AMQP communication
- **ReaderViewAMQPMapper**: MapStruct mapper for reader entity conversions
- **ReaderEventPublisher**: Publisher for reader events (created, updated, deleted)
- **ReaderEventListener**: Listener for reader events from other services

### Other Domain Events
- **BookEventListener**: Handles book events from lms_books_command
- **GenreEventListener**: Handles genre events from book services
- **LendingEventListener**: Handles lending events from lending services

## Communication Flow

### Incoming Messages (from lms_auth_users)
- **User Created**: When a user is created in lms_auth_users, the event is received and processed
- **User Updated**: When a user is updated in lms_auth_users, the event is received and processed
- **User Deleted**: When a user is deleted in lms_auth_users, the event is received and processed

### Outgoing Messages (to lms_auth_users and other services)
- **Reader Events**: Created, updated, deleted reader events sent to other services
- **User Requests**: Requests for user information sent to lms_auth_users via `USER_REQUESTED`
- **Saga Events**: Complex reader-user creation workflows via saga patterns

### Bidirectional Communication with lms_auth_users
- **Reader → Auth Users**: Sends user information requests when creating readers
- **Auth Users → Reader**: Receives user events to maintain consistency
- **Saga Pattern**: Handles complex reader-user creation workflows

## Configuration Details

### RabbitMQ Exchange
- **DirectExchange**: `LMS.direct` (shared across all LMS services)

### User Event Queues (NEW)
- `userCreatedQueue`: Anonymous queue for user creation events
- `userUpdatedQueue`: Anonymous queue for user update events  
- `userDeletedQueue`: Anonymous queue for user deletion events

### User Event Bindings (NEW)
- `user.created` → `userCreatedQueue`
- `user.updated` → `userUpdatedQueue`
- `user.deleted` → `userDeletedQueue`

### Existing Reader Event Infrastructure
- Reader creation, update, and deletion events
- Saga queues for reader-user workflows
- RPC bootstrap for service discovery

## Integration Benefits

1. **Real-time Synchronization**: Immediately receives user events from auth service
2. **Data Consistency**: Maintains consistent user information across services
3. **Decoupled Architecture**: Services communicate via events, not direct calls
4. **Fault Tolerance**: AMQP provides reliable message delivery
5. **Scalability**: Anonymous queues allow multiple instances of the service

## Usage Examples

### Receiving User Events
When a user is created in `lms_auth_users`:
1. UserEventPublisher in lms_auth_users sends `USER_CREATED` event
2. UserEventListener in lms_readers_command receives the event
3. UserService.handleUserCreated() processes the event
4. Any necessary reader-related data is updated/created

### Requesting User Information
When creating a reader:
1. ReaderEventPublisher sends `USER_REQUESTED` event
2. lms_auth_users UserEventListener receives and processes the request
3. User information is returned (implementation specific)

## Error Handling

- All AMQP operations include comprehensive try-catch blocks
- Failed message processing logs errors but doesn't break the service
- User event processing failures are isolated from core reader functionality
- Provides fallback mechanisms for critical operations

## Future Enhancements

- Implement request-response patterns for user data retrieval
- Add user event replay capabilities for service recovery
- Implement more sophisticated saga orchestration
- Add metrics and monitoring for user event processing
- Consider adding user data caching mechanisms

## Dependencies

- **Spring AMQP**: For RabbitMQ integration
- **Jackson**: For JSON serialization/deserialization
- **MapStruct**: For object mapping
- **Spring Transaction**: For transactional event processing

This enhanced implementation provides complete bidirectional AMQP communication between `lms_readers_command` and `lms_auth_users`, enabling robust, scalable, and maintainable inter-service communication.
