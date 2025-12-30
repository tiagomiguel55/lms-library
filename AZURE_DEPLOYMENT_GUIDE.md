# Azure Docker Swarm Deployment Guide - LMS Books

## Overview

Your deployment uses a **shared RabbitMQ infrastructure** pattern:
1. **One shared RabbitMQ** deployed once (lms_shared_rabbitmq)
2. **Command Service** connects to shared RabbitMQ
3. **Query Service** connects to shared RabbitMQ

## Prerequisites

- Docker Swarm initialized on Azure VM
- Overlay network `lms_network` created
- All code committed and pushed to GitHub

## Step-by-Step Deployment

### STEP 1: Prepare Azure VM (Run Once)

SSH into your Azure VM and run:

```bash
# Initialize Docker Swarm (if not already done)
docker swarm init

# Create the shared overlay network
docker network create -d overlay lms_network

# Verify network creation
docker network ls | grep lms_network
```

Expected output:
```
xxxxxxxx  lms_network  overlay  swarm
```

### STEP 2: Deploy Shared RabbitMQ Infrastructure

```bash
cd /path/to/lms-library

# Deploy the shared RabbitMQ stack
docker stack deploy -c docker-compose-swarm-shared.yml lms_shared

# Wait 30 seconds for RabbitMQ to start
sleep 30

# Verify deployment
docker stack services lms_shared
```

Expected output:
```
ID             NAME                    MODE        REPLICAS   IMAGE
xxxxxxxx       lms_shared_lms_shared_rabbitmq  replicated  1/1        rabbitmq:3-management
```

**Test RabbitMQ is running:**
```bash
# Check if RabbitMQ is healthy
docker stack ps lms_shared

# You should see the RabbitMQ container running
```

Access RabbitMQ Management UI:
- URL: `http://<AZURE_VM_IP>:15672`
- Username: `guest`
- Password: `guest`

### STEP 3: Deploy Command Service via Jenkins

1. Go to your Jenkins server
2. Click on `lms_books_command` pipeline
3. Click "Build with Parameters"
4. Select `ENVIRONMENT: dev` (or staging/prod)
5. Click "Build"
6. Wait for the build to complete (should succeed)

**Verify Command Service:**
```bash
docker stack services lmsbooks-dev
docker stack ps lmsbooks-dev
```

### STEP 4: Deploy Query Service via Jenkins

1. Go to your Jenkins server
2. Click on `lms_books_query` pipeline
3. Click "Build with Parameters"
4. Select `ENVIRONMENT: dev` (or staging/prod)
5. Click "Build"
6. Wait for the build to complete

**Verify Query Service:**
```bash
docker stack services lmsbooks-query-dev
docker stack ps lmsbooks-query-dev
```

### STEP 5: Verify RabbitMQ Connectivity

```bash
# Get a query service container
QUERY_TASK=$(docker service ps lmsbooks-query-dev_lmsbooks_query --filter "desired-state=running" --format "{{.Name}}.{{.ID}}" | head -1)

# Test DNS resolution from query service
docker exec $QUERY_TASK nslookup lms_shared_rabbitmq

# Should show the RabbitMQ service IP
```

## Troubleshooting

### Error: `java.net.UnknownHostException: lms_shared_rabbitmq`

**Cause:** The shared RabbitMQ stack hasn't been deployed or is not on the correct network.

**Fix:**
```bash
# Verify lms_shared is running
docker stack services lms_shared

# If not running, deploy it:
docker stack deploy -c docker-compose-swarm-shared.yml lms_shared

# Verify the network exists
docker network ls | grep lms_network
```

### Error: Shared RabbitMQ not starting

```bash
# Check logs
docker service logs lms_shared_lms_shared_rabbitmq

# Check service status
docker service ps lms_shared_lms_shared_rabbitmq

# If stuck, restart the stack
docker stack rm lms_shared
sleep 10
docker stack deploy -c docker-compose-swarm-shared.yml lms_shared
```

### Query service can't connect to RabbitMQ

1. Verify RabbitMQ is on `lms_network`:
   ```bash
   docker network inspect lms_network
   ```
   Should show `lms_shared_lms_shared_rabbitmq` in the connected containers

2. Verify query service is on `lms_network`:
   ```bash
   docker network inspect lms_network
   ```
   Should show query service containers in the connected containers

3. Check query service logs:
   ```bash
   docker service logs lmsbooks-query-dev_lmsbooks_query --tail 50
   ```

## Network Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    lms_network (overlay)                │
│  (Shared network - all services connect here)           │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  lms_shared_lms_shared_rabbitmq (RabbitMQ)       │  │
│  │  Port: 5672 (AMQP)                               │  │
│  └──────────────────────────────────────────────────┘  │
│                       ▲                                  │
│                       │ Connects to                      │
│                       │                                  │
│        ┌──────────────┴──────────────┐                  │
│        │                             │                  │
│  ┌─────▼──────────┐          ┌──────▼──────────┐       │
│  │ lmsbooks-dev   │          │lmsbooks-query-dev       │
│  │ (Command)      │          │ (Query)        │        │
│  └────────────────┘          └────────────────┘       │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## Deployment Order

**IMPORTANT: Follow this order:**

1. ✅ Create `lms_network` (once)
2. ✅ Deploy `lms_shared` RabbitMQ (once)
3. ✅ Deploy `lmsbooks-dev` (command) via Jenkins
4. ✅ Deploy `lmsbooks-query-dev` (query) via Jenkins

After this, your system should be working!

## Monitoring

### Check all stacks
```bash
docker stack ls
```

### Check service health
```bash
docker stack ps lms_shared
docker stack ps lmsbooks-dev
docker stack ps lmsbooks-query-dev
```

### View logs
```bash
# RabbitMQ logs
docker service logs lms_shared_lms_shared_rabbitmq --tail 100

# Command service logs
docker service logs lmsbooks-dev_lmsbooks_command --tail 100

# Query service logs
docker service logs lmsbooks-query-dev_lmsbooks_query --tail 100
```

### RabbitMQ Management UI
- URL: `http://<AZURE_VM_IP>:15672`
- Username: `guest`
- Password: `guest`

Check:
- Connected clients
- Created queues
- Message flow

## For Staging/Production

Repeat the same steps but with:
- `ENVIRONMENT: staging` for staging
- `ENVIRONMENT: prod` for production

The shared RabbitMQ stack (`lms_shared`) is shared across all environments!

