#!/bin/bash
# ============================================================================
# Auto-Scaling Script for LMS Books Command Service
# ============================================================================
# This script reads load test results and scales the service accordingly
# using Docker Swarm's docker service scale command
# ============================================================================

set -e

# Configuration
SERVICE_NAME="${SERVICE_NAME:-lmsbooks-staging_lmsbooks_command_staging}"
RESULTS_FILE="${RESULTS_FILE:-/tmp/load_test_results.json}"
MIN_REPLICAS="${MIN_REPLICAS:-1}"
MAX_REPLICAS="${MAX_REPLICAS:-5}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "LMS BOOKS COMMAND - AUTO-SCALING"
echo "=========================================="
echo "Service Name: ${SERVICE_NAME}"
echo "Results File: ${RESULTS_FILE}"
echo "Min Replicas: ${MIN_REPLICAS}"
echo "Max Replicas: ${MAX_REPLICAS}"
echo "=========================================="
echo ""

# Check if results file exists
if [ ! -f "${RESULTS_FILE}" ]; then
    echo -e "${RED}❌ ERROR: Results file not found: ${RESULTS_FILE}${NC}"
    echo "Please run load tests first."
    exit 1
fi

# Read results from JSON file
echo "Reading load test results..."
echo ""

# Parse JSON using jq or python
if command -v jq &> /dev/null; then
    RECOMMENDATION=$(jq -r '.scaling.recommendation' "${RESULTS_FILE}")
    RECOMMENDED_REPLICAS=$(jq -r '.scaling.recommended_replicas' "${RESULTS_FILE}")
    SUCCESS_RATE=$(jq -r '.results.success_rate' "${RESULTS_FILE}")
    RPS=$(jq -r '.results.requests_per_second' "${RESULTS_FILE}")
    AVG_RESPONSE_TIME=$(jq -r '.results.response_time_ms.average' "${RESULTS_FILE}")
elif command -v python3 &> /dev/null; then
    RECOMMENDATION=$(python3 -c "import json; data=json.load(open('${RESULTS_FILE}')); print(data['scaling']['recommendation'])")
    RECOMMENDED_REPLICAS=$(python3 -c "import json; data=json.load(open('${RESULTS_FILE}')); print(data['scaling']['recommended_replicas'])")
    SUCCESS_RATE=$(python3 -c "import json; data=json.load(open('${RESULTS_FILE}')); print(data['results']['success_rate'])")
    RPS=$(python3 -c "import json; data=json.load(open('${RESULTS_FILE}')); print(data['results']['requests_per_second'])")
    AVG_RESPONSE_TIME=$(python3 -c "import json; data=json.load(open('${RESULTS_FILE}')); print(data['results']['response_time_ms']['average'])")
else
    echo -e "${RED}❌ ERROR: Neither jq nor python3 is available for JSON parsing${NC}"
    exit 1
fi

echo "=========================================="
echo "LOAD TEST RESULTS SUMMARY"
echo "=========================================="
echo -e "${BLUE}Performance Metrics:${NC}"
echo "  Success Rate:            ${SUCCESS_RATE}%"
echo "  Requests per Second:     ${RPS} RPS"
echo "  Avg Response Time:       ${AVG_RESPONSE_TIME} ms"
echo ""
echo -e "${BLUE}Scaling Analysis:${NC}"
echo "  Recommendation:          ${RECOMMENDATION}"
echo "  Recommended Replicas:    ${RECOMMENDED_REPLICAS}"
echo "=========================================="
echo ""

# Get current number of replicas
echo "Checking current service status..."
CURRENT_REPLICAS=$(docker service inspect "${SERVICE_NAME}" --format '{{.Spec.Mode.Replicated.Replicas}}' 2>/dev/null || echo "0")

if [ "${CURRENT_REPLICAS}" = "0" ]; then
    echo -e "${YELLOW}⚠️ Service ${SERVICE_NAME} not found or has 0 replicas${NC}"
    echo "Attempting to find the service..."

    # Try to find service with similar name
    FOUND_SERVICE=$(docker service ls --format "{{.Name}}" | grep -i "lmsbooks.*command" | head -1 || echo "")

    if [ -n "${FOUND_SERVICE}" ]; then
        echo "Found service: ${FOUND_SERVICE}"
        SERVICE_NAME="${FOUND_SERVICE}"
        CURRENT_REPLICAS=$(docker service inspect "${SERVICE_NAME}" --format '{{.Spec.Mode.Replicated.Replicas}}')
    else
        echo -e "${RED}❌ ERROR: Could not find the service${NC}"
        docker service ls
        exit 1
    fi
fi

echo "Current replicas: ${CURRENT_REPLICAS}"
echo ""

# Determine target replicas
TARGET_REPLICAS=${CURRENT_REPLICAS}

