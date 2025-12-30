# Testing Guide: Docker Swarm Setup for LMS Books Event Flow

## Your Setup Overview

You're using Docker Swarm with:
- **Command Service**: lms_books_command with its own PostgreSQL (port 5432)
- **Query Service**: lms_books_query with its own PostgreSQL (port 5433)
- **Shared RabbitMQ**: docker-compose-swarm-shared.yml (port 5672 & 15672)
- **Network**: lms_network (external, managed by Docker Swarm)

## Prerequisites

1. Docker Swarm initialized: `docker swarm init`
2. Create the external network: `docker network create -d overlay lms_network`
3. Both services compiled: `mvn clean package -DskipTests`
4. Docker images built for command and query services

## Step 1: Prepare and Build Services

### 1.1 Build Command Service Docker Image

```bash
cd C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_command

# Build the Docker image
docker build -t lmsbooks:command -f Dockerfile .
```

### 1.2 Build Query Service Docker Image

```bash
cd C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_query

# Build the Docker image
docker build -t lmsbooks:query -f Dockerfile .
```

### 1.3 Verify Images Are Built

```bash
docker images | grep lmsbooks
```

Expected output:
```
lmsbooks                     command         xxxxx        2 minutes ago
lmsbooks                     query           xxxxx        1 minute ago
```

## Step 2: Initialize Docker Swarm

### 2.1 Check if Swarm is already initialized

```bash
docker info | findstr Swarm
```

If it shows `Swarm: inactive`, initialize it:

```bash
docker swarm init
```

### 2.2 Create the overlay network

```bash
docker network create -d overlay lms_network
```

Verify:
```bash
docker network ls | findstr lms_network
```

## Step 3: Deploy RabbitMQ (Shared Infrastructure)

```bash
cd C:\Users\migue\IdeaProjects\lms-library

docker stack deploy -c docker-compose-swarm-shared.yml lms_shared
```

Wait 30 seconds for RabbitMQ to start, then verify:

```bash
docker stack services lms_shared
```

Expected output:
```
ID             NAME                MODE        REPLICAS   IMAGE
xxxxx          lms_shared_rabbitmq  replicated  1/1        rabbitmq:3-management
```

### Check RabbitMQ is running

- Open browser: http://localhost:15672
- Username: guest
- Password: guest

You should see the RabbitMQ Management UI

## Step 4: Deploy Command Service

```bash
cd C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_command

docker stack deploy -c docker-compose-swarm.yml lms_command
```

Wait 30-40 seconds for services to start, then verify:

```bash
docker stack services lms_command
```

Expected output:
```
ID             NAME                          MODE        REPLICAS   IMAGE
xxxxx          lms_command_lmsbooks_command  replicated  2/2        lmsbooks:command
xxxxx          lms_command_postgres_command  replicated  1/1        postgres:latest
```

### Check Command Service Logs

```bash
docker service logs lms_command_lmsbooks_command
```

Wait for message: `Started LMSBooks in X seconds`

### Verify Command Service is Accessible

- Open browser: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Step 5: Deploy Query Service

```bash
cd C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_query

docker stack deploy -c docker-compose-swarm.yml lms_query
```

Wait 30-40 seconds for services to start, then verify:

```bash
docker stack services lms_query
```

Expected output:
```
ID             NAME                        MODE        REPLICAS   IMAGE
xxxxx          lms_query_lmsbooks_query    replicated  2/2        lmsbooks:query
xxxxx          lms_query_postgres_query    replicated  1/1        postgres:latest
```

### Check Query Service Logs

```bash
docker service logs lms_query_lmsbooks_query
```

Wait for message: `Started LMSBooks in X seconds`

### Verify Query Service is Accessible

- Open browser: http://localhost:8085
- Swagger UI: http://localhost:8085/swagger-ui.html

## Step 6: Verify All Services Are Running

```bash
docker stack ls
```

Expected output:
```
NAME         SERVICES   ORCHESTRATOR
lms_command  2          Swarm
lms_query    2          Swarm
lms_shared   1          Swarm
```

```bash
docker stack services lms_command lms_query lms_shared
```

All services should show `X/X` replicas (e.g., `2/2` or `1/1`)

## Step 7: Test the Complete Event Flow

### Phase 1: Create an Author

```bash
curl -X POST http://localhost:8080/api/v1/authors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "J.K. Rowling",
    "gender": "Female",
    "birthDate": "1965-07-31",
    "nationality": "British"
  }'
```

Save the `authorNumber` from the response.

### Phase 2: Create a Genre

```bash
curl -X POST http://localhost:8080/api/v1/genres \
  -H "Content-Type: application/json" \
  -d '{
    "genre": "Fantasy"
  }'
```

### Phase 3: Create a Book

Replace `{authorNumber}` with the value from Phase 1:

```bash
curl -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "978-0747532699",
    "title": "Harry Potter and the Philosopher'\''s Stone",
    "description": "A young wizard discovers his magical heritage",
    "genre": "Fantasy",
    "authors": [{authorNumber}]
  }'
```

This triggers:
- ✅ AuthorCreated event
- ✅ GenreCreated event
- ✅ BookFinalized event

### Phase 4: Wait for Events to Propagate

```bash
# Wait 5 seconds for RabbitMQ to process and deliver events
timeout /t 5
```

### Phase 5: Verify Book in Query Service

```bash
curl -X GET http://localhost:8085/api/v1/books/isbn/978-0747532699 \
  -H "Content-Type: application/json"
```

