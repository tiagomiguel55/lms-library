# LMS Books Command Service - CI/CD Pipeline

## 🎯 Project Requirements Implementation

This implementation satisfies the following requirements:

### ✅ 3.1 Automatic Deployment (Service A)
- **DEV** and **STAGING** environments deploy automatically after successful build
- No manual intervention required

### ✅ 3.2 Manual Approval (Service B)
- **PRODUCTION** deployment requires manual approval
- Email notification sent to approver before deployment
- 30-minute timeout for approval decision

### ✅ 3.3 Rolling Update Strategy
- Implemented for **PRODUCTION** environment
- 3 instances updated sequentially
- Zero-downtime deployment
- Health checks between each instance update

## 📁 Project Structure

```
P2/lms_books_command/
├── Jenkinsfile                          # Main Jenkins pipeline
├── rolling-update.sh                    # Linux/Mac rolling update script
├── rolling-update.bat                   # Windows rolling update script
├── docker-compose-dev.yml              # Development environment
├── docker-compose-staging.yml          # Staging environment
├── docker-compose-prod.yml             # Production environment (3 replicas)
├── JENKINS_SETUP_GUIDE.md              # Quick setup instructions
├── JENKINS_PIPELINE_DOCUMENTATION.md   # Complete documentation
├── pom.xml                             # Updated with PITest plugin
└── src/main/resources/
    ├── application-dev.properties      # Dev configuration
    ├── application-staging.properties  # Staging configuration
    └── application-prod.properties     # Production configuration
```

## 🚀 Quick Start

### 1. Setup Jenkins
Follow the instructions in `JENKINS_SETUP_GUIDE.md` to:
- Install required plugins
- Configure credentials
- Create pipeline job
- Configure email notifications

### 2. Deploy to Development (with Tests)
```
Build Parameters:
- ENVIRONMENT: dev
- RUN_MUTATION_TESTS: ✅
- SKIP_DEPLOY: ☐

Result: Automatic deployment with unit tests + mutation tests
```

### 3. Deploy to Staging
```
Build Parameters:
- ENVIRONMENT: staging
- RUN_MUTATION_TESTS: ☐
- SKIP_DEPLOY: ☐

Result: Automatic deployment (no tests)
```

### 4. Deploy to Production
```
Build Parameters:
- ENVIRONMENT: prod
- RUN_MUTATION_TESTS: ☐
- SKIP_DEPLOY: ☐

Result: 
1. Build completes
2. Email sent for approval
3. Wait for manual approval (30 min timeout)
4. Rolling update starts (3 instances)
5. Each instance updated sequentially with health checks
```

## 🔧 Environment Configuration

### Development Environment
- **Port**: 8081
- **Database**: PostgreSQL on 5433
- **RabbitMQ**: 5673 (Management: 15673)
- **Profile**: `dev`
- **Features**: Full logging, SQL visible, auto-schema update

### Staging Environment
- **Port**: 8082
- **Database**: PostgreSQL on 5434
- **RabbitMQ**: 5674 (Management: 15674)
- **Profile**: `staging`
- **Features**: Moderate logging, schema validation

### Production Environment
- **Ports**: 8083, 8084, 8085 (3 instances)
- **Database**: PostgreSQL on 5435
- **RabbitMQ**: 5675 (Management: 15675)
- **Profile**: `prod`
- **Features**: Minimal logging, connection pooling, health checks

## 🧪 Testing Strategy

### Unit Tests (Dev Only)
```bash
mvn test
```
- Runs all JUnit tests
- Results published in Jenkins
- Required threshold: All tests must pass

### Mutation Tests (Dev Only)
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```
- Uses PITest for mutation testing
- Thresholds: 60% mutation coverage, 70% line coverage
- HTML report generated in `target/pit-reports/`
- Published in Jenkins build page

## 🔄 Rolling Update Process

The production deployment uses a rolling update strategy:

```
┌─────────────────────────────────────────────────┐
│  Rolling Update Flow (Zero Downtime)            │
├─────────────────────────────────────────────────┤
│                                                  │
│  Step 1: Update Instance 1                      │
│    ├── Stop lmsbooks_prod_1                     │
│    ├── Start new version                        │
│    ├── Health check (12 retries × 5s)           │
│    └── Wait 10s                                  │
│                                                  │
│  Step 2: Update Instance 2                      │
│    ├── Stop lmsbooks_prod_2                     │
│    ├── Start new version                        │
│    ├── Health check (12 retries × 5s)           │
│    └── Wait 10s                                  │
│                                                  │
│  Step 3: Update Instance 3                      │
│    ├── Stop lmsbooks_prod_3                     │
│    ├── Start new version                        │
│    └── Health check (12 retries × 5s)           │
│                                                  │
│  ✅ Deployment Complete                         │
│     All instances running new version           │
└─────────────────────────────────────────────────┘
```

**Benefits:**
- ✅ Zero downtime - always 2/3 instances running
- ✅ Gradual rollout - issues detected early
- ✅ Automatic health checks - ensures stability
- ✅ Easy rollback - if any instance fails

## 📧 Email Notifications

The pipeline sends emails for:

1. **Production Approval Request**
   - When: Before production deployment
   - Recipients: Configured approver
   - Contains: Build info, approval link

2. **Build Success**
   - When: Pipeline completes successfully
   - Contains: Environment, build number, image tag

3. **Build Failure**
   - When: Pipeline fails
   - Contains: Error details, console link

## 🏥 Health Checks

Each production instance has health checks:
```yaml
healthcheck:
  test: curl -f http://localhost:8080/actuator/health
  interval: 10s
  timeout: 5s
  retries: 3
  start_period: 40s