case "${RECOMMENDATION}" in
    "scale_up")
        TARGET_REPLICAS=${RECOMMENDED_REPLICAS}
        if [ "${TARGET_REPLICAS}" -gt "${MAX_REPLICAS}" ]; then
            TARGET_REPLICAS=${MAX_REPLICAS}
            echo -e "${YELLOW}⚠️ Recommended replicas (${RECOMMENDED_REPLICAS}) exceeds maximum (${MAX_REPLICAS})${NC}"
        fi
        if [ "${TARGET_REPLICAS}" -le "${CURRENT_REPLICAS}" ]; then
            TARGET_REPLICAS=$((CURRENT_REPLICAS + 1))
            if [ "${TARGET_REPLICAS}" -gt "${MAX_REPLICAS}" ]; then
                TARGET_REPLICAS=${MAX_REPLICAS}
            fi
        fi
        ;;
    "scale_down")
        TARGET_REPLICAS=$((CURRENT_REPLICAS - 1))
        if [ "${TARGET_REPLICAS}" -lt "${MIN_REPLICAS}" ]; then
            TARGET_REPLICAS=${MIN_REPLICAS}
        fi
        ;;
    "none"|*)
        echo -e "${GREEN}✅ No scaling action required${NC}"
        echo "Current configuration is optimal based on load test results."
        ;;
esac

echo "=========================================="
echo "SCALING DECISION"
echo "=========================================="
echo "  Current Replicas:  ${CURRENT_REPLICAS}"
echo "  Target Replicas:   ${TARGET_REPLICAS}"
echo "=========================================="
echo ""

# Execute scaling if needed
if [ "${TARGET_REPLICAS}" -ne "${CURRENT_REPLICAS}" ]; then
    echo -e "${BLUE}Executing scaling operation...${NC}"
    echo ""
    echo "Command: docker service scale ${SERVICE_NAME}=${TARGET_REPLICAS}"
    echo ""

    # Execute the scale command
    docker service scale "${SERVICE_NAME}=${TARGET_REPLICAS}"

    SCALE_EXIT_CODE=$?

    if [ ${SCALE_EXIT_CODE} -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ Scaling command executed successfully${NC}"

        # Wait for scaling to complete
        echo ""
        echo "Waiting for scaling to complete..."
        sleep 10

        # Verify new replica count
        NEW_REPLICAS=$(docker service inspect "${SERVICE_NAME}" --format '{{.Spec.Mode.Replicated.Replicas}}')
        RUNNING_REPLICAS=$(docker service ps "${SERVICE_NAME}" --filter "desired-state=running" --format "{{.ID}}" | wc -l)

        echo ""
        echo "=========================================="
        echo "SCALING VERIFICATION"
        echo "=========================================="
        echo "  Desired Replicas:  ${NEW_REPLICAS}"
        echo "  Running Replicas:  ${RUNNING_REPLICAS}"
        echo ""

        # Show service status
        echo "Service Status:"
        docker service ps "${SERVICE_NAME}" --format "table {{.Name}}\t{{.CurrentState}}\t{{.Error}}" | head -10

        echo ""
        echo "=========================================="

        if [ "${RUNNING_REPLICAS}" -ge "${TARGET_REPLICAS}" ]; then
            echo -e "${GREEN}✅ Scaling completed successfully!${NC}"
            echo "Service ${SERVICE_NAME} now has ${RUNNING_REPLICAS} running replicas."
        else
            echo -e "${YELLOW}⚠️ Scaling in progress...${NC}"
            echo "Some replicas may still be starting."
            echo "Desired: ${TARGET_REPLICAS}, Running: ${RUNNING_REPLICAS}"
        fi
    else
        echo -e "${RED}❌ Scaling command failed with exit code ${SCALE_EXIT_CODE}${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ No scaling needed${NC}"
    echo "Service is already at the optimal replica count (${CURRENT_REPLICAS})."
fi

echo ""
echo "=========================================="
echo "SCALING OPERATION COMPLETE"
echo "=========================================="
echo "  Service:           ${SERVICE_NAME}"
echo "  Previous Replicas: ${CURRENT_REPLICAS}"
echo "  Current Replicas:  ${TARGET_REPLICAS}"
echo "  Recommendation:    ${RECOMMENDATION}"
echo "=========================================="

# Output scaling result for pipeline
cat > /tmp/scaling_result.json << EOF
{
    "timestamp": "$(date -Iseconds)",
    "service_name": "${SERVICE_NAME}",
    "previous_replicas": ${CURRENT_REPLICAS},
    "target_replicas": ${TARGET_REPLICAS},
    "recommendation": "${RECOMMENDATION}",
    "load_test_metrics": {
        "success_rate": ${SUCCESS_RATE},
        "requests_per_second": ${RPS},
        "avg_response_time_ms": ${AVG_RESPONSE_TIME}
    },
    "scaling_executed": $([ "${TARGET_REPLICAS}" -ne "${CURRENT_REPLICAS}" ] && echo "true" || echo "false")
}
EOF

echo ""
echo "Scaling result saved to: /tmp/scaling_result.json"
