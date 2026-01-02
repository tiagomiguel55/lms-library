import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const requestDuration = new Trend('request_duration');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8091';

// Quick load test - shorter duration for CI/CD pipeline
export const options = {
    scenarios: {
        quick_load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 30 },   // Ramp up
                { duration: '1m', target: 50 },    // Stay at load
                { duration: '30s', target: 100 },  // Peak load
                { duration: '30s', target: 100 },  // Hold peak
                { duration: '30s', target: 0 },    // Ramp down
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000', 'p(99)<5000'],
        errors: ['rate<0.15'],
        http_reqs: ['rate>5'],
    },
};

export default function() {
    // Health check
    const healthRes = http.get(`${BASE_URL}/actuator/health`);
    check(healthRes, { 'health is 200': (r) => r.status === 200 });
    errorRate.add(healthRes.status !== 200);
    requestDuration.add(healthRes.timings.duration);

    // GET lendings
    const lendingsRes = http.get(`${BASE_URL}/api/lendings?page=0&size=10`);
    check(lendingsRes, { 'lendings is 2xx': (r) => r.status >= 200 && r.status < 300 });
    errorRate.add(lendingsRes.status < 200 || lendingsRes.status >= 300);
    requestDuration.add(lendingsRes.timings.duration);

    sleep(Math.random() * 2 + 0.5);
}

export function handleSummary(data) {
    const p95 = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'] : 0;
    const errorRateVal = data.metrics.http_req_failed ? data.metrics.http_req_failed.values.rate : 0;

    let scalingAction = 'MAINTAIN';
    let recommendedReplicas = 2;

    if (p95 > 3000 || errorRateVal > 0.15) {
        scalingAction = 'SCALE_UP';
        recommendedReplicas = 4;
    } else if (p95 > 2000 || errorRateVal > 0.08) {
        scalingAction = 'SCALE_UP';
        recommendedReplicas = 3;
    } else if (p95 < 500 && errorRateVal < 0.01) {
        scalingAction = 'SCALE_DOWN';
        recommendedReplicas = 1;
    }

    const summary = {
        timestamp: new Date().toISOString(),
        metrics: {
            http_reqs: data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0,
            http_req_duration_avg: data.metrics.http_req_duration ? data.metrics.http_req_duration.values.avg : 0,
            http_req_duration_p95: p95,
            http_req_duration_p99: data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'] : 0,
            http_req_failed: errorRateVal,
            vus_max: data.metrics.vus_max ? data.metrics.vus_max.values.max : 0,
        },
        scalingRecommendation: {
            action: scalingAction,
            recommendedReplicas: recommendedReplicas,
        },
    };

    return {
        'load-test-results.json': JSON.stringify(summary, null, 2),
        stdout: `
================================================================================
                         K6 QUICK LOAD TEST SUMMARY
================================================================================
Total Requests:      ${summary.metrics.http_reqs}
Max VUs:             ${summary.metrics.vus_max}
Avg Response Time:   ${summary.metrics.http_req_duration_avg.toFixed(2)} ms
P95 Response Time:   ${summary.metrics.http_req_duration_p95.toFixed(2)} ms
P99 Response Time:   ${summary.metrics.http_req_duration_p99.toFixed(2)} ms
Error Rate:          ${(summary.metrics.http_req_failed * 100).toFixed(2)}%

SCALING RECOMMENDATION: ${scalingAction}
Recommended Replicas:   ${recommendedReplicas}
================================================================================
`,
    };
}

