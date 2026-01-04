package pt.psoft.g1.psoftg1.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Service for managing feature flags and dark launch logic
 *
 * @RefreshScope enables runtime configuration updates without restart
 */
@Service
@RequiredArgsConstructor
@RefreshScope
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagConfig featureFlagConfig;
    private final ABTestingService abTestingService;
    private final Random random = new Random();

    /**
     * Check if a feature is enabled for the current user
     * Supports multiple release strategies:
     * - SIMPLE: Just on/off toggle
     * - DARK_LAUNCH: Whitelist + traffic percentage
     * - AB_TESTING: A/B experiments with metrics
     */
    public boolean isFeatureEnabled(String featureName) {
        // Check master kill switch first
        if (featureFlagConfig.isMasterKillSwitch()) {
            log.warn("Master kill switch is ACTIVE - feature {} is disabled", featureName);
            return false;
        }

        boolean enabled = featureFlagConfig.isFeatureEnabled(featureName);

        if (!enabled) {
            return false;
        }

        // Get the configured release strategy for this feature
        String strategy = featureFlagConfig.getReleaseStrategy().getOrDefault(featureName, "SIMPLE");

        log.debug("Feature {} using release strategy: {}", featureName, strategy);

        switch (strategy.toUpperCase()) {
            case "AB_TESTING":
                return checkABTesting(featureName);

            case "DARK_LAUNCH":
                return checkDarkLaunch(featureName);

            case "SIMPLE":
            default:
                // Simple toggle - feature is enabled globally
                return enabled;
        }
    }

    /**
     * Check A/B Testing strategy
     */
    private boolean checkABTesting(String featureName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false; // Unauthenticated users don't participate in A/B tests
        }

        String userId = auth.getName();
        boolean hasActiveExperiment = abTestingService.getActiveExperiments().stream()
                .anyMatch(e -> e.getFeatureName().equals(featureName));

        if (hasActiveExperiment) {
            return abTestingService.isFeatureEnabledForUser(featureName, userId, featureName);
        }

        log.warn("Feature {} configured for AB_TESTING but no active experiment found", featureName);
        return false; // No experiment = feature disabled
    }

    /**
     * Check Dark Launch strategy
     */
    private boolean checkDarkLaunch(String featureName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check whitelist (users/roles)
        if (featureFlagConfig.getDarkLaunch().isEnabled() && auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            String role = auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()
                    ? auth.getAuthorities().iterator().next().getAuthority()
                    : "";

            if (featureFlagConfig.isUserAllowedForDarkLaunch(username, role)) {
                log.info("Dark launch access granted for user {} to feature {}", username, featureName);
                return true;
            }
        }

        // Check traffic percentage (canary release)
        int trafficPercentage = featureFlagConfig.getDarkLaunch().getTrafficPercentage();
        if (trafficPercentage > 0) {
            boolean allowed = random.nextInt(100) < trafficPercentage;
            if (allowed) {
                log.debug("Dark launch access granted via traffic percentage ({}) for feature {}",
                         trafficPercentage, featureName);
            }
            return allowed;
        }

        return false; // Dark launch not enabled or no whitelist/traffic
    }

    /**
     * Require a feature to be enabled, throw exception if not
     */
    public void requireFeature(String featureName) throws FeatureDisabledException {
        if (!isFeatureEnabled(featureName)) {
            log.warn("Feature {} is currently disabled", featureName);
            throw new FeatureDisabledException("Feature '" + featureName + "' is currently disabled");
        }
    }

    /**
     * Check master kill switch status
     */
    public boolean isMasterKillSwitchActive() {
        return featureFlagConfig.isMasterKillSwitch();
    }

    /**
     * Get current feature flag configuration (for monitoring)
     */
    public FeatureFlagConfig getConfiguration() {
        return featureFlagConfig;
    }
}
