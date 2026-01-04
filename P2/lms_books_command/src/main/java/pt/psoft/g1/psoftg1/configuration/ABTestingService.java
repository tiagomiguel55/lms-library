package pt.psoft.g1.psoftg1.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A/B Testing Service using Feature Flags
 *
 * This service enables A/B testing without deploying multiple versions:
 * - Assigns users to variants (A or B) based on user ID
 * - Tracks metrics for each variant
 * - Allows dynamic adjustment of traffic distribution
 * - Provides experiment results and statistics
 */
@Slf4j
@Service
public class ABTestingService {

    private final FeatureFlagConfig featureFlagConfig;

    // Store active experiments
    private final Map<String, Experiment> experiments = new ConcurrentHashMap<>();

    // Store user variant assignments (userId -> variant)
    private final Map<String, Map<String, String>> userAssignments = new ConcurrentHashMap<>();

    // Store metrics for each variant
    private final Map<String, ExperimentMetrics> metrics = new ConcurrentHashMap<>();

    public ABTestingService(FeatureFlagConfig featureFlagConfig) {
        this.featureFlagConfig = featureFlagConfig;
    }

    /**
     * Create a new A/B test experiment
     */
    public Experiment createExperiment(String featureName, int trafficPercentToB) {
        String experimentId = featureName; // Use featureName as experimentId for consistency

        Experiment experiment = new Experiment();
        experiment.setExperimentId(experimentId);
        experiment.setFeatureName(featureName);
        experiment.setTrafficPercentToB(trafficPercentToB);
        experiment.setStartTime(System.currentTimeMillis());
        experiment.setActive(true);

        experiments.put(experimentId, experiment);
        userAssignments.put(experimentId, new ConcurrentHashMap<>());

        // Initialize metrics for both variants
        metrics.put(experimentId + "_A", new ExperimentMetrics("A"));
        metrics.put(experimentId + "_B", new ExperimentMetrics("B"));

        log.info("Created A/B test experiment: {} for feature: {} with {}% traffic to B",
                experimentId, featureName, trafficPercentToB);

        return experiment;
    }

    /**
     * Assign user to a variant (A or B)
     * Uses consistent hashing to ensure same user always gets same variant
     */
    public String assignVariant(String experimentId, String userId) {
        Experiment experiment = experiments.get(experimentId);
        if (experiment == null || !experiment.isActive()) {
            return "A"; // Default to control group
        }

        // Check if user already has an assignment
        Map<String, String> expAssignments = userAssignments.get(experimentId);
        if (expAssignments.containsKey(userId)) {
            return expAssignments.get(userId);
        }

        // Assign based on hash of userId
        String variant = determineVariantByHash(userId, experiment.getTrafficPercentToB());
        expAssignments.put(userId, variant);

        log.debug("Assigned user {} to variant {} for experiment {}", userId, variant, experimentId);

        return variant;
    }

    /**
     * Check if feature should be enabled for user based on A/B test
     */
    public boolean isFeatureEnabledForUser(String experimentId, String userId, String featureName) {
        String variant = assignVariant(experimentId, userId);

        // First check if the feature is globally enabled via feature flags
        if (!featureFlagConfig.isFeatureEnabled(featureName)) {
            return false;
        }

        // Variant A (control) = feature disabled, Variant B = feature enabled
        boolean enabled = variant.equals("B");

        log.debug("Feature {} is {} for user {} (variant {})",
                featureName, enabled ? "enabled" : "disabled", userId, variant);

        return enabled;
    }

    /**
     * Record a metric event for a variant
     */
    public void recordMetric(String experimentId, String userId, String metricName, double value) {
        String variant = assignVariant(experimentId, userId);
        String metricsKey = experimentId + "_" + variant;

        ExperimentMetrics variantMetrics = metrics.get(metricsKey);
        if (variantMetrics != null) {
            variantMetrics.recordMetric(metricName, value);
        }
    }

    /**
     * Record a success event (e.g., conversion, completion)
     */
    public void recordSuccess(String experimentId, String userId) {
        recordMetric(experimentId, userId, "success", 1.0);
    }

    /**
     * Record a failure event (e.g., error, abandonment)
     */
    public void recordFailure(String experimentId, String userId) {
        recordMetric(experimentId, userId, "failure", 1.0);
    }

