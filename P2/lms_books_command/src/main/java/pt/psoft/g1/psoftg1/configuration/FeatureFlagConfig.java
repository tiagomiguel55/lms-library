package pt.psoft.g1.psoftg1.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Feature Flag Configuration for Dark Launch and Kill Switch strategies
 *
 * This configuration enables toggling features on/off without redeployment
 * - Dark Launch: Deploy features hidden from users, gradually enable
 * - Kill Switch: Emergency disable of features if problems detected
 *
 * @RefreshScope allows runtime configuration updates via POST /actuator/refresh
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "feature-flags")
@RefreshScope
@Component
public class FeatureFlagConfig {

    /**
     * Master kill switch - disables all write operations if true
     * Use this in emergency situations to make the service read-only
     */
    private boolean masterKillSwitch = false;

    /**
     * Release strategy to use per feature
     * Options: SIMPLE, DARK_LAUNCH, AB_TESTING
     */
    private Map<String, String> releaseStrategy = new HashMap<>();

    /**
     * Enable/disable specific features
     */
    private FeatureToggles features = new FeatureToggles();

    /**
     * Dark launch configuration
     */
    private DarkLaunch darkLaunch = new DarkLaunch();

    @Data
    public static class FeatureToggles {
        // Book management features
        private boolean bookCreation = true;
        private boolean bookUpdate = true;
        private boolean bookDeletion = true;
        private boolean bookPhotoUpload = true;

        // Author management features
        private boolean authorCreation = true;
        private boolean authorUpdate = true;

        // Genre management features
        private boolean genreCreation = true;

        // Advanced features
        private boolean batchOperations = true;
        private boolean externalApiIntegration = true;

        // New experimental features (Dark Launch)
        private boolean experimentalBookRecommendations = false;
        private boolean experimentalBulkImport = false;
    }

    @Data
    public static class DarkLaunch {
        // Enable dark launch mode
        private boolean enabled = false;

        // Percentage of requests to route to new features (0-100)
        private int trafficPercentage = 0;

        // Specific users/roles allowed to access dark launch features
        private String[] allowedUsers = new String[0];
        private String[] allowedRoles = new String[0];
    }

    /**
     * Check if a feature is enabled
     */
    public boolean isFeatureEnabled(String featureName) {
        if (masterKillSwitch) {
            return false;
        }

        try {
            var field = FeatureToggles.class.getDeclaredField(featureName);
            field.setAccessible(true);
            return (boolean) field.get(features);
        } catch (Exception e) {
            // Default to disabled if feature not found
            return false;
        }
    }

    /**
     * Check if dark launch is enabled for a specific feature
     */
    public boolean isDarkLaunchEnabled(String featureName) {
        return darkLaunch.enabled && isFeatureEnabled(featureName);
    }

    /**
     * Check if user is allowed to access dark launch features
     */
    public boolean isUserAllowedForDarkLaunch(String username, String role) {
        if (!darkLaunch.enabled) {
            return false;
        }

        // Check if user is in allowed list
        for (String allowedUser : darkLaunch.allowedUsers) {
            if (allowedUser.equals(username)) {
                return true;
            }
        }

        // Check if role is in allowed list
        for (String allowedRole : darkLaunch.allowedRoles) {
            if (allowedRole.equals(role)) {
                return true;
            }
        }

        return false;
    }
}