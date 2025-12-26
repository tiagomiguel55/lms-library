# LMS Microservices Docker Swarm Deployment Script
# This script deploys all microservices with RabbitMQ communication

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "LMS Microservices - Docker Swarm Deployment" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# Check if Docker Swarm is initialized
$swarmStatus = docker info --format '{{.Swarm.LocalNodeState}}'
if ($swarmStatus -ne "active") {
    Write-Host "Initializing Docker Swarm..." -ForegroundColor Yellow
    docker swarm init
}

# Create the external network if it doesn't exist
Write-Host "`nCreating external network 'lms_network'..." -ForegroundColor Yellow
docker network create --driver overlay --attachable lms_network 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Network 'lms_network' created successfully" -ForegroundColor Green
} else {
    Write-Host "Network 'lms_network' already exists" -ForegroundColor Gray
}

# Deploy shared infrastructure (RabbitMQ)
Write-Host "`n[1/5] Deploying shared infrastructure (RabbitMQ)..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\..
docker stack deploy -c docker-compose-swarm-shared.yml shared_stack
if ($LASTEXITCODE -eq 0) {
    Write-Host "Shared infrastructure deployed successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy shared infrastructure" -ForegroundColor Red
    exit 1
}

# Wait for RabbitMQ to be ready with health check
Write-Host "Waiting for RabbitMQ to be ready..." -ForegroundColor Gray
$maxAttempts = 30
$attempt = 0
$rabbitmqReady = $false

while ($attempt -lt $maxAttempts -and -not $rabbitmqReady) {
    $attempt++
    Write-Host "  Checking RabbitMQ health (attempt $attempt/$maxAttempts)..." -ForegroundColor Gray
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:15672" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $rabbitmqReady = $true
            Write-Host "  RabbitMQ is ready!" -ForegroundColor Green
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $rabbitmqReady) {
    Write-Host "RabbitMQ failed to start properly" -ForegroundColor Red
    exit 1
}

# Additional wait to ensure RabbitMQ is fully initialized
Start-Sleep -Seconds 10

# Deploy Books Command microservice
Write-Host "`n[2/5] Deploying Books Command microservice (PostgreSQL)..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_books_command
docker stack deploy -c docker-compose-swarm.yml books_command_stack
if ($LASTEXITCODE -eq 0) {
    Write-Host "Books Command deployed successfully (2 replicas)" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy Books Command" -ForegroundColor Red
}

# Wait for databases to initialize
Write-Host "Waiting for Books Command database to initialize (15 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 15

# Deploy Books Query microservice
Write-Host "`n[3/5] Deploying Books Query microservice (MongoDB)..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_books_query
docker stack deploy -c docker-compose-swarm.yml books_query_stack
if ($LASTEXITCODE -eq 0) {
    Write-Host "Books Query deployed successfully (2 replicas)" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy Books Query" -ForegroundColor Red
}

# Wait for databases to initialize
Write-Host "Waiting for Books Query database to initialize (15 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 15

# Deploy Lendings Command microservice
Write-Host "`n[4/5] Deploying Lendings Command microservice (PostgreSQL)..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_lendings_command
docker stack deploy -c docker-compose-swarm.yml lendings_command_stack
if ($LASTEXITCODE -eq 0) {
    Write-Host "Lendings Command deployed successfully (2 replicas)" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy Lendings Command" -ForegroundColor Red
}

# Wait for databases to initialize
Write-Host "Waiting for Lendings Command database to initialize (15 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 15

# Deploy Lendings Query microservice
Write-Host "`n[5/5] Deploying Lendings Query microservice (MongoDB)..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_lendings_query
docker stack deploy -c docker-compose-swarm.yml lendings_query_stack
if ($LASTEXITCODE -eq 0) {
    Write-Host "Lendings Query deployed successfully (2 replicas)" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy Lendings Query" -ForegroundColor Red
}

# Return to P2 directory
Set-Location -Path $PSScriptRoot

Write-Host "`nWaiting for services to stabilize (30 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan

Write-Host "`nChecking service health..." -ForegroundColor Yellow
docker service ls

Write-Host "`nService Ports:" -ForegroundColor Yellow
Write-Host "  - RabbitMQ Management: http://localhost:15672 (guest/guest)" -ForegroundColor White
Write-Host "  - Books Command:       http://localhost:8080" -ForegroundColor White
Write-Host "  - Books Query:         http://localhost:8085" -ForegroundColor White
Write-Host "  - Lendings Command:    http://localhost:8090" -ForegroundColor White
Write-Host "  - Lendings Query:      http://localhost:8091" -ForegroundColor White

Write-Host "`nDatabase Ports:" -ForegroundColor Yellow
Write-Host "  - PostgreSQL (Books):    localhost:5432" -ForegroundColor White
Write-Host "  - PostgreSQL (Lendings): localhost:5436" -ForegroundColor White
Write-Host "  - MongoDB (Books):       localhost:27017" -ForegroundColor White
Write-Host "  - MongoDB (Lendings):    localhost:27018" -ForegroundColor White

Write-Host "`nTo check service status:" -ForegroundColor Yellow
Write-Host "  docker stack ls" -ForegroundColor Gray
Write-Host "  docker service ls" -ForegroundColor Gray
Write-Host "  docker service ps <service_name>" -ForegroundColor Gray
Write-Host "  docker service logs <service_name>" -ForegroundColor Gray

Write-Host "`nNote: Services may take 1-2 minutes to fully start" -ForegroundColor Cyan
Write-Host "Monitor with: docker service ls" -ForegroundColor Cyan

