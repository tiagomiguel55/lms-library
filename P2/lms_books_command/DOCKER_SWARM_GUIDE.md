# Docker Swarm Deployment Guide - LMS Books Command Service

## Overview

This project now uses **Docker Swarm** for container orchestration instead of docker-compose. Docker Swarm provides built-in rolling updates, load balancing, and high availability.

## 🎯 Why Docker Swarm?

- **Built-in Rolling Updates**: Automatic zero-downtime deployments
- **Load Balancing**: Automatic distribution of traffic across replicas
- **High Availability**: Automatic container restart on failure
- **Service Discovery**: Containers can find each other automatically
- **Scaling**: Easy horizontal scaling with replicas

## 📁 Docker Swarm Files

### Environment-Specific Compose Files
- `docker-compose-swarm.yml` - **DEV** environment (2 replicas)
- `docker-compose-swarm-staging.yml` - **STAGING** environment (2 replicas)
- `docker-compose-swarm-prod.yml` - **PRODUCTION** environment (3 replicas)

### Deployment Scripts
- `deploy-swarm-dev.sh` - Deploy to dev (Linux/Mac)
- `deploy-swarm-staging.sh` - Deploy to staging (Linux/Mac)
- `deploy-swarm-prod.sh` - Deploy to prod (Linux/Mac)
- `deploy-swarm-prod.bat` - Deploy to prod (Windows)

## 🚀 Quick Start

### 1. Initialize Docker Swarm (First Time Only)

```bash
# Initialize swarm on your machine
docker swarm init

# Create the overlay network
docker network create --driver overlay --attachable lms_network
```

### 2. Deploy to Development

```bash
cd P2/lms_books_command
chmod +x deploy-swarm-dev.sh
./deploy-swarm-dev.sh
```

**Result**: 2 replicas of the service deployed with automatic load balancing

### 3. Deploy to Staging

```bash
chmod +x deploy-swarm-staging.sh
./deploy-swarm-staging.sh
```

**Result**: 2 replicas deployed on different ports (8081)

### 4. Deploy to Production

```bash
chmod +x deploy-swarm-prod.sh
./deploy-swarm-prod.sh <image-tag>

# Example with specific version
./deploy-swarm-prod.sh v1.2.3

# Or use latest
./deploy-swarm-prod.sh latest
```

**Result**: 3 replicas with automatic rolling update (one at a time)

## 🔧 Environment Configuration

### Development (Stack: lmsbooks-dev)
- **Replicas**: 2
- **Service Port**: 8080 (mapped internally)
- **Database**: postgres_command_dev:5432
- **RabbitMQ**: rabbitmq_dev:5672, Management UI: 15672
- **Profile**: dev
- **Update Strategy**: Simple restart

### Staging (Stack: lmsbooks-staging)
- **Replicas**: 2
- **Service Port**: 8081
- **Database**: postgres_command_staging:5433
- **RabbitMQ**: rabbitmq_staging:5673, Management UI: 15673
- **Profile**: staging
- **Update Strategy**: Rolling update (1 at a time, 10s delay)

### Production (Stack: lmsbooks-prod)
- **Replicas**: 3
- **Service Port**: 8082
- **Database**: postgres_command_prod:5434
- **RabbitMQ**: rabbitmq_prod:5674, Management UI: 15674
- **Profile**: prod
- **Update Strategy**: Advanced rolling update with health checks
  - Update 1 container at a time
  - 10-second delay between updates
  - 60-second monitoring period
  - Automatic rollback on failure
  - Resource limits: 1 CPU, 1GB RAM per container

## 🔄 Rolling Update Configuration (Production)

The production environment uses advanced rolling update settings:

```yaml
update_config:
  parallelism: 1          # Update 1 container at a time
  delay: 10s              # Wait 10s between updates
  failure_action: rollback # Rollback if update fails
  monitor: 60s            # Monitor for 60s after update
  max_failure_ratio: 0.3  # Allow 30% failure rate
  order: start-first      # Start new before stopping old

rollback_config:
  parallelism: 1          # Rollback 1 at a time
  delay: 5s
  failure_action: pause
  monitor: 30s
```

### Rolling Update Process

```
Initial State: [v1.0] [v1.0] [v1.0]
                  ↓
Step 1:        [v2.0] [v1.0] [v1.0]  ← Update replica 1, health check
Wait 10s              ↓
Step 2:        [v2.0] [v2.0] [v1.0]  ← Update replica 2, health check
Wait 10s                     ↓
Step 3:        [v2.0] [v2.0] [v2.0]  ← Update replica 3, health check
                  ✅ Complete
```

## 📊 Docker Swarm Commands

### View Stack Services
```bash
docker stack services lmsbooks-dev
docker stack services lmsbooks-staging
docker stack services lmsbooks-prod
```

### View Running Tasks (Containers)
```bash
docker stack ps lmsbooks-dev
docker stack ps lmsbooks-staging
docker stack ps lmsbooks-prod
```

### View Service Logs
```bash
# Get service name first
docker service ls

# View logs
docker service logs lmsbooks-prod_lmsbooks_command
docker service logs --follow lmsbooks-prod_lmsbooks_command
docker service logs --tail 50 lmsbooks-prod_lmsbooks_command
```

### Scale Services
```bash
# Scale production to 5 replicas
docker service scale lmsbooks-prod_lmsbooks_command=5

# Scale back to 3
docker service scale lmsbooks-prod_lmsbooks_command=3
```

### Update Service Image
```bash
# Update to new image version
docker service update --image lmsbooks:v2.0.0 lmsbooks-prod_lmsbooks_command

# Force update (pull new image)
docker service update --force lmsbooks-prod_lmsbooks_command
```

