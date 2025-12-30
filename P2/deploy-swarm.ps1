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

# Build Docker images for all microservices
Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "Building Docker Images" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

Write-Host "`n[1/6] Building Books Command image..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_books_command
docker build -t lmsbooks:command .
if ($LASTEXITCODE -eq 0) {
    Write-Host "Books Command image built successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to build Books Command image" -ForegroundColor Red
    exit 1
}

Write-Host "`n[2/6] Building Books Query image..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_books_query
docker build -t lmsbooks:query .
if ($LASTEXITCODE -eq 0) {
    Write-Host "Books Query image built successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to build Books Query image" -ForegroundColor Red
    exit 1
}

Write-Host "`n[3/6] Building Readers Command image..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_readers_command
docker build -t lmsreaders:command .
if ($LASTEXITCODE -eq 0) {
    Write-Host "Readers Command image built successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to build Readers Command image" -ForegroundColor Red
    exit 1
}

Write-Host "`n[4/6] Building Readers Query image..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_readers_query
docker build -t lmsreaders:query .
if ($LASTEXITCODE -eq 0) {
    Write-Host "Readers Query image built successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to build Readers Query image" -ForegroundColor Red
    exit 1
}

Write-Host "`n[5/6] Building Lendings Command image..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_lendings_command
docker build -t lmslendings:command .
if ($LASTEXITCODE -eq 0) {
    Write-Host "Lendings Command image built successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to build Lendings Command image" -ForegroundColor Red
    exit 1
}

Write-Host "`n[6/6] Building Lendings Query image..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_lendings_query
docker build -t lmslendings:query .
if ($LASTEXITCODE -eq 0) {
    Write-Host "Lendings Query image built successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to build Lendings Query image" -ForegroundColor Red
    exit 1
}

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "All Images Built Successfully" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan

# Ask user if they want to clean volumes and databases
Write-Host "`n" -NoNewline
$cleanVolumes = Read-Host "Do you want to clean database volumes and delete all databases? This will delete all existing data (y/N)"
if ($cleanVolumes -eq 'y' -or $cleanVolumes -eq 'Y') {
    Write-Host "`nCleaning Docker stacks and databases..." -ForegroundColor Yellow
    
    # Remove all existing stacks to stop database containers
    Write-Host "Removing existing stacks..." -ForegroundColor Gray
    docker stack rm shared_stack 2>$null
    docker stack rm books_command_stack 2>$null
    docker stack rm books_query_stack 2>$null
    docker stack rm readers_command_stack 2>$null
    docker stack rm readers_query_stack 2>$null
    docker stack rm lendings_command_stack 2>$null
    docker stack rm lendings_query_stack 2>$null
    
    # Wait for services to be removed
    Start-Sleep -Seconds 5
    
    # Remove all Docker volumes
    Write-Host "Removing all volumes..." -ForegroundColor Gray
    docker volume prune -f
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Stacks and volumes cleaned successfully" -ForegroundColor Green
    } else {
        Write-Host "Warning: Cleanup encountered issues" -ForegroundColor Yellow
    }
} else {
    Write-Host "Skipping cleanup - existing data will be preserved" -ForegroundColor Gray
}

# Deploy shared infrastructure (RabbitMQ)
Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "Deploying Microservices" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

Write-Host "`n[1/7] Deploying shared infrastructure (RabbitMQ)..." -ForegroundColor Yellow
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
Write-Host "`n[2/7] Deploying Books Command microservice (PostgreSQL)..." -ForegroundColor Yellow
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
Write-Host "`n[3/7] Deploying Books Query microservice (MongoDB)..." -ForegroundColor Yellow
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

# Deploy Readers Command microservice
Write-Host "`n[4/7] Deploying Readers Command microservice (PostgreSQL)..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_readers_command
docker stack deploy -c docker-compose-swarm.yml readers_command_stack
if ($LASTEXITCODE -eq 0) {
    Write-Host "Readers Command deployed successfully (2 replicas)" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy Readers Command" -ForegroundColor Red
}

# Wait for databases to initialize
Write-Host "Waiting for Readers Command database to initialize (15 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 15

# Deploy Readers Query microservice
Write-Host "`n[5/7] Deploying Readers Query microservice (PostgreSQL)..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot\lms_readers_query
docker stack deploy -c docker-compose-swarm.yml readers_query_stack
if ($LASTEXITCODE -eq 0) {
    Write-Host "Readers Query deployed successfully (2 replicas)" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy Readers Query" -ForegroundColor Red
}

# Wait for databases to initialize
Write-Host "Waiting for Readers Query database to initialize (15 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 15

# Deploy Lendings Command microservice
Write-Host "`n[6/7] Deploying Lendings Command microservice (PostgreSQL)..." -ForegroundColor Yellow
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
Write-Host "`n[7/7] Deploying Lendings Query microservice (PostgreSQL)..." -ForegroundColor Yellow
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
Write-Host "  - Readers Command:     http://localhost:8082" -ForegroundColor White
Write-Host "  - Readers Query:       http://localhost:8086" -ForegroundColor White
Write-Host "  - Lendings Command:    http://localhost:8090" -ForegroundColor White
Write-Host "  - Lendings Query:      http://localhost:8091" -ForegroundColor White

Write-Host "`nDatabase Ports:" -ForegroundColor Yellow
Write-Host "  - PostgreSQL (Books):    localhost:5432" -ForegroundColor White
Write-Host "  - PostgreSQL (Readers Command): localhost:5434" -ForegroundColor White
Write-Host "  - PostgreSQL (Readers Query):   localhost:5435" -ForegroundColor White
Write-Host "  - PostgreSQL (Lendings Command): localhost:5436" -ForegroundColor White
Write-Host "  - PostgreSQL (Lendings Query):   localhost:5437" -ForegroundColor White
Write-Host "  - MongoDB (Books):       localhost:27017" -ForegroundColor White

Write-Host "`nTo check service status:" -ForegroundColor Yellow
Write-Host "  docker stack ls" -ForegroundColor Gray
Write-Host "  docker service ls" -ForegroundColor Gray
Write-Host "  docker service ps <service_name>" -ForegroundColor Gray
Write-Host "  docker service logs <service_name>" -ForegroundColor Gray

Write-Host "`nNote: Services may take 1-2 minutes to fully start" -ForegroundColor Cyan
Write-Host "Monitor with: docker service ls" -ForegroundColor Cyan

