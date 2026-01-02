package pt.psoft.g1.psoftg1.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Service for managing feature flags and dark launch logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagConfig featureFlagConfig;
    private final Random random = new Random();

    /**
     * Check if a feature is enabled for the current user
     */
    public boolean isFeatureEnabled(String featureName) {
        // Check master kill switch first
        if (featureFlagConfig.isMasterKillSwitch()) {
            log.warn("Master kill switch is ACTIVE - feature {} is disabled", featureName);
            return false;
        }

        boolean enabled = featureFlagConfig.isFeatureEnabled(featureName);

        // Check dark launch mode
        if (enabled && featureFlagConfig.getDarkLaunch().isEnabled()) {
            return isDarkLaunchAllowed(featureName);
        }

        return enabled;
    }

    /**
     * Check if dark launch allows access to this feature
     */
    private boolean isDarkLaunchAllowed(String featureName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            String role = auth.getAuthorities().isEmpty() ? "" :
                         auth.getAuthorities().iterator().next().getAuthority();

            // Check if user is in whitelist
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

        return false;
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

