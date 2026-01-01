# LMS Auth Users - AMQP Implementation Summary

This document describes the AMQP components implemented for the `lms_auth_users` module to enable communication with other LMS microservices, particularly `lms_readers_command`.

## Components Created

### 1. UserViewAMQP.java
- **Location**: `pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP`
- **Purpose**: DTO for user data transmitted via AMQP
- **Fields**:
  - `username`: User's username
  - `fullName`: User's full name
  - `password`: User's password (encrypted)
  - `version`: Version for optimistic locking
  - `_links`: HATEOAS links (map)

### 2. UserViewAMQPMapper.java
- **Location**: `pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQPMapper`
- **Purpose**: MapStruct mapper for converting between User entity and UserViewAMQP
- **Methods**:
  - `toUserViewAMQP(User user)`: Converts User entity to AMQP DTO
  - `toUserViewAMQP(UserViewAMQP userView)`: Identity mapping for AMQP DTOs

### 3. UserEventPublisher.java
- **Location**: `pt.psoft.g1.psoftg1.usermanagement.publishers.UserEventPublisher`
- **Purpose**: Publishes user events to RabbitMQ
- **Methods**:
  - `sendUserCreated(UserViewAMQP event)`: Publishes user creation events
  - `sendUserUpdated(UserViewAMQP event)`: Publishes user update events
  - `sendUserDeleted(String username)`: Publishes user deletion events

### 4. UserEventListener.java
- **Location**: `pt.psoft.g1.psoftg1.usermanagement.listeners.UserEventListener`
- **Purpose**: Listens for user-related AMQP messages from other services
- **Listeners**:
  - `receiveUserCreated()`: Handles user creation events from other services
  - `receiveUserUpdated()`: Handles user update events from other services
  - `receiveUserDeleted()`: Handles user deletion events from other services
  - `receiveUserRequest()`: Handles user information requests from other services (especially `lms_readers_command`)

### 5. RabbitMQConfig.java
- **Location**: `pt.psoft.g1.psoftg1.configuration.RabbitMQConfig`
- **Purpose**: RabbitMQ configuration for user events
- **Configuration**:
  - DirectExchange: `LMS.direct`
  - Queues: `userCreatedQueue`, `userUpdatedQueue`, `userDeletedQueue`, `userRequestQueue`
  - Bindings: Links queues to exchange with appropriate routing keys

### 6. UserEvents.java
- **Location**: `pt.psoft.g1.psoftg1.shared.model.UserEvents`
- **Purpose**: Constants for user event routing keys
- **Constants**:
  - `USER_CREATED = "user.created"`
  - `USER_UPDATED = "user.updated"`
  - `USER_DELETED = "user.deleted"`
  - `USER_REQUESTED = "reader.user.requested.user"`

## Integration with UserService

The existing `UserService` has been enhanced to publish AMQP events:

- **User Creation**: Publishes `USER_CREATED` event after successful user creation
- **User Update**: Publishes `USER_UPDATED` event after successful user update
- **User Deletion**: Publishes `USER_DELETED` event after successful user deletion

All AMQP publishing is done in a fault-tolerant way - if publishing fails, it logs the error but doesn't break the main operation.

## Dependencies Added

1. **Maven Dependency**: `spring-boot-starter-amqp` added to `pom.xml`
2. **Model Enhancement**: Added `@Getter` annotation to `version` field in `User` model

## Communication Flow

### Outgoing Messages (from lms_auth_users)
- When users are created/updated/deleted, events are published to the `LMS.direct` exchange
- Other services can subscribe to these events by binding to the appropriate routing keys

### Incoming Messages (to lms_auth_users)
- The service listens on `reader.user.requested.user` queue for user information requests
- Particularly designed to handle requests from `lms_readers_command` module
- Other user event listeners are in place for potential future use

## Usage Notes

1. **Service Integration**: The listener methods in `UserEventListener` contain placeholder code and need to be implemented based on specific business requirements
2. **Error Handling**: All AMQP operations include try-catch blocks to prevent failures from affecting core functionality
3. **Queue Configuration**: Uses anonymous queues for event listening and durable queues for request/response patterns
4. **Thread Safety**: All components are designed to be thread-safe and work within Spring's transaction management

## Future Enhancements

- Implement response handling for user requests from other services
- Add more sophisticated error handling and retry mechanisms
- Consider implementing saga patterns for distributed transactions
- Add metrics and monitoring for AMQP message processing
