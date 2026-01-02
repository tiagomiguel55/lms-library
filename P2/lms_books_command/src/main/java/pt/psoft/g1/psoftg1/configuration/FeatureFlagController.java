package pt.psoft.g1.psoftg1.configuration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API for managing feature flags and kill switches
 * This enables runtime toggling without redeployment
 *
 * Requires LIBRARIAN role for all operations
 */
@Tag(name = "Feature Flags", description = "Manage feature flags, dark launch, and kill switches")
@RestController
@RequestMapping("/api/admin/feature-flags")
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagController {

    private final FeatureFlagConfig featureFlagConfig;
    private final FeatureFlagService featureFlagService;

    @Operation(summary = "Get all feature flags status")
    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<FeatureFlagStatus> getFeatureFlags() {
        log.info("Retrieving feature flag status");

        FeatureFlagStatus status = new FeatureFlagStatus();
        status.setMasterKillSwitch(featureFlagConfig.isMasterKillSwitch());
        status.setFeatures(featureFlagConfig.getFeatures());
        status.setDarkLaunch(featureFlagConfig.getDarkLaunch());
        status.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Toggle master kill switch - EMERGENCY USE ONLY")
    @PostMapping("/kill-switch")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Map<String, Object>> toggleMasterKillSwitch(@RequestParam boolean enabled) {
        log.warn("MASTER KILL SWITCH toggled to: {} by admin", enabled);

        featureFlagConfig.setMasterKillSwitch(enabled);

        Map<String, Object> response = new HashMap<>();
        response.put("masterKillSwitch", enabled);
        response.put("timestamp", LocalDateTime.now());
        response.put("message", enabled ?
            "⚠️ MASTER KILL SWITCH ACTIVATED - All write operations disabled" :
            "✅ Master kill switch deactivated - Service resumed");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Toggle a specific feature")
    @PostMapping("/features/{featureName}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Map<String, Object>> toggleFeature(
            @PathVariable String featureName,
            @RequestParam boolean enabled) {

        log.info("Feature '{}' toggled to: {}", featureName, enabled);

        try {
            var field = FeatureFlagConfig.FeatureToggles.class.getDeclaredField(featureName);
            field.setAccessible(true);
            field.set(featureFlagConfig.getFeatures(), enabled);

            Map<String, Object> response = new HashMap<>();
            response.put("feature", featureName);
            response.put("enabled", enabled);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (NoSuchFieldException e) {
            log.error("Unknown feature: {}", featureName);
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown feature: " + featureName));
        } catch (Exception e) {
            log.error("Error toggling feature {}: {}", featureName, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Configure dark launch settings")
    @PutMapping("/dark-launch")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Map<String, Object>> configureDarkLaunch(
            @RequestBody DarkLaunchConfig config) {

        log.info("Configuring dark launch: enabled={}, traffic={}%",
                config.isEnabled(), config.getTrafficPercentage());

        FeatureFlagConfig.DarkLaunch darkLaunch = featureFlagConfig.getDarkLaunch();
        darkLaunch.setEnabled(config.isEnabled());
        darkLaunch.setTrafficPercentage(config.getTrafficPercentage());

        if (config.getAllowedUsers() != null) {
            darkLaunch.setAllowedUsers(config.getAllowedUsers());
        }
        if (config.getAllowedRoles() != null) {
            darkLaunch.setAllowedRoles(config.getAllowedRoles());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("darkLaunch", darkLaunch);
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Dark launch configuration updated");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Health check with kill switch status")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", featureFlagConfig.isMasterKillSwitch() ? "DEGRADED" : "UP");
        health.put("masterKillSwitch", featureFlagConfig.isMasterKillSwitch());
        health.put("darkLaunchEnabled", featureFlagConfig.getDarkLaunch().isEnabled());
        health.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(health);
    }

    @Data
    public static class FeatureFlagStatus {
        private boolean masterKillSwitch;
        private FeatureFlagConfig.FeatureToggles features;
        private FeatureFlagConfig.DarkLaunch darkLaunch;
        private LocalDateTime timestamp;
    }

    @Data
    public static class DarkLaunchConfig {
        private boolean enabled;
        private int trafficPercentage;
        private String[] allowedUsers;
        private String[] allowedRoles;
    }
}
