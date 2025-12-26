# LMS Microservices Docker Swarm Shutdown Script
# This script removes all deployed stacks

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "LMS Microservices - Docker Swarm Shutdown" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

Write-Host "`nRemoving Lendings Query stack..." -ForegroundColor Yellow
docker stack rm lendings_query_stack

Write-Host "Removing Lendings Command stack..." -ForegroundColor Yellow
docker stack rm lendings_command_stack

Write-Host "Removing Books Query stack..." -ForegroundColor Yellow
docker stack rm books_query_stack

Write-Host "Removing Books Command stack..." -ForegroundColor Yellow
docker stack rm books_command_stack

Write-Host "Removing Shared infrastructure stack..." -ForegroundColor Yellow
docker stack rm shared_stack

Write-Host "`nWaiting for stacks to be removed (10 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 10

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "All stacks removed!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan

Write-Host "`nNote: Network and volumes are preserved." -ForegroundColor Yellow
Write-Host "To remove the network: docker network rm lms_network" -ForegroundColor Gray
Write-Host "To prune volumes: docker volume prune" -ForegroundColor Gray