    /**
     * Get experiment results comparing variants A and B
     */
    public ExperimentResults getExperimentResults(String experimentId) {
        ExperimentMetrics metricsA = metrics.get(experimentId + "_A");
        ExperimentMetrics metricsB = metrics.get(experimentId + "_B");
        Experiment experiment = experiments.get(experimentId);

        if (metricsA == null || metricsB == null || experiment == null) {
            return null;
        }

        ExperimentResults results = new ExperimentResults();
        results.setExperimentId(experimentId);
        results.setFeatureName(experiment.getFeatureName());
        results.setVariantAMetrics(metricsA);
        results.setVariantBMetrics(metricsB);
        results.setTotalUsers(userAssignments.get(experimentId).size());
        results.setTrafficPercentToB(experiment.getTrafficPercentToB());

        // Calculate improvement
        double successRateA = metricsA.getSuccessRate();
        double successRateB = metricsB.getSuccessRate();
        double improvement = ((successRateB - successRateA) / successRateA) * 100;
        results.setImprovementPercent(improvement);

        // Determine winner
        if (metricsA.getTotalEvents() > 100 && metricsB.getTotalEvents() > 100) {
            if (improvement > 5) {
                results.setWinner("B");
                results.setStatisticallySignificant(true);
            } else if (improvement < -5) {
                results.setWinner("A");
                results.setStatisticallySignificant(true);
            } else {
                results.setWinner("INCONCLUSIVE");
                results.setStatisticallySignificant(false);
            }
        } else {
            results.setWinner("INSUFFICIENT_DATA");
            results.setStatisticallySignificant(false);
        }

        return results;
    }

    /**
     * Update traffic distribution for an experiment
     */
    public void updateTrafficDistribution(String experimentId, int newPercentToB) {
        Experiment experiment = experiments.get(experimentId);
        if (experiment != null) {
            experiment.setTrafficPercentToB(newPercentToB);
            log.info("Updated traffic distribution for experiment {}: {}% to B", experimentId, newPercentToB);
        }
    }

    /**
     * Stop an experiment
     */
    public void stopExperiment(String experimentId) {
        Experiment experiment = experiments.get(experimentId);
        if (experiment != null) {
            experiment.setActive(false);
            experiment.setEndTime(System.currentTimeMillis());
            log.info("Stopped experiment: {}", experimentId);
        }
    }

    /**
     * Get all active experiments
     */
    public List<Experiment> getActiveExperiments() {
        return experiments.values().stream()
                .filter(Experiment::isActive)
                .toList();
    }

    /**
     * Determine variant based on consistent hash
     */
    private String determineVariantByHash(String userId, int percentToB) {
        // Use consistent hashing to ensure same user always gets same variant
        int hash = Math.abs(userId.hashCode());
        int bucket = hash % 100;

        return bucket < percentToB ? "B" : "A";
    }

    // ========== Data Classes ==========

    @Data
    public static class Experiment {
        private String experimentId;
        private String featureName;
        private int trafficPercentToB; // 0-100
        private boolean active;
        private long startTime;
        private long endTime;
    }

    @Data
    public static class ExperimentMetrics {
        private String variant;
        private long totalEvents = 0;
        private long successEvents = 0;
        private long failureEvents = 0;
        private Map<String, List<Double>> customMetrics = new ConcurrentHashMap<>();

        public ExperimentMetrics(String variant) {
            this.variant = variant;
        }

        public void recordMetric(String metricName, double value) {
            totalEvents++;

            if (metricName.equals("success")) {
                successEvents += (long) value;
            } else if (metricName.equals("failure")) {
                failureEvents += (long) value;
            }

            customMetrics.computeIfAbsent(metricName, k -> new ArrayList<>()).add(value);
        }

        public double getSuccessRate() {
            return totalEvents > 0 ? (double) successEvents / totalEvents * 100 : 0.0;
        }

        public double getFailureRate() {
            return totalEvents > 0 ? (double) failureEvents / totalEvents * 100 : 0.0;
        }

        public double getAverageMetric(String metricName) {
            List<Double> values = customMetrics.get(metricName);
            if (values == null || values.isEmpty()) {
                return 0.0;
            }
            return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
    }

    @Data
    public static class ExperimentResults {
        private String experimentId;
        private String featureName;
        private ExperimentMetrics variantAMetrics;
        private ExperimentMetrics variantBMetrics;
        private int totalUsers;
        private int trafficPercentToB;
        private double improvementPercent;
        private String winner;
        private boolean statisticallySignificant;
    }
}