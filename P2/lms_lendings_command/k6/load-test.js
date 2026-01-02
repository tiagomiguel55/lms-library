import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const requestDuration = new Trend('request_duration');
const successfulRequests = new Counter('successful_requests');
const failedRequests = new Counter('failed_requests');

// Configuration from environment variables
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8091';
const ENVIRONMENT = __ENV.ENVIRONMENT || 'staging';

// Load test configuration with multiple scenarios
export const options = {
    scenarios: {
        // Scenario 1: Smoke test - Basic functionality check
        smoke_test: {
            executor: 'constant-vus',
            vus: 5,
            duration: '30s',
            startTime: '0s',
            tags: { test_type: 'smoke' },
        },
        // Scenario 2: Load test - Normal expected load
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 20 },   // Ramp up to 20 users
                { duration: '2m', target: 20 },   // Stay at 20 users
                { duration: '1m', target: 50 },   // Ramp up to 50 users
                { duration: '2m', target: 50 },   // Stay at 50 users
                { duration: '1m', target: 0 },    // Ramp down
            ],
            startTime: '30s',
            tags: { test_type: 'load' },
        },
        // Scenario 3: Stress test - Beyond normal capacity
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 100 },  // Ramp up quickly
                { duration: '1m', target: 100 },   // Stay at peak
                { duration: '30s', target: 0 },    // Ramp down
            ],
            startTime: '8m',
            tags: { test_type: 'stress' },
        },
        // Scenario 4: Spike test - Sudden traffic spike
        spike_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 150 },  // Sudden spike
                { duration: '30s', target: 150 },  // Hold spike
                { duration: '10s', target: 0 },    // Quick drop
            ],
            startTime: '10m',
            tags: { test_type: 'spike' },
        },
    },
    thresholds: {
        // Response time thresholds
        http_req_duration: ['p(95)<2000', 'p(99)<5000'],  // 95% under 2s, 99% under 5s
        // Error rate threshold
        errors: ['rate<0.1'],  // Error rate should be below 10%
        // Request rate
        http_reqs: ['rate>10'],  // At least 10 requests per second
    },
    // Summary output
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

