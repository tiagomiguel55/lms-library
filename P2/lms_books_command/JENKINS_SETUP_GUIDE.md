# Jenkins Pipeline Quick Setup Guide

## Step 1: Configure Jenkins

### Install Required Plugins
1. Go to Jenkins → Manage Jenkins → Manage Plugins
2. Install these plugins:
   - Pipeline
   - Git Plugin
   - Docker Pipeline
   - Email Extension Plugin
   - HTML Publisher Plugin
   - JUnit Plugin

### Configure Email Notifications
1. Go to Jenkins → Manage Jenkins → Configure System
2. Find "Extended E-mail Notification"
3. Configure SMTP server settings:
   - SMTP server: your-smtp-server
   - SMTP port: 587 (or your port)
   - Use authentication: Yes
   - Credentials: Add your email credentials

### Set Environment Variables
1. Go to Jenkins → Manage Jenkins → Configure System
2. Under "Global properties" → "Environment variables":
   - Name: `MAVEN_HOME`, Value: `/usr/share/maven`
   - Name: `JAVA_HOME`, Value: `/usr/lib/jvm/java-17-openjdk-amd64`

## Step 2: Create GitHub Credentials
1. Go to Jenkins → Credentials → System → Global credentials
2. Click "Add Credentials"
3. Kind: Username with password
4. ID: `password_for_github_tiago`
5. Username: Your GitHub username
6. Password: Your GitHub token/password

## Step 3: Create the Pipeline Job

1. Click "New Item"
2. Enter name: `LMS-Books-Command-Pipeline`
3. Select "Pipeline"
4. Click OK

### Configure the Pipeline

**General Section:**
- ✅ Check "This project is parameterized"
- Add Choice Parameter:
  - Name: `ENVIRONMENT`
  - Choices: `dev`, `staging`, `prod`
  - Description: Select deployment environment
  
- Add Boolean Parameter:
  - Name: `RUN_MUTATION_TESTS`
  - Default: ✅ Checked
  - Description: Run mutation tests (only for dev environment)
  
- Add Boolean Parameter:
  - Name: `SKIP_DEPLOY`
  - Default: ☐ Unchecked
  - Description: Skip deployment phase

**Pipeline Section:**
- Definition: Pipeline script from SCM
- SCM: Git
- Repository URL: `https://github.com/tiagomiguel55/lms-library.git`
- Credentials: Select your GitHub credentials
- Branch: `*/main`
- Script Path: `P2/lms_books_command/Jenkinsfile`

**Build Triggers (Optional):**
- ✅ Poll SCM: `H/5 * * * *` (check every 5 minutes)

Click "Save"

## Step 4: Update Jenkinsfile Configuration

Edit the Jenkinsfile and update this line:
```groovy
EMAIL_RECIPIENT = 'your-email@example.com'
```

Replace with your actual email address.

## Step 5: Test the Pipeline

### Test Development Deployment
1. Click "Build with Parameters"
2. Select:
   - ENVIRONMENT: `dev`
   - RUN_MUTATION_TESTS: ✅
   - SKIP_DEPLOY: ☐
3. Click "Build"

**Expected Result:**
- Compiles code
- Runs unit tests
- Runs mutation tests
- Builds Docker image
- Deploys to dev environment automatically

### Test Staging Deployment
1. Click "Build with Parameters"
2. Select:
   - ENVIRONMENT: `staging`
   - SKIP_DEPLOY: ☐
3. Click "Build"

**Expected Result:**
- Compiles code (no tests)
- Builds Docker image
- Deploys to staging environment automatically

### Test Production Deployment
1. Click "Build with Parameters"
2. Select:
   - ENVIRONMENT: `prod`
   - SKIP_DEPLOY: ☐
3. Click "Build"

**Expected Result:**
- Compiles code (no tests)
- Builds Docker image
- **Pauses and sends email for approval**
- After approval → Rolling update deployment (3 instances)

## Step 6: Verify Deployments

### Check Running Containers
```bash
# Development
docker-compose -f docker-compose-dev.yml ps

# Staging
docker-compose -f docker-compose-staging.yml ps

# Production
docker-compose -f docker-compose-prod.yml ps
```

### Access Services

**Development:**
- Service: http://localhost:8081
- Database: localhost:5433
- RabbitMQ: http://localhost:15673

**Staging:**
- Service: http://localhost:8082
- Database: localhost:5434
- RabbitMQ: http://localhost:15674

**Production:**
- Instance 1: http://localhost:8083
- Instance 2: http://localhost:8084
- Instance 3: http://localhost:8085
- Database: localhost:5435
- RabbitMQ: http://localhost:15675

### Health Check Endpoints
```bash
# Dev
curl http://localhost:8081/actuator/health

# Staging
curl http://localhost:8082/actuator/health

# Production (all instances)
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

## Troubleshooting

### Pipeline Not Starting
- Check Jenkins logs: Jenkins → Manage Jenkins → System Log
- Verify Git credentials
- Ensure Jenkinsfile path is correct

### Docker Build Fails
- Check Docker is running: `docker ps`
- Verify Maven build succeeds: `mvn clean package`
- Check disk space: `df -h`

### Email Not Sending
- Verify SMTP configuration in Jenkins
- Check email credentials
- Test email in Jenkins → Manage Jenkins → Configure System → Test Configuration

### Rolling Update Fails
- Check health endpoint is accessible
- Verify all 3 instances can start
- Review Docker logs: `docker logs <container-name>`

## Manual Commands

### Build and Test Locally
```bash
cd P2/lms_books_command

# Clean and compile
mvn clean compile

# Run unit tests
mvn test

# Run mutation tests
mvn org.pitest:pitest-maven:mutationCoverage

# Package application
mvn package -DskipTests
```

### Manual Deployment

**Development:**
```bash
docker-compose -f docker-compose-dev.yml up -d
```

**Staging:**
```bash
docker-compose -f docker-compose-staging.yml up -d
```

**Production (Rolling Update):**
```bash
# Linux/Mac
./rolling-update.sh latest

# Windows
rolling-update.bat latest
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

## Viewing Reports

### Unit Test Report
1. Go to build page
2. Click "Test Result"
3. View test summary and details

### Mutation Test Report
1. Go to build page
2. Click "PITest Mutation Report"
3. View detailed mutation coverage

### Console Output
1. Go to build page
2. Click "Console Output"
3. View full build log

## Best Practices

1. **Always test in dev first** - Run full test suite
2. **Deploy to staging** - Integration testing
3. **Review staging** - Verify everything works
4. **Deploy to production** - With approval
5. **Monitor production** - Check health endpoints

## Support

For issues or questions:
1. Check Jenkins console output
2. Review Docker logs
3. Consult JENKINS_PIPELINE_DOCUMENTATION.md

