package pt.psoft.g1.psoftg1.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing A/B Testing experiments
 *
 * Provides endpoints to:
 * - Create and configure A/B tests
 * - Adjust traffic distribution
 * - View experiment results
 * - Stop experiments
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ab-testing")
@RequiredArgsConstructor
public class ABTestingController {

    private final ABTestingService abTestingService;

    /**
     * Create a new A/B test experiment
     *
     * Example:
     * POST /api/admin/ab-testing/experiments
     * {
     *   "experimentId": "new-search-algorithm",
     *   "featureName": "experimentalBookRecommendations",
     *   "trafficPercentToB": 10
     * }
     */
    @PostMapping("/experiments")
    public ResponseEntity<ABTestingService.Experiment> createExperiment(
            @RequestBody CreateExperimentRequest request) {

        log.info("Creating A/B test experiment: {}", request.getExperimentId());

        ABTestingService.Experiment experiment = abTestingService.createExperiment(
                request.getExperimentId(),
                request.getFeatureName(),
                request.getTrafficPercentToB()
        );

        return ResponseEntity.ok(experiment);
    }

    /**
     * Get all active experiments
     */
    @GetMapping("/experiments")
    public ResponseEntity<List<ABTestingService.Experiment>> getActiveExperiments() {
        List<ABTestingService.Experiment> experiments = abTestingService.getActiveExperiments();
        return ResponseEntity.ok(experiments);
    }

    /**
     * Get results for a specific experiment
     */
    @GetMapping("/experiments/{experimentId}/results")
    public ResponseEntity<ABTestingService.ExperimentResults> getExperimentResults(
            @PathVariable String experimentId) {

        ABTestingService.ExperimentResults results = abTestingService.getExperimentResults(experimentId);

        if (results == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Update traffic distribution for an experiment
     *
     * Example:
     * PUT /api/admin/ab-testing/experiments/new-search-algorithm/traffic
     * {
     *   "percentToB": 25
     * }
     */
    @PutMapping("/experiments/{experimentId}/traffic")
    public ResponseEntity<Void> updateTrafficDistribution(
            @PathVariable String experimentId,
            @RequestBody UpdateTrafficRequest request) {

        log.info("Updating traffic for experiment {} to {}%", experimentId, request.getPercentToB());

        abTestingService.updateTrafficDistribution(experimentId, request.getPercentToB());

        return ResponseEntity.ok().build();
    }

    /**
     * Stop an experiment
     */
    @PostMapping("/experiments/{experimentId}/stop")
    public ResponseEntity<Void> stopExperiment(@PathVariable String experimentId) {
        log.info("Stopping experiment: {}", experimentId);

        abTestingService.stopExperiment(experimentId);

        return ResponseEntity.ok().build();
    }

    /**
     * Check which variant a user is assigned to
     */
    @GetMapping("/experiments/{experimentId}/variant")
    public ResponseEntity<Map<String, String>> getUserVariant(
            @PathVariable String experimentId,
            @RequestParam String userId) {

        String variant = abTestingService.assignVariant(experimentId, userId);

        return ResponseEntity.ok(Map.of(
                "experimentId", experimentId,
                "userId", userId,
                "variant", variant
        ));
    }

    /**
     * Manually record a metric for testing
     */
    @PostMapping("/experiments/{experimentId}/metrics")
    public ResponseEntity<Void> recordMetric(
            @PathVariable String experimentId,
            @RequestBody RecordMetricRequest request) {

        abTestingService.recordMetric(
                experimentId,
                request.getUserId(),
                request.getMetricName(),
                request.getValue()
        );

        return ResponseEntity.ok().build();
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class CreateExperimentRequest {
        private String experimentId;
        private String featureName;
        private int trafficPercentToB = 10; // Default 10% to variant B
    }

    @lombok.Data
    public static class UpdateTrafficRequest {
        private int percentToB;
    }

    @lombok.Data
    public static class RecordMetricRequest {
        private String userId;
        private String metricName;
        private double value;
    }
}


