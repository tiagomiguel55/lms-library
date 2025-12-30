# Jenkins CI/CD Pipeline - LMS Books Command Service

## Overview

This document describes the CI/CD pipeline implementation for the LMS Books Command service using Jenkins. The pipeline supports three environments (dev, staging, prod) and implements a rolling update deployment strategy for production.

## Pipeline Features

### 1. Multi-Environment Support
- **DEV**: Development environment with automatic deployment
- **STAGING**: Staging environment with automatic deployment  
- **PROD**: Production environment with manual approval required

### 2. Testing Strategies

#### Unit Tests (DEV only)
- Automatically runs all unit tests when deploying to dev
- Test results are published in Jenkins
- Located in: `target/surefire-reports/`

#### Mutation Tests (DEV only)
- Uses PITest for mutation testing
- Configurable threshold: 60% mutation coverage, 70% line coverage
- HTML report published in Jenkins
- Can be disabled via pipeline parameter

### 3. Deployment Strategies

#### DEV & STAGING
- **Strategy**: Simple automatic deployment
- Single instance deployment
- Immediate deployment after successful build

#### PRODUCTION
- **Strategy**: Rolling Update
- 3 instances deployed sequentially
- Each instance is health-checked before proceeding
- Zero-downtime deployment
- **Manual approval required** before deployment

### 4. Rolling Update Implementation

The rolling update strategy updates instances one at a time:

1. **Stop instance 1** → Update → Health check → Wait
2. **Stop instance 2** → Update → Health check → Wait  
3. **Stop instance 3** → Update → Health check → Complete

**Benefits:**
- Zero downtime
- Gradual rollout
- Easy rollback if issues detected
- Always have healthy instances running

### 5. Approval Process

For production deployments:
1. Pipeline sends email notification to approver
2. Build pauses waiting for manual approval (30 min timeout)
3. Approver reviews and clicks approve/reject
4. Only after approval, rolling update proceeds

## Pipeline Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `ENVIRONMENT` | Choice | Select: dev, staging, or prod |
| `RUN_MUTATION_TESTS` | Boolean | Enable/disable mutation tests (dev only) |
| `SKIP_DEPLOY` | Boolean | Skip deployment phase (for testing) |

## Environment Configuration

### Port Mapping

| Environment | Service Port | PostgreSQL | RabbitMQ | Management UI |
|-------------|--------------|------------|----------|---------------|
| DEV | 8081 | 5433 | 5673 | 15673 |
| STAGING | 8082 | 5434 | 5674 | 15674 |
| PROD (1) | 8083 | 5435 | 5675 | 15675 |
| PROD (2) | 8084 | - | - | - |
| PROD (3) | 8085 | - | - | - |

### Docker Compose Files

- `docker-compose-dev.yml` - Development environment
- `docker-compose-staging.yml` - Staging environment
- `docker-compose-prod.yml` - Production environment (3 replicas)

## Jenkins Setup

### Prerequisites

1. **Jenkins Plugins Required:**
   - Pipeline
   - Git
   - Docker Pipeline
   - Email Extension Plugin
   - HTML Publisher Plugin
   - JUnit Plugin

2. **Jenkins Configuration:**
   ```groovy
   MAVEN_HOME=/usr/share/maven
   JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   ```

3. **Credentials:**
   - GitHub credentials (ID: `password_for_github_tiago`)
   - Email SMTP configuration

### Pipeline Creation

1. Create new Pipeline job in Jenkins
2. Configure pipeline from SCM:
   - Repository: Your Git repo
   - Script Path: `P2/lms_books_command/Jenkinsfile`
3. Enable "This project is parameterized"
4. Configure email notifications

## Usage

### Deploy to Development
```bash
1. Select ENVIRONMENT: dev
2. Check RUN_MUTATION_TESTS: true
3. Run pipeline
```

Result: Automatic deployment with full testing

### Deploy to Staging
```bash
1. Select ENVIRONMENT: staging
2. Run pipeline
```

Result: Automatic deployment without mutation tests

### Deploy to Production
```bash
1. Select ENVIRONMENT: prod
2. Run pipeline
3. Wait for email notification
4. Review and approve in Jenkins
5. Rolling update executes
```

Result: Manual approval → Rolling update deployment

## Manual Rolling Update

You can also perform rolling updates manually using the provided scripts:

**Linux/Mac:**
```bash
cd P2/lms_books_command
chmod +x rolling-update.sh
./rolling-update.sh <image-tag>
```

**Windows:**
```cmd
cd P2\lms_books_command
rolling-update.bat <image-tag>
```

## Monitoring & Health Checks

Each production instance has health checks configured:
- **Endpoint**: `http://localhost:8080/actuator/health`
- **Interval**: 10 seconds
- **Timeout**: 5 seconds
- **Retries**: 3
- **Start period**: 40 seconds

