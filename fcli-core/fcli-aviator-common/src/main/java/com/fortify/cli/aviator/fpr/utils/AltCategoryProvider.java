package com.fortify.cli.aviator.fpr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider for alternative category mappings and fallback meta values (accuracy, impact, probability, remediation).
 * Uses predefined mappings for analyzer.category combinations. Can be extended with properties or DB if needed.
 */
public class AltCategoryProvider {
    private static final Logger logger = LoggerFactory.getLogger(AltCategoryProvider.class);

    private final Map<String, String> accuracyMap = new HashMap<>();
    private final Map<String, String> impactMap = new HashMap<>();
    private final Map<String, String> probabilityMap = new HashMap<>();
    private final Map<String, String> remediationMap = new HashMap<>();
    private final Map<String, String> defenderCoveredMap = new HashMap<>();

    /**
     * Constructor initializes default fallback mappings.
     * Add more based on your FVDL patterns or library equivalents.
     */
    public AltCategoryProvider() {
    }

    /**
     * Gets accuracy fallback for analyzer.category.
     *
     * @param analyzerDotCategory Analyzer.category string
     * @return Accuracy string or null if not found
     */
    public String getAccuracy(String analyzerDotCategory) {
        return accuracyMap.get(analyzerDotCategory);
    }

    /**
     * Gets impact fallback for analyzer.category.
     *
     * @param analyzerDotCategory Analyzer.category string
     * @return Impact string or null if not found
     */
    public String getImpact(String analyzerDotCategory) {
        return impactMap.get(analyzerDotCategory);
    }

    /**
     * Gets probability fallback for analyzer.category.
     *
     * @param analyzerDotCategory Analyzer.category string
     * @return Probability string or null if not found
     */
    public String getProbability(String analyzerDotCategory) {
        return probabilityMap.get(analyzerDotCategory);
    }

    /**
     * Gets remediation constant fallback for analyzer.category.
     *
     * @param analyzerDotCategory Analyzer.category string
     * @return Remediation string or null if not found
     */
    public String getRemediationConstant(String analyzerDotCategory) {
        return remediationMap.get(analyzerDotCategory);
    }

    /**
     * Gets defender covered fallback for category.
     *
     * @param category Category string
     * @return Defender string or "None" if not found
     */
    public String getLegacyMappingValue(String category, String coveredDefenderKey) {
        // Assuming coveredDefenderKey is like "COVERED_DEFENDER"
        return defenderCoveredMap.getOrDefault(category, "None");
    }

    // Add methods to load from properties/DB if needed in future
}