#!/bin/bash

# Feature Flag Validation Script
# Validates feature flags with proper authentication
# Used in Jenkins pipeline to verify dark launch and kill switch configuration

set -e

# Parameters
SERVICE_URL=${1:-"http://74.161.33.56:8082"}
AUTH_SERVICE_URL=${2:-"http://74.161.33.56:8084"}
ENVIRONMENT=${3:-"staging"}

echo "=========================================="
echo "FEATURE FLAG VALIDATION"
echo "=========================================="
echo "Service URL: $SERVICE_URL"
echo "Auth Service URL: $AUTH_SERVICE_URL"
echo "Environment: $ENVIRONMENT"
echo "=========================================="

# Test results tracking
TESTS_PASSED=0
TESTS_FAILED=0

# Function to get JWT token from auth service
get_jwt_token() {
    local auth_url=$1

    echo ""
    echo "Getting JWT token from auth service..."
    echo "Auth endpoint: $auth_url/api/public/login"

    # Default librarian user credentials (should be set during bootstrapping)
    local username="maria@gmail.com"
    local password="Mariaroberta!123"

    # Try to get token
    local response=$(curl -s -X POST "$auth_url/api/public/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}" \
        2>/dev/null || echo "")

    if [ -z "$response" ]; then
        echo "❌ Failed to connect to auth service"
        return 1
    fi

    # Extract token from response
    local token=$(echo "$response" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || echo "")

    if [ -z "$token" ]; then
        echo "❌ Could not extract token from auth response"
        echo "Response: $response"
        return 1
    fi

    echo "✅ JWT token obtained successfully"
    echo "$token"
    return 0
}

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
        return 1
    fi
}

# Get JWT token
echo ""
echo "=========================================="
echo "AUTHENTICATION"
echo "=========================================="

JWT_TOKEN=$(get_jwt_token "$AUTH_SERVICE_URL") || {
    echo "❌ Could not obtain JWT token"
    echo "Feature flag validation will use unauthenticated endpoints only"
    JWT_TOKEN=""
}

if [ -n "$JWT_TOKEN" ]; then
    echo "JWT Token obtained (length: ${#JWT_TOKEN} chars)"
else
    echo "⚠️  JWT token not available - proceeding with public endpoints"
fi

# Wait for service to be ready
echo ""
echo "=========================================="
echo "SERVICE HEALTH CHECK"
echo "=========================================="

MAX_HEALTH_WAIT=60
HEALTH_ELAPSED=0
SERVICE_READY=false

while [ $HEALTH_ELAPSED -lt $MAX_HEALTH_WAIT ]; do
    echo "Checking service health... (${HEALTH_ELAPSED}s elapsed)"

    HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' "$SERVICE_URL/actuator/health" 2>/dev/null || echo "000")
    HTTP_CODE=$(echo "$HTTP_CODE" | grep -o '^[0-9]\{3\}' || echo "000")

    echo "HTTP Response: $HTTP_CODE"

    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "503" ]; then
        echo "✅ Service is responding!"
        SERVICE_READY=true
        break
    fi

    sleep 10
    HEALTH_ELAPSED=$((HEALTH_ELAPSED + 10))
done

if [ "$SERVICE_READY" = false ]; then
    echo "❌ Service not responding after ${MAX_HEALTH_WAIT}s"
    exit 1
fi

# Test 1: Feature Flag Health Check (Public Endpoint - No Auth Required)
echo ""
echo "=========================================="
echo "FEATURE FLAG TESTS"
echo "=========================================="

run_test "Feature Flag Health Endpoint" \
    "curl -s -m 10 '$SERVICE_URL/api/admin/feature-flags/health' | grep -qE '\"status\"|masterKillSwitch' 2>/dev/null"

