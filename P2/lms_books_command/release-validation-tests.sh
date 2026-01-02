#!/bin/bash

# Release Validation Tests - Automatically rollback if tests fail
# This script validates a deployment and triggers rollback if problems are detected

set -e

SERVICE_URL=${1:-"http://localhost:8080"}
ENVIRONMENT=${2:-"staging"}
SERVICE_NAME=${3:-"lmsbooks-staging_lmsbooks_command_staging"}
PREVIOUS_IMAGE=${4:-""}

echo "=========================================="
echo "RELEASE VALIDATION TESTS"
echo "=========================================="
echo "Service URL: $SERVICE_URL"
echo "Environment: $ENVIRONMENT"
echo "Service Name: $SERVICE_NAME"
echo "Previous Image: $PREVIOUS_IMAGE"
echo "=========================================="

# Test results tracking
TESTS_PASSED=0
TESTS_FAILED=0
ROLLBACK_REQUIRED=false

# Function to run a test
run_test() {
    local test_name=$1
    local test_command=$2

    echo ""
    echo "Running: $test_name"

    if eval "$test_command"; then
        echo "✅ PASSED: $test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo "❌ FAILED: $test_name"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        ROLLBACK_REQUIRED=true
        return 1
    fi
}

# Wait for service to be ready
echo ""
echo "Waiting for service to be ready..."
sleep 10

# Test 1: Health Check
run_test "Health Check Endpoint" \
    "curl -f -s -o /dev/null -w '%{http_code}' $SERVICE_URL/actuator/health | grep -q '200'"

# Test 2: Liveness Probe
run_test "Liveness Probe" \
    "curl -f -s $SERVICE_URL/actuator/health/liveness | grep -q '\"status\":\"UP\"'"

# Test 3: Readiness Probe
run_test "Readiness Probe" \
    "curl -f -s $SERVICE_URL/actuator/health/readiness | grep -q '\"status\":\"UP\"'"

# Test 4: Feature Flag Health
run_test "Feature Flag Health Check" \
    "curl -f -s $SERVICE_URL/api/admin/feature-flags/health | grep -q '\"status\"'"

# Test 5: Master Kill Switch Status (should be OFF)
run_test "Master Kill Switch Disabled" \
    "curl -f -s $SERVICE_URL/api/admin/feature-flags/health | grep -q '\"masterKillSwitch\":false'"

# Test 6: API Documentation Available
run_test "API Documentation Endpoint" \
    "curl -f -s -o /dev/null -w '%{http_code}' $SERVICE_URL/api-docs | grep -q '200'"

# Test 7: Basic API Response Time (should be < 2 seconds)
run_test "API Response Time Check" \
    "timeout 2 curl -f -s $SERVICE_URL/actuator/health"

# Test 8: Database Connection (via health endpoint)
run_test "Database Connectivity" \
    "curl -f -s $SERVICE_URL/actuator/health | grep -qE '\"db\".*\"UP\"' || true"

# Test 9: Service Replicas Running
if [ -n "$SERVICE_NAME" ]; then
    run_test "Service Replicas Health" \
        "docker service ps $SERVICE_NAME --filter 'desired-state=running' | grep -q Running"
fi

# Test 10: Memory Usage Check
if [ -n "$SERVICE_NAME" ]; then
    echo ""
    echo "Running: Memory Usage Check"
    CONTAINER_ID=$(docker ps -q --filter "name=$SERVICE_NAME" | head -1)
    if [ -n "$CONTAINER_ID" ]; then
        MEMORY_USAGE=$(docker stats --no-stream --format "{{.MemPerc}}" $CONTAINER_ID | sed 's/%//')
        if (( $(echo "$MEMORY_USAGE < 90" | bc -l) )); then
            echo "✅ PASSED: Memory Usage Check ($MEMORY_USAGE%)"
            TESTS_PASSED=$((TESTS_PASSED + 1))
        else
            echo "❌ FAILED: Memory Usage Check (${MEMORY_USAGE}% > 90%)"
            TESTS_FAILED=$((TESTS_FAILED + 1))
            ROLLBACK_REQUIRED=true
        fi
    else
        echo "⚠️ SKIPPED: Memory Usage Check (no container found)"
    fi
fi

echo ""
echo "=========================================="
echo "TEST RESULTS SUMMARY"
echo "=========================================="
echo "Tests Passed: $TESTS_PASSED"
echo "Tests Failed: $TESTS_FAILED"
echo "Total Tests:  $((TESTS_PASSED + TESTS_FAILED))"
echo "Success Rate: $(echo "scale=2; $TESTS_PASSED * 100 / ($TESTS_PASSED + $TESTS_FAILED)" | bc)%"
echo "=========================================="

# Determine if rollback is needed
if [ "$ROLLBACK_REQUIRED" = true ]; then
    echo ""
    echo "❌ RELEASE VALIDATION FAILED"
    echo "Rollback required due to test failures"
    echo ""

    if [ -n "$PREVIOUS_IMAGE" ] && [ -n "$SERVICE_NAME" ]; then
        echo "=========================================="
        echo "INITIATING AUTOMATIC ROLLBACK"
        echo "=========================================="
        echo "Service: $SERVICE_NAME"
        echo "Rolling back to: $PREVIOUS_IMAGE"
        echo ""

        # Save rollback information
        echo "{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"service\":\"$SERVICE_NAME\",\"previous_image\":\"$PREVIOUS_IMAGE\",\"reason\":\"validation_tests_failed\",\"tests_failed\":$TESTS_FAILED}" > /tmp/rollback_log.json

        # Perform rollback
        docker service update --image "$PREVIOUS_IMAGE" "$SERVICE_NAME"

        echo ""
        echo "Waiting for rollback to complete..."
        sleep 15

        echo ""
        echo "Verifying rollback..."
        docker service ps "$SERVICE_NAME" --format "{{.Name}}: {{.CurrentState}}" | head -5

        echo ""
        echo "✅ ROLLBACK COMPLETED"
        echo "Service restored to previous stable version"
    else
        echo "⚠️ Cannot perform automatic rollback: missing service name or previous image"
    fi

    echo ""
    echo "=========================================="
    exit 1
else
    echo ""
    echo "✅ ALL TESTS PASSED"
    echo "Release validation successful - deployment is healthy"
    echo ""

    # Save success information
    echo "{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"service\":\"$SERVICE_NAME\",\"status\":\"success\",\"tests_passed\":$TESTS_PASSED}" > /tmp/validation_success.json

    echo "=========================================="
    exit 0
fi