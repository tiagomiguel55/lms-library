/**
 * K6 Load Test Script for LMS Books Command Service
 * ============================================================================
 * This script performs load testing on the staging environment to:
 * - Demonstrate scalability with multiple instances
 * - Test system behavior under load
 * - Compare microservices performance vs monolith baseline
 * - Output metrics for auto-scaling decisions
 * ============================================================================
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const successRate = new Rate('success_rate');
const requestDuration = new Trend('request_duration');
const requestsTotal = new Counter('requests_total');
const requestsFailed = new Counter('requests_failed');

// Configuration from environment variables
const SERVICE_URL = __ENV.SERVICE_URL || 'http://74.161.33.56:8082';
const MONOLITH_BASELINE_RPS = parseFloat(__ENV.MONOLITH_BASELINE_RPS) || 50;

// Test configuration
export const options = {
    scenarios: {
        // Warm-up phase
        warmup: {
            executor: 'constant-vus',
            vus: 2,
            duration: '10s',
            startTime: '0s',
            tags: { phase: 'warmup' },
        },
        // Ramp-up load test
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },   // Ramp up to 10 users
                { duration: '1m', target: 10 },    // Stay at 10 users
                { duration: '30s', target: 25 },   // Ramp up to 25 users
                { duration: '1m', target: 25 },    // Stay at 25 users
                { duration: '30s', target: 50 },   // Ramp up to 50 users
                { duration: '1m', target: 50 },    // Stay at 50 users (peak load)
                { duration: '30s', target: 0 },    // Ramp down
            ],
            startTime: '10s',
            tags: { phase: 'load_test' },
        },
        // Stress test - push the system
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 100 },  // Ramp to 100 users
                { duration: '30s', target: 100 },  // Stay at 100 users
                { duration: '20s', target: 0 },    // Ramp down
            ],
            startTime: '5m10s',
            tags: { phase: 'stress_test' },
        },
    },
    thresholds: {
        // Success rate should be above 95%
        'success_rate': ['rate>0.95'],
        // 95% of requests should be below 500ms, average should be below 200ms
        'http_req_duration': ['p(95)<500', 'avg<200'],
        // Failed requests should be less than 5%
        'http_req_failed': ['rate<0.05'],
    },
};

// Setup function - runs once before the test
export function setup() {
    console.log('========================================');
    console.log('LMS BOOKS COMMAND - K6 LOAD TEST');
    console.log('========================================');
    console.log(`Service URL: ${SERVICE_URL}`);
    console.log(`Monolith Baseline RPS: ${MONOLITH_BASELINE_RPS}`);
    console.log('========================================');

    // Health check
    const healthRes = http.get(`${SERVICE_URL}/actuator/health`, {
        timeout: '10s',
    });

    if (healthRes.status !== 200) {
        console.log(`Warning: Health check returned status ${healthRes.status}`);
        // Try base URL
        const baseRes = http.get(`${SERVICE_URL}/api/books`);
        if (baseRes.status !== 200) {
            console.log('ERROR: Service is not available!');
        }
    } else {
        console.log('✅ Service health check passed');
    }

    return {
        serviceUrl: SERVICE_URL,
        monolithBaseline: MONOLITH_BASELINE_RPS,
        startTime: new Date().toISOString(),
    };
}

// Main test function - runs for each VU iteration
export default function (data) {
    const endpoints = [
        { method: 'GET', path: '/api/books', name: 'List Books' },
        { method: 'GET', path: '/api/books?page=0&size=10', name: 'List Books Paginated' },
    ];

    // Randomly select an endpoint
    const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
    const url = `${data.serviceUrl}${endpoint.path}`;

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
        },
        tags: { endpoint: endpoint.name },
    };

    const startTime = Date.now();
    let res;

    if (endpoint.method === 'GET') {
        res = http.get(url, params);
    } else if (endpoint.method === 'POST') {
        res = http.post(url, JSON.stringify(endpoint.body), params);
    }

    const duration = Date.now() - startTime;

    // Record custom metrics
    requestsTotal.add(1);
    requestDuration.add(duration);

    const isSuccess = res.status >= 200 && res.status < 400;
    successRate.add(isSuccess);

    if (!isSuccess) {
        requestsFailed.add(1);
    }

    // Validate response
    check(res, {
        'status is 2xx or 3xx': (r) => r.status >= 200 && r.status < 400,
        'response time < 500ms': (r) => r.timings.duration < 500,
        'response has body': (r) => r.body && r.body.length > 0,
    });

    // Random sleep between requests (simulating user think time)
    sleep(Math.random() * 0.5 + 0.1);
}

// Teardown function - runs once after the test
export function teardown(data) {
    console.log('========================================');
    console.log('LOAD TEST COMPLETED');
    console.log('========================================');
    console.log(`Start Time: ${data.startTime}`);
    console.log(`End Time: ${new Date().toISOString()}`);
    console.log('========================================');
}

// Handle summary - generate JSON output for pipeline consumption
export function handleSummary(data) {
    const metrics = data.metrics;

    // Calculate key metrics
    const totalRequests = metrics.http_reqs ? metrics.http_reqs.values.count : 0;
    const failedRequests = metrics.http_req_failed ? metrics.http_req_failed.values.passes : 0;
    const successfulRequests = totalRequests - failedRequests;
    const successRateValue = totalRequests > 0 ? (successfulRequests / totalRequests) * 100 : 0;

    const avgResponseTime = metrics.http_req_duration ? metrics.http_req_duration.values.avg : 0;
    const minResponseTime = metrics.http_req_duration ? metrics.http_req_duration.values.min : 0;
    const maxResponseTime = metrics.http_req_duration ? metrics.http_req_duration.values.max : 0;
    const p95ResponseTime = metrics.http_req_duration ? metrics.http_req_duration.values['p(95)'] : 0;

    const testDuration = data.state.testRunDurationMs / 1000;
    const requestsPerSecond = testDuration > 0 ? totalRequests / testDuration : 0;

    // Calculate performance comparison with monolith
    const performanceImprovement = MONOLITH_BASELINE_RPS > 0
        ? ((requestsPerSecond - MONOLITH_BASELINE_RPS) / MONOLITH_BASELINE_RPS) * 100
        : 0;

    // Determine scaling recommendation
    let scaleRecommendation = 'none';
    let recommendedReplicas = 1;

    if (successRateValue < 95) {
        scaleRecommendation = 'scale_up';
        recommendedReplicas = 3;
    } else if (avgResponseTime > 500) {
        scaleRecommendation = 'scale_up';
        recommendedReplicas = 2;
    } else if (requestsPerSecond < MONOLITH_BASELINE_RPS) {
        scaleRecommendation = 'scale_up';
        recommendedReplicas = 2;
    } else if (successRateValue >= 99 && avgResponseTime < 100) {
        scaleRecommendation = 'none';
        recommendedReplicas = 1;
    }

    // Build summary output
    const summary = {
        timestamp: new Date().toISOString(),
        service_url: SERVICE_URL,
        test_configuration: {
            scenarios: Object.keys(options.scenarios),
            thresholds: options.thresholds,
        },
        results: {
            total_requests: totalRequests,
            successful_requests: successfulRequests,
            failed_requests: failedRequests,
            success_rate: parseFloat(successRateValue.toFixed(2)),
            total_duration_seconds: parseFloat(testDuration.toFixed(2)),
            requests_per_second: parseFloat(requestsPerSecond.toFixed(2)),
            response_time_ms: {
                average: parseFloat(avgResponseTime.toFixed(2)),
                minimum: parseFloat(minResponseTime.toFixed(2)),
                maximum: parseFloat(maxResponseTime.toFixed(2)),
                p95: parseFloat(p95ResponseTime.toFixed(2)),
            },
        },
        comparison: {
            monolith_baseline_rps: MONOLITH_BASELINE_RPS,
            microservice_rps: parseFloat(requestsPerSecond.toFixed(2)),
            performance_improvement_percent: parseFloat(performanceImprovement.toFixed(2)),
        },
        scaling: {
            recommendation: scaleRecommendation,
            recommended_replicas: recommendedReplicas,
        },
        thresholds_passed: !data.thresholds || Object.values(data.thresholds).every(t => t.ok),
    };

    // Console output
    console.log('\n========================================');
    console.log('LOAD TEST RESULTS SUMMARY');
    console.log('========================================');
    console.log(`Total Requests:        ${totalRequests}`);
    console.log(`Successful Requests:   ${successfulRequests}`);
    console.log(`Failed Requests:       ${failedRequests}`);
    console.log(`Success Rate:          ${successRateValue.toFixed(2)}%`);
    console.log('----------------------------------------');
    console.log(`Avg Response Time:     ${avgResponseTime.toFixed(2)} ms`);
    console.log(`Min Response Time:     ${minResponseTime.toFixed(2)} ms`);
    console.log(`Max Response Time:     ${maxResponseTime.toFixed(2)} ms`);
    console.log(`P95 Response Time:     ${p95ResponseTime.toFixed(2)} ms`);
    console.log('----------------------------------------');
    console.log(`Test Duration:         ${testDuration.toFixed(2)} seconds`);
    console.log(`Requests/Second:       ${requestsPerSecond.toFixed(2)} RPS`);
    console.log('========================================');
    console.log('COMPARISON WITH MONOLITH');
    console.log('========================================');
    console.log(`Monolith Baseline:     ${MONOLITH_BASELINE_RPS} RPS`);
    console.log(`Microservice:          ${requestsPerSecond.toFixed(2)} RPS`);
    console.log(`Performance Diff:      ${performanceImprovement >= 0 ? '+' : ''}${performanceImprovement.toFixed(2)}%`);
    console.log('========================================');
    console.log('SCALING RECOMMENDATION');
    console.log('========================================');
    console.log(`Recommendation:        ${scaleRecommendation}`);
    console.log(`Recommended Replicas:  ${recommendedReplicas}`);
    console.log('========================================');

    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        '/tmp/load_test_results.json': JSON.stringify(summary, null, 2),
    };
}

// Text summary helper
function textSummary(data, options) {
    const indent = options.indent || '';
    let summary = '';

    summary += `${indent}========================================\n`;
    summary += `${indent}K6 LOAD TEST EXECUTION COMPLETE\n`;
    summary += `${indent}========================================\n`;

    if (data.metrics) {
        summary += `${indent}HTTP Requests: ${data.metrics.http_reqs?.values?.count || 0}\n`;
        summary += `${indent}HTTP Failures: ${data.metrics.http_req_failed?.values?.passes || 0}\n`;
        summary += `${indent}Avg Duration:  ${(data.metrics.http_req_duration?.values?.avg || 0).toFixed(2)}ms\n`;
    }

    summary += `${indent}========================================\n`;

    return summary;
}