// Setup function - runs once before the test
export function setup() {
    console.log(`========================================`);
    console.log(`K6 Load Test - LMS Lendings Command Service`);
    console.log(`========================================`);
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Environment: ${ENVIRONMENT}`);
    console.log(`========================================`);

    // Health check before starting
    const healthRes = http.get(`${BASE_URL}/actuator/health`, { timeout: '10s' });
    if (healthRes.status !== 200) {
        console.warn(`Warning: Health check returned status ${healthRes.status}`);
    }

    return {
        baseUrl: BASE_URL,
        environment: ENVIRONMENT,
        startTime: new Date().toISOString(),
    };
}

// Main test function
export default function(data) {
    const baseUrl = data.baseUrl;

    group('Health & Actuator Endpoints', function() {
        // Health check endpoint
        const healthRes = http.get(`${baseUrl}/actuator/health`);
        check(healthRes, {
            'health check status is 200': (r) => r.status === 200,
            'health check response time < 500ms': (r) => r.timings.duration < 500,
        }) ? successfulRequests.add(1) : failedRequests.add(1);
        errorRate.add(healthRes.status !== 200);
        requestDuration.add(healthRes.timings.duration);
    });

    group('Lendings API Endpoints', function() {
        // GET all lendings (paginated)
        const lendingsRes = http.get(`${baseUrl}/api/lendings?page=0&size=10`);
        const lendingsOk = check(lendingsRes, {
            'GET lendings status is 2xx': (r) => r.status >= 200 && r.status < 300,
            'GET lendings response time < 1000ms': (r) => r.timings.duration < 1000,
        });
        lendingsOk ? successfulRequests.add(1) : failedRequests.add(1);
        errorRate.add(!lendingsOk);
        requestDuration.add(lendingsRes.timings.duration);

        // GET lending by ID (using a sample ID - may return 404 which is acceptable)
        const lendingByIdRes = http.get(`${baseUrl}/api/lendings/1`);
        const lendingByIdOk = check(lendingByIdRes, {
            'GET lending by ID status is 2xx or 404': (r) => (r.status >= 200 && r.status < 300) || r.status === 404,
            'GET lending by ID response time < 1000ms': (r) => r.timings.duration < 1000,
        });
        lendingByIdOk ? successfulRequests.add(1) : failedRequests.add(1);
        errorRate.add(!lendingByIdOk);
        requestDuration.add(lendingByIdRes.timings.duration);

        // GET overdue lendings
        const overdueRes = http.get(`${baseUrl}/api/lendings/overdue`);
        const overdueOk = check(overdueRes, {
            'GET overdue lendings status is 2xx or 404': (r) => (r.status >= 200 && r.status < 300) || r.status === 404,
            'GET overdue response time < 1500ms': (r) => r.timings.duration < 1500,
        });
        overdueOk ? successfulRequests.add(1) : failedRequests.add(1);
        errorRate.add(!overdueOk);
        requestDuration.add(overdueRes.timings.duration);
    });

    group('Search & Filter Operations', function() {
        // Search lendings by reader
        const searchReaderRes = http.get(`${baseUrl}/api/lendings/reader/1`);
        const searchReaderOk = check(searchReaderRes, {
            'Search by reader status is 2xx or 404': (r) => (r.status >= 200 && r.status < 300) || r.status === 404,
            'Search by reader response time < 1500ms': (r) => r.timings.duration < 1500,
        });
        searchReaderOk ? successfulRequests.add(1) : failedRequests.add(1);
        errorRate.add(!searchReaderOk);
        requestDuration.add(searchReaderRes.timings.duration);

        // Search lendings by book ISBN
        const searchBookRes = http.get(`${baseUrl}/api/lendings/book/978-0-123456-78-9`);
        const searchBookOk = check(searchBookRes, {
            'Search by book status is 2xx or 404': (r) => (r.status >= 200 && r.status < 300) || r.status === 404,
            'Search by book response time < 1500ms': (r) => r.timings.duration < 1500,
        });
        searchBookOk ? successfulRequests.add(1) : failedRequests.add(1);
        errorRate.add(!searchBookOk);
        requestDuration.add(searchBookRes.timings.duration);
    });

    // Simulate user think time
    sleep(Math.random() * 2 + 1);  // Sleep between 1-3 seconds
}

// Teardown function - runs once after the test
export function teardown(data) {
    console.log(`========================================`);
    console.log(`Load Test Completed`);
    console.log(`Started: ${data.startTime}`);
    console.log(`Ended: ${new Date().toISOString()}`);
    console.log(`========================================`);
}

// Handle summary - export results as JSON for pipeline processing
export function handleSummary(data) {
    const summary = {
        timestamp: new Date().toISOString(),
        environment: ENVIRONMENT,
        baseUrl: BASE_URL,
        metrics: {
            http_reqs: data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0,
            http_req_duration_avg: data.metrics.http_req_duration ? data.metrics.http_req_duration.values.avg : 0,
            http_req_duration_p95: data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'] : 0,
            http_req_duration_p99: data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'] : 0,
            http_req_failed: data.metrics.http_req_failed ? data.metrics.http_req_failed.values.rate : 0,
            vus_max: data.metrics.vus_max ? data.metrics.vus_max.values.max : 0,
            iterations: data.metrics.iterations ? data.metrics.iterations.values.count : 0,
        },
        thresholds: {
            passed: Object.keys(data.thresholds).filter(k => data.thresholds[k].ok).length,
            failed: Object.keys(data.thresholds).filter(k => !data.thresholds[k].ok).length,
        },
        // Scaling recommendation based on metrics
        scalingRecommendation: calculateScalingRecommendation(data),
    };

    return {
        'load-test-results.json': JSON.stringify(summary, null, 2),
        stdout: generateTextSummary(data, summary),
    };
}

function calculateScalingRecommendation(data) {
    const p95 = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'] : 0;
    const errorRate = data.metrics.http_req_failed ? data.metrics.http_req_failed.values.rate : 0;
    const maxVUs = data.metrics.vus_max ? data.metrics.vus_max.values.max : 0;

    // Scaling logic based on performance metrics
    if (p95 > 3000 || errorRate > 0.15) {
        return {
            action: 'SCALE_UP',
            reason: `High latency (p95: ${p95.toFixed(0)}ms) or error rate (${(errorRate * 100).toFixed(1)}%)`,
            recommendedReplicas: 4,
        };
    } else if (p95 > 2000 || errorRate > 0.08) {
        return {
            action: 'SCALE_UP',
            reason: `Moderate latency (p95: ${p95.toFixed(0)}ms) or error rate (${(errorRate * 100).toFixed(1)}%)`,
            recommendedReplicas: 3,
        };
    } else if (p95 < 500 && errorRate < 0.01 && maxVUs < 20) {
        return {
            action: 'SCALE_DOWN',
            reason: `Low latency (p95: ${p95.toFixed(0)}ms) and minimal load`,
            recommendedReplicas: 1,
        };
    } else {
        return {
            action: 'MAINTAIN',
            reason: `Performance within acceptable thresholds`,
            recommendedReplicas: 2,
        };
    }
}

function generateTextSummary(data, summary) {
    return `
================================================================================
                    K6 LOAD TEST SUMMARY - LMS LENDINGS COMMAND
================================================================================

Environment: ${summary.environment}
Timestamp: ${summary.timestamp}

PERFORMANCE METRICS:
--------------------
Total Requests:          ${summary.metrics.http_reqs}
Total Iterations:        ${summary.metrics.iterations}
Max Virtual Users:       ${summary.metrics.vus_max}

Response Times:
  Average:               ${summary.metrics.http_req_duration_avg.toFixed(2)} ms
  95th Percentile:       ${summary.metrics.http_req_duration_p95.toFixed(2)} ms
  99th Percentile:       ${summary.metrics.http_req_duration_p99.toFixed(2)} ms

Error Rate:              ${(summary.metrics.http_req_failed * 100).toFixed(2)}%

THRESHOLD RESULTS:
------------------
Passed:                  ${summary.thresholds.passed}
Failed:                  ${summary.thresholds.failed}

SCALING RECOMMENDATION:
-----------------------
Action:                  ${summary.scalingRecommendation.action}
Reason:                  ${summary.scalingRecommendation.reason}
Recommended Replicas:    ${summary.scalingRecommendation.recommendedReplicas}

================================================================================
`;
}