**Expected Response** (status 200):
```json
{
  "isbn": "978-0747532699",
  "title": "Harry Potter and the Philosopher's Stone",
  "description": "A young wizard discovers his magical heritage",
  "genre": "Fantasy",
  "authors": [
    {
      "authorNumber": 1,
      "name": "J.K. Rowling"
    }
  ]
}
```

## Step 8: Monitor Events in RabbitMQ

Open http://localhost:15672 and check the **Queues** tab.

You should see these queues with messages:
- `query.book.created` - Book creation event
- `query.author.created` - Author creation event
- `query.genre.created` - Genre creation event
- `query.book.finalized` - Book finalization event

Click on each queue to see message details.

## Step 9: Monitor Service Logs

### Command Service Logs

```bash
docker service logs -f lms_command_lmsbooks_command
```

Look for patterns:
```
[COMMAND] 📤 Sending Book Created event
[COMMAND] 📤 Sending Author Pending Created event
[COMMAND] 📤 Sending Genre Pending Created event
[COMMAND] 📤 Sending Book Finalized event
```

### Query Service Logs

```bash
docker service logs -f lms_query_lmsbooks_query
```

Look for patterns:
```
[QUERY] 📥 Received Book Created: 978-0747532699
[QUERY] ✅ Read model updated for book: 978-0747532699
[QUERY] 📥 Received Author Created: J.K. Rowling
[QUERY] ✅ Book updated with author: J.K. Rowling
[QUERY] 📥 Received Genre Created: Fantasy
[QUERY] ✅ Book finalized and saved: 978-0747532699
```

## Step 10: Check Databases

### Command Database (port 5432)

```bash
docker exec -it $(docker ps -f "name=lms_command_postgres" -q) \
  psql -U postgres -d command_books -c "SELECT isbn, title, genre FROM book;"
```

### Query Database (port 5433)

```bash
docker exec -it $(docker ps -f "name=lms_query_postgres" -q) \
  psql -U postgres -d query_books -c "SELECT isbn, title, genre FROM book;"
```

Both should have the same books (eventually consistent).

## Troubleshooting

### Issue 1: Service won't start

**Check logs:**
```bash
docker service logs lms_command_lmsbooks_command
docker service logs lms_query_lmsbooks_query
```

**Common issues:**
- Port conflicts: Check if ports 8080/8085 are already in use
- Network issues: Ensure `lms_network` exists: `docker network ls | grep lms_network`
- Database issues: Check if postgres is running: `docker service logs lms_command_postgres_command`

### Issue 2: Events not appearing in Query Service

**Verify RabbitMQ:**
```bash
docker exec -it $(docker ps -f "name=lms_shared_rabbitmq" -q) \
  rabbitmqctl list_queues name messages
```

You should see queues with message counts.

**Verify connectivity:**
```bash
docker exec -it $(docker ps -f "name=lms_query" -q) \
  nc -zv rabbitmq 5672
```

Should return: `Connection to rabbitmq port 5672 succeeded!`

### Issue 3: Book visible in Command but not Query

**Solutions:**
1. Wait longer (5-10 seconds) - events take time to process
2. Check Query Service logs for deserialization errors
3. Verify both services can reach RabbitMQ
4. Check if Query database is properly initialized

### Issue 4: Replicas showing 0/2

**This means services failed to start:**

```bash
docker service ps lms_command_lmsbooks_command
```

Look for error messages. Usually caused by:
- Port conflicts
- Image not found
- Resource constraints

**Solution:**
```bash
# Remove and redeploy
docker stack rm lms_command
docker stack deploy -c docker-compose-swarm.yml lms_command
```

## Performance Testing

Once basic flow works, test with multiple books:

```bash
# Create 10 books in rapid succession
for /l %i in (1,1,10) do (
  curl -X POST http://localhost:8080/api/v1/books ^
    -H "Content-Type: application/json" ^
    -d "{\"isbn\":\"978-074753269%i\",\"title\":\"Book %i\",\"description\":\"Test\",\"genre\":\"Fantasy\",\"authors\":[1]}"
  timeout /t 1
)
```

Then verify all appear in Query Service:
```bash
curl -X GET http://localhost:8085/api/v1/books/genre/Fantasy
```

## Cleanup

When done testing:

```bash
# Remove all stacks
docker stack rm lms_command lms_query lms_shared

# Remove images (optional)
docker rmi lmsbooks:command lmsbooks:query

# Remove network (optional)
docker network rm lms_network
```

## Success Criteria ✅

You have successfully tested the flow if:

1. ✅ Command Service starts and is accessible at http://localhost:8080
2. ✅ Query Service starts and is accessible at http://localhost:8085
3. ✅ RabbitMQ starts and is accessible at http://localhost:15672
4. ✅ Author can be created via POST /api/v1/authors
5. ✅ Genre can be created via POST /api/v1/genres
6. ✅ Book can be created via POST /api/v1/books
7. ✅ Book appears in Query Service within 5-10 seconds
8. ✅ RabbitMQ shows events flowing through queues
9. ✅ All three events appear in logs:
   - AuthorCreated ✅
   - GenreCreated ✅
   - BookFinalized ✅
10. ✅ Both databases have the same book data

## Quick Reference Commands

```bash
# Check all stacks
docker stack ls

# Check all services
docker stack services lms_command lms_query lms_shared

# View service logs
docker service logs -f lms_command_lmsbooks_command
docker service logs -f lms_query_lmsbooks_query

# List all queues in RabbitMQ
docker exec -it $(docker ps -f "name=rabbitmq" -q) rabbitmqctl list_queues

# Check network
docker network inspect lms_network

# Remove everything
docker stack rm lms_command lms_query lms_shared
```