```

Access health endpoints:
```bash
# Development
curl http://localhost:8081/actuator/health

# Staging
curl http://localhost:8082/actuator/health

# Production
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

## 🛠️ Manual Operations

### Manual Rolling Update
```bash
# Linux/Mac
cd P2/lms_books_command
chmod +x rolling-update.sh
./rolling-update.sh <image-tag>

# Windows
cd P2\lms_books_command
rolling-update.bat <image-tag>
```

### View Running Services
```bash
# Dev
docker-compose -f docker-compose-dev.yml ps

# Staging
docker-compose -f docker-compose-staging.yml ps

# Production
docker-compose -f docker-compose-prod.yml ps
```

### View Logs
```bash
# Dev
docker-compose -f docker-compose-dev.yml logs -f

# Production (specific instance)
docker logs -f lmsbooks_prod_1
```

### Stop Services
```bash
# Dev
docker-compose -f docker-compose-dev.yml down

# Staging
docker-compose -f docker-compose-staging.yml down

# Production
docker-compose -f docker-compose-prod.yml down
```

## 📊 Pipeline Stages

```
┌──────────────────────────────────────────────────────┐
│  Jenkins Pipeline Stages                              │
├──────────────────────────────────────────────────────┤
│  1. Initialization          - Display build info      │
│  2. Environment Check       - Verify tools            │
│  3. Checkout               - Get code from Git        │
│  4. Build & Compile        - Maven compile            │
│  5. Unit Tests             - JUnit tests (dev only)   │
│  6. Mutation Tests         - PITest (dev only)        │
│  7. Package                - Create JAR               │
│  8. Build Docker Image     - Create container         │
│  9. Deploy to Dev          - Automatic (dev only)     │
│ 10. Deploy to Staging      - Automatic (staging only) │
│ 11. Manual Approval        - Email + Wait (prod only) │
│ 12. Rolling Update         - 3 instances (prod only)  │
│ 13. Verification           - Check deployment         │
│ 14. Cleanup                - Remove old images        │
└──────────────────────────────────────────────────────┘
```

## 📋 Requirements Checklist

- ✅ **Three Environments**: dev, staging, prod
- ✅ **Automatic Deployment**: dev and staging deploy automatically
- ✅ **Manual Approval**: prod requires approval with email notification
- ✅ **Rolling Update**: Implemented for production with 3 instances
- ✅ **Unit Tests**: Run in dev environment
- ✅ **Mutation Tests**: Run in dev environment with PITest
- ✅ **Zero Downtime**: Always have healthy instances during deployment
- ✅ **Health Checks**: Automatic verification after each update
- ✅ **Email Notifications**: Approval requests and build status
- ✅ **Docker Integration**: All environments containerized
- ✅ **Environment-Specific Config**: Separate properties files

## 📚 Documentation

- **JENKINS_SETUP_GUIDE.md** - Step-by-step Jenkins configuration
- **JENKINS_PIPELINE_DOCUMENTATION.md** - Complete pipeline documentation
- **This README** - Overview and quick reference

## 🔍 Troubleshooting

### Build Fails
1. Check console output in Jenkins
2. Verify Maven dependencies: `mvn dependency:resolve`
3. Check Java version: Java 17 required

### Tests Fail
1. Review test reports in Jenkins
2. Run locally: `mvn test`
3. Check PITest report for mutation coverage

### Deployment Fails
1. Check Docker logs: `docker logs <container>`
2. Verify health endpoints are accessible
3. Check database connectivity
4. Verify RabbitMQ is running

### Rolling Update Issues
1. Check health check configuration
2. Verify all instances can start
3. Review rolling-update script logs
4. Check available resources (CPU, memory, disk)

## 👥 Contributors

This CI/CD pipeline implements industry best practices for:
- Continuous Integration (CI)
- Continuous Deployment (CD)
- Zero-downtime deployments
- Automated testing
- Manual approval workflows

## 📞 Support

For questions or issues:
1. Review the documentation files
2. Check Jenkins console output
3. Review Docker logs
4. Consult with the DevOps team

---

**Note**: Remember to update the `EMAIL_RECIPIENT` variable in the Jenkinsfile with your actual email address before using the pipeline.