## Rollback Strategy

If deployment fails:

1. **During Rolling Update**: 
   - Pipeline stops automatically
   - Failed instance is marked
   - Previous instances remain healthy

2. **Manual Rollback**:
   ```bash
   docker-compose -f docker-compose-prod.yml down
   docker tag lmsbooks:<previous-tag> lmsbooks:latest
   docker-compose -f docker-compose-prod.yml up -d
   ```

## Reports & Artifacts

### Generated Reports
1. **Unit Test Report**: JUnit XML format
2. **Mutation Test Report**: PITest HTML report
3. **Build Logs**: Console output in Jenkins

### Accessing Reports
- Unit Tests: Jenkins → Build → Test Results
- Mutation Tests: Jenkins → Build → PITest Mutation Report
- Docker Status: Check deployment verification stage logs

## Email Notifications

Email notifications are sent for:
1. **Production Approval Required**: Notification to approve deployment
2. **Build Success**: Summary of successful deployment
3. **Build Failure**: Alert with console output link

## Troubleshooting

### Build Fails at Unit Tests
- Check test logs in Jenkins
- Review `target/surefire-reports/`

### Mutation Tests Timeout
- Increase timeout or disable via parameter
- Check PITest configuration in pom.xml

### Rolling Update Fails
- Check health endpoint availability
- Review Docker logs: `docker logs <container-name>`
- Verify database connectivity

### Docker Image Issues
- Clean old images: `docker image prune -a`
- Check disk space: `df -h`

## Best Practices

1. **Always test in DEV first** with full test suite
2. **Deploy to STAGING** for integration testing
3. **Review staging results** before production
4. **Monitor during rolling updates** in production
5. **Keep last 5 images** for quick rollback

## Compliance with Requirements

### ✅ 3.1 - Automatic Deployment (Service A)
- DEV and STAGING deploy automatically after successful build

### ✅ 3.2 - Manual Approval (Service B)
- PROD requires manual approval
- Email notification sent to approver
- 30-minute timeout for approval

### ✅ 3.3 - Rolling Update Strategy
- Implemented for production environment
- Updates 3 instances sequentially
- Zero-downtime deployment
- Health checks between updates

## Contact

For issues or questions about the pipeline, contact the DevOps team.
#!/bin/bash

# Rolling Update Deployment Script for Production
# This script performs a rolling update of the LMS Books Command service

set -e

IMAGE_TAG=${1:-latest}
COMPOSE_FILE="docker-compose-prod.yml"

echo "=========================================="
echo "Rolling Update Deployment Script"
echo "Image Tag: ${IMAGE_TAG}"
echo "=========================================="

# Array of service instances
instances=("lmsbooks_prod_1" "lmsbooks_prod_2" "lmsbooks_prod_3")

# Function to check health of a service
check_health() {
    local instance=$1
    local max_retries=12
    local retry_interval=5
    
    echo "Checking health of ${instance}..."
    
    for ((i=1; i<=max_retries; i++)); do
        if docker exec ${instance} curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo "✅ ${instance} is healthy!"
            return 0
        else
            echo "Health check attempt ${i}/${max_retries} failed. Waiting..."
            sleep ${retry_interval}
        fi
    done
    
    echo "❌ ERROR: ${instance} failed to become healthy!"
    return 1
}

# Perform rolling update
for i in "${!instances[@]}"; do
    instance="${instances[$i]}"
    instance_num=$((i + 1))
    total=${#instances[@]}
    
    echo ""
    echo "=========================================="
    echo "Updating instance ${instance_num}/${total}: ${instance}"
    echo "=========================================="
    
    # Stop and remove the old container
    echo "Stopping ${instance}..."
    docker-compose -f ${COMPOSE_FILE} stop ${instance}
    docker-compose -f ${COMPOSE_FILE} rm -f ${instance}
    
    # Start the new container
    echo "Starting ${instance} with image tag: ${IMAGE_TAG}..."
    export IMAGE_TAG=${IMAGE_TAG}
    docker-compose -f ${COMPOSE_FILE} up -d ${instance}
    
    # Wait for container to start
    sleep 10
    
    # Check health
    if ! check_health ${instance}; then
        echo "❌ Deployment failed at instance ${instance}"
        echo "Rolling back..."
        exit 1
    fi
    
    echo "✅ ${instance} successfully updated!"
    
    # Pause between updates (except for the last one)
    if [ ${instance_num} -lt ${total} ]; then
        echo "Pausing 10 seconds before next update..."
        sleep 10
    fi
done

echo ""
echo "=========================================="
echo "✅ Rolling update completed successfully!"
echo "All instances are running version: ${IMAGE_TAG}"
echo "=========================================="

# Show final status
echo ""
echo "Final service status:"
docker-compose -f ${COMPOSE_FILE} ps

exit 0

