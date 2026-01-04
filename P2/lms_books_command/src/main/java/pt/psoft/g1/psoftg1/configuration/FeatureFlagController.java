package pt.psoft.g1.psoftg1.configuration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing Feature Flags and A/B Testing experiments
 * This enables runtime configuration of release strategies without deployment
 */
@Tag(name = "Feature Flags", description = "Endpoints for managing feature flags and A/B experiments")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/feature-flags")
@PreAuthorize("hasRole('ADMIN')")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final ABTestingService abTestingService;

    // ========== Feature Flags Management ==========

    @Operation(summary = "Get current feature flag configuration")
    @GetMapping("/config")
    public ResponseEntity<FeatureFlagConfig> getConfiguration() {
        return ResponseEntity.ok(featureFlagService.getConfiguration());
    }

    @Operation(summary = "Check if master kill switch is active")
    @GetMapping("/kill-switch/status")
    public ResponseEntity<Map<String, Object>> getKillSwitchStatus() {
        boolean active = featureFlagService.isMasterKillSwitchActive();
        return ResponseEntity.ok(Map.of(
                "masterKillSwitch", active,
                "status", active ? "ACTIVE - All features disabled" : "INACTIVE - Normal operation"
        ));
    }

    @Operation(summary = "Check if a specific feature is enabled")
    @GetMapping("/features/{featureName}/status")
    public ResponseEntity<Map<String, Object>> getFeatureStatus(@PathVariable String featureName) {
        boolean enabled = featureFlagService.isFeatureEnabled(featureName);
        return ResponseEntity.ok(Map.of(
                "featureName", featureName,
                "enabled", enabled,
                "masterKillSwitch", featureFlagService.isMasterKillSwitchActive()
        ));
    }

    // ========== A/B Testing Management ==========

    @Operation(summary = "Create a new A/B test experiment")
    @PostMapping("/experiments")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ABTestingService.Experiment> createExperiment(
            @RequestParam String featureName,
            @RequestParam(defaultValue = "50") int trafficPercentToB) {

        if (trafficPercentToB < 0 || trafficPercentToB > 100) {
            return ResponseEntity.badRequest().build();
        }

        ABTestingService.Experiment experiment = abTestingService.createExperiment(featureName, trafficPercentToB);
        return ResponseEntity.status(HttpStatus.CREATED).body(experiment);
    }

    @Operation(summary = "Get all active experiments")
    @GetMapping("/experiments")
    public ResponseEntity<List<ABTestingService.Experiment>> getActiveExperiments() {
        return ResponseEntity.ok(abTestingService.getActiveExperiments());
    }

    @Operation(summary = "Get experiment results and statistics")
    @GetMapping("/experiments/{experimentId}/results")
    public ResponseEntity<ABTestingService.ExperimentResults> getExperimentResults(
            @PathVariable String experimentId) {

        ABTestingService.ExperimentResults results = abTestingService.getExperimentResults(experimentId);

        if (results == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Update traffic distribution for an experiment")
    @PatchMapping("/experiments/{experimentId}/traffic")
    public ResponseEntity<Map<String, String>> updateTrafficDistribution(
            @PathVariable String experimentId,
            @RequestParam int newPercentToB) {

        if (newPercentToB < 0 || newPercentToB > 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Traffic percentage must be between 0 and 100"));
        }

        abTestingService.updateTrafficDistribution(experimentId, newPercentToB);
        return ResponseEntity.ok(Map.of(
                "message", "Traffic distribution updated successfully",
                "experimentId", experimentId,
                "newTrafficPercentToB", String.valueOf(newPercentToB)
        ));
    }

    @Operation(summary = "Stop an active experiment")
    @PostMapping("/experiments/{experimentId}/stop")
    public ResponseEntity<Map<String, String>> stopExperiment(@PathVariable String experimentId) {
        abTestingService.stopExperiment(experimentId);
        return ResponseEntity.ok(Map.of(
                "message", "Experiment stopped successfully",
                "experimentId", experimentId
        ));
    }

    @Operation(summary = "Record a success metric for an experiment")
    @PostMapping("/experiments/{experimentId}/metrics/success")
    public ResponseEntity<Map<String, String>> recordSuccess(
            @PathVariable String experimentId,
            @RequestParam String userId) {

        abTestingService.recordSuccess(experimentId, userId);
        return ResponseEntity.ok(Map.of(
                "message", "Success metric recorded",
                "experimentId", experimentId,
                "userId", userId
        ));
    }

    @Operation(summary = "Record a failure metric for an experiment")
    @PostMapping("/experiments/{experimentId}/metrics/failure")
    public ResponseEntity<Map<String, String>> recordFailure(
            @PathVariable String experimentId,
            @RequestParam String userId) {

        abTestingService.recordFailure(experimentId, userId);
        return ResponseEntity.ok(Map.of(
                "message", "Failure metric recorded",
                "experimentId", experimentId,
                "userId", userId
        ));
    }

    @Operation(summary = "Get user's assigned variant for an experiment")
    @GetMapping("/experiments/{experimentId}/variant")
    public ResponseEntity<Map<String, String>> getUserVariant(
            @PathVariable String experimentId,
            @RequestParam String userId) {

        String variant = abTestingService.assignVariant(experimentId, userId);
        return ResponseEntity.ok(Map.of(
                "experimentId", experimentId,
                "userId", userId,
                "variant", variant,
                "description", variant.equals("A") ? "Control group (feature disabled)" : "Test group (feature enabled)"
        ));
    }

    // ========== Health & Monitoring ==========

    @Operation(summary = "Health check for feature flag system")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "masterKillSwitch", featureFlagService.isMasterKillSwitchActive(),
                "activeExperiments", abTestingService.getActiveExperiments().size()
        ));
    }
}