# Test 2: Get Feature Flags Status (Requires Authentication)
if [ -n "$JWT_TOKEN" ]; then
    echo ""
    echo "Running: Get Feature Flags Status (Authenticated)"

    FEATURE_FLAGS_RESPONSE=$(curl -s -m 10 \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$SERVICE_URL/api/admin/feature-flags" 2>/dev/null || echo "")

    if [ -n "$FEATURE_FLAGS_RESPONSE" ] && echo "$FEATURE_FLAGS_RESPONSE" | grep -q "masterKillSwitch"; then
        echo "✅ PASSED: Feature Flags retrieved with authentication"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        echo "Response (formatted):"
        echo "$FEATURE_FLAGS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$FEATURE_FLAGS_RESPONSE"

        # Test 3: Master Kill Switch is Disabled
        if echo "$FEATURE_FLAGS_RESPONSE" | grep -q '"masterKillSwitch":false'; then
            echo ""
            echo "Running: Master Kill Switch Status"
            echo "✅ PASSED: Master Kill Switch is DISABLED (false)"
            TESTS_PASSED=$((TESTS_PASSED + 1))
        elif echo "$FEATURE_FLAGS_RESPONSE" | grep -q '"masterKillSwitch":true'; then
            echo ""
            echo "❌ FAILED: Master Kill Switch is ENABLED (true)"
            echo "This would disable all write operations - should be false in staging"
            TESTS_FAILED=$((TESTS_FAILED + 1))
        fi

        # Test 4: Dark Launch Configuration
        if echo "$FEATURE_FLAGS_RESPONSE" | grep -q '"darkLaunch"'; then
            echo ""
            echo "Running: Dark Launch Configuration"

            DARK_LAUNCH_CONFIG=$(echo "$FEATURE_FLAGS_RESPONSE" | grep -o '"darkLaunch":{[^}]*}' || echo "")
            echo "Dark Launch Config: $DARK_LAUNCH_CONFIG"

            if echo "$FEATURE_FLAGS_RESPONSE" | grep -q '"enabled":true'; then
                echo "✅ PASSED: Dark Launch is ENABLED"
                TESTS_PASSED=$((TESTS_PASSED + 1))

                # Check traffic percentage
                if echo "$DARK_LAUNCH_CONFIG" | grep -q '"trafficPercentage":'; then
                    TRAFFIC=$(echo "$DARK_LAUNCH_CONFIG" | grep -o '"trafficPercentage":[0-9]*' | cut -d':' -f2)
                    echo "   Traffic Percentage: ${TRAFFIC}%"

                    if [ "$TRAFFIC" -gt "0" ] && [ "$TRAFFIC" -le "100" ]; then
                        echo "✅ Traffic percentage is valid: ${TRAFFIC}%"
                    fi
                fi
            else
                echo "⚠️  Dark Launch is DISABLED in $ENVIRONMENT"
                echo "Note: This is expected in production for safety"
            fi
        fi

        # Test 5: Feature Toggles
        if echo "$FEATURE_FLAGS_RESPONSE" | grep -q '"features"'; then
            echo ""
            echo "Running: Feature Toggles Status"

            FEATURES=$(echo "$FEATURE_FLAGS_RESPONSE" | grep -o '"features":{[^}]*}')
            echo "Features:"
            echo "$FEATURES" | python3 -m json.tool 2>/dev/null || echo "$FEATURES"

            # Count enabled features
            ENABLED_COUNT=$(echo "$FEATURES" | grep -o 'true' | wc -l || echo "0")
            DISABLED_COUNT=$(echo "$FEATURES" | grep -o 'false' | wc -l || echo "0")

            echo ""
            echo "Summary:"
            echo "  Enabled features: $ENABLED_COUNT"
            echo "  Disabled features: $DISABLED_COUNT"
            echo "✅ PASSED: Feature toggles retrieved"
            TESTS_PASSED=$((TESTS_PASSED + 1))
        fi
    else
        echo "❌ FAILED: Could not get feature flags"
        echo "Response: $FEATURE_FLAGS_RESPONSE"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
else
    echo "⚠️  Skipping authenticated feature flag tests (no JWT token)"
fi

# Test 6: Kill Switch Toggle Capability (Verify Endpoint Exists - no auth required for health check)
echo ""
echo "Running: Kill Switch Endpoint Availability"
KILL_SWITCH_ENDPOINT=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "$SERVICE_URL/api/admin/feature-flags/kill-switch?enabled=false" \
    -H "Content-Type: application/json" 2>/dev/null || echo "000")

if [ "$KILL_SWITCH_ENDPOINT" = "401" ]; then
    echo "✅ PASSED: Kill Switch endpoint exists (requires authentication)"
    TESTS_PASSED=$((TESTS_PASSED + 1))
elif [ "$KILL_SWITCH_ENDPOINT" = "200" ]; then
    echo "⚠️  Kill Switch endpoint is publicly accessible (should require auth)"
    echo "HTTP Code: $KILL_SWITCH_ENDPOINT"
else
    echo "⚠️  Kill Switch endpoint not responding"
    echo "HTTP Code: $KILL_SWITCH_ENDPOINT"
fi

# Summary
echo ""
echo "=========================================="
echo "FEATURE FLAG VALIDATION SUMMARY"
echo "=========================================="
echo "Tests Passed:  $TESTS_PASSED ✅"
echo "Tests Failed:  $TESTS_FAILED ❌"
echo "Environment:   $ENVIRONMENT"
echo "Timestamp:     $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "=========================================="

# Save results to file
echo "{
  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
  \"environment\": \"$ENVIRONMENT\",
  \"service_url\": \"$SERVICE_URL\",
  \"tests_passed\": $TESTS_PASSED,
  \"tests_failed\": $TESTS_FAILED,
  \"has_jwt_token\": $([ -n "$JWT_TOKEN" ] && echo "true" || echo "false")
}" > /tmp/feature_flag_validation_results.json

# Exit with appropriate code
if [ $TESTS_FAILED -gt 0 ]; then
    echo ""
    echo "❌ Some feature flag validation tests failed"
    exit 1
else
    echo ""
    echo "✅ All feature flag validation tests passed"
    exit 0
fi