### Check Service Health
```bash
docker service inspect lmsbooks-prod_lmsbooks_command --pretty
docker service ps lmsbooks-prod_lmsbooks_command
```

### Remove Stack
```bash
docker stack rm lmsbooks-dev
docker stack rm lmsbooks-staging
docker stack rm lmsbooks-prod
```

## 🏥 Health Checks

All environments have health checks configured:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 3
  start_period: 40s
```

Docker Swarm uses these health checks during rolling updates to ensure each container is healthy before proceeding.

## 🔍 Monitoring and Debugging

### Check Swarm Status
```bash
docker info | grep Swarm
docker node ls
```

### View All Services
```bash
docker service ls
```

### Inspect a Service
```bash
docker service inspect lmsbooks-prod_lmsbooks_command
```

### View Service Events
```bash
docker service ps lmsbooks-prod_lmsbooks_command --no-trunc
```

### Check Update Status
```bash
docker service inspect lmsbooks-prod_lmsbooks_command --format='{{.UpdateStatus.State}}'
```

### View Container Placement
```bash
docker service ps lmsbooks-prod_lmsbooks_command --format "table {{.Name}}\t{{.Node}}\t{{.CurrentState}}"
```

## 🔧 Troubleshooting

### Service Won't Start
```bash
# Check service logs
docker service logs lmsbooks-prod_lmsbooks_command

# Check task failures
docker service ps lmsbooks-prod_lmsbooks_command --no-trunc

# Inspect service
docker service inspect lmsbooks-prod_lmsbooks_command
```

### Rolling Update Failed
```bash
# Check update status
docker service inspect lmsbooks-prod_lmsbooks_command --format='{{json .UpdateStatus}}'

# View rollback status
docker service ps lmsbooks-prod_lmsbooks_command

# Manual rollback if needed
docker service rollback lmsbooks-prod_lmsbooks_command
```

### Network Issues
```bash
# Check network
docker network ls
docker network inspect lms_network

# Recreate network if needed
docker network rm lms_network
docker network create --driver overlay --attachable lms_network
```

### Database Connection Issues
```bash
# Check if database service is running
docker service ps lmsbooks-prod_postgres_command_prod

# Check database logs
docker service logs lmsbooks-prod_postgres_command_prod

# Test connectivity from app container
docker exec <container-id> ping postgres_command_prod
```

## 🎯 Jenkins Pipeline Integration

The updated Jenkinsfile now uses Docker Swarm for deployments:

### Key Changes:
1. **Automatic Swarm Initialization**: Pipeline checks and initializes swarm if needed
2. **Stack-Based Deployment**: Uses `docker stack deploy` instead of `docker-compose up`
3. **Rolling Update Monitoring**: Tracks update progress and reports status
4. **Environment-Specific Stacks**: Separate stack names for isolation

### Pipeline Flow:
```
Build → Test → Package → Docker Image → Deploy to Swarm
                                           ↓
                       ┌──────────────────┴──────────────────┐
                       ↓                  ↓                   ↓
                   DEV Stack         STAGING Stack       PROD Stack
                   (2 replicas)      (2 replicas)        (3 replicas)
                   Auto Deploy       Auto Deploy         Manual Approval
                                                         + Rolling Update
```

## 📋 Best Practices

### 1. Always Use Stack Names
Keep stack names consistent:
- `lmsbooks-dev` for development
- `lmsbooks-staging` for staging
- `lmsbooks-prod` for production

### 2. Tag Images Properly
```bash
docker tag lmsbooks:123 lmsbooks:prod
docker tag lmsbooks:123 lmsbooks:v1.2.3
```

### 3. Monitor Rolling Updates
Don't walk away during production updates. Monitor:
```bash
watch -n 2 'docker service ps lmsbooks-prod_lmsbooks_command'
```

### 4. Use Health Checks
Always configure health checks for automatic failover and update validation.

### 5. Resource Limits
Set appropriate resource limits for production:
```yaml
resources:
  limits:
    cpus: '1'
    memory: 1G
  reservations:
    cpus: '0.5'
    memory: 512M
```

## 🔄 Migration from docker-compose

If you were using the old docker-compose files:

1. **Stop old containers**:
   ```bash
   docker-compose -f docker-compose-dev.yml down
   docker-compose -f docker-compose-staging.yml down
   docker-compose -f docker-compose-prod.yml down
   ```

2. **Initialize Swarm**:
   ```bash
   docker swarm init
   docker network create --driver overlay --attachable lms_network
   ```

3. **Deploy to Swarm**:
   ```bash
   ./deploy-swarm-dev.sh
   ./deploy-swarm-staging.sh
   ./deploy-swarm-prod.sh latest
   ```

## 📚 Additional Resources

- [Docker Swarm Documentation](https://docs.docker.com/engine/swarm/)
- [Docker Service Commands](https://docs.docker.com/engine/reference/commandline/service/)
- [Docker Stack Commands](https://docs.docker.com/engine/reference/commandline/stack/)
- [Rolling Updates](https://docs.docker.com/engine/swarm/swarm-tutorial/rolling-update/)

## 🆚 Swarm vs docker-compose

| Feature | docker-compose | Docker Swarm |
|---------|----------------|--------------|
| Rolling Updates | ❌ Manual | ✅ Automatic |
| Load Balancing | ❌ No | ✅ Built-in |
| High Availability | ❌ No | ✅ Yes |
| Scaling | Manual | Automatic |
| Health Checks | Basic | Advanced with routing |
| Multi-node | ❌ No | ✅ Yes |
| Zero Downtime | ❌ No | ✅ Yes |

---

**Your Docker Swarm deployment is now fully configured and ready to use!** 🚀

All three environments (dev, staging, prod) can be deployed with a single command, and production deployments will automatically perform rolling updates with zero downtime.

