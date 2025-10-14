package com.fortify.cli.aviator.fpr.processor;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.utils.AltCategoryProvider;
import com.fortify.cli.aviator.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Finalizes a Vulnerability by applying fallbacks and deriving fields (e.g., likelihood, priority)
 * using AbstractIssueParseHandler logic from the closed-source library.
 */
public class VulnFinalizer {
    private static final Logger logger = LoggerFactory.getLogger(VulnFinalizer.class);
    private final AltCategoryProvider altCategoryProvider;

    /**
     * Constructor initializing the AltCategoryProvider for fallbacks.
     */
    public VulnFinalizer() {
        this.altCategoryProvider = new AltCategoryProvider();
    }

    /**
     * Finalizes a Vulnerability by applying fallbacks and derived fields.
     *
     * @param vuln Vulnerability to finalize
     */
    public void finalize(Vulnerability vuln) {
        if (vuln == null) {
            logger.warn("Cannot finalize null Vulnerability");
            return;
        }

        // Apply fallbacks for missing meta fields
        applyFallbacks(vuln);
        // Derive additional fields (likelihood, priority, package name)
        deriveFields(vuln);
    }

    /**
     * Applies fallbacks for accuracy, impact, and probability using AltCategoryProvider.
     *
     * @param vuln Vulnerability to update
     */
    private void applyFallbacks(Vulnerability vuln) {
        String analyzerDotCategory = getAnalyzerDotCategory(vuln.getAnalyzerName(), vuln.getCategory());
        if (vuln.getAccuracy() == null || vuln.getAccuracy() == 0.0) {
            String fallbackAccuracy = altCategoryProvider.getAccuracy(analyzerDotCategory);
            vuln.setAccuracy(fallbackAccuracy != null ? Double.parseDouble(fallbackAccuracy) : 0.0);
        }
        if (vuln.getImpact() == null || vuln.getImpact() == 0.0) {
            String fallbackImpact = altCategoryProvider.getImpact(analyzerDotCategory);
            vuln.setImpact(fallbackImpact != null ? Double.parseDouble(fallbackImpact) : 0.0);
        }
        if (vuln.getProbability() == null || vuln.getProbability() == 0.0) {
            String fallbackProbability = altCategoryProvider.getProbability(analyzerDotCategory);
            vuln.setProbability(fallbackProbability != null ? Double.parseDouble(fallbackProbability) : 0.0);
        }
    }

    /**
     * Derives final fields like category, likelihood, and priority after all other data has been populated.
     *
     * @param vuln Vulnerability to update
     */
    /**
     * Derives final fields like category, likelihood, and priority using the
     * correct, library-accurate formulas. This method is called at the end of
     * vulnerability processing to ensure it uses final data.
     *
     * @param vuln Vulnerability to update
     */
    private void deriveFields(Vulnerability vuln) {
        // 1. Derive Category (Type + SubType)
        String category = vuln.getType() + (StringUtil.isEmpty(vuln.getSubType()) ? "" : ": " + vuln.getSubType());
        vuln.setCategory(category);
        vuln.setSubcategory(vuln.getSubType());

        // 2. Derive Likelihood using the CORRECT formula.
        // We use the final values on the vuln object, which may have been populated by fallbacks.
        double accuracy = (vuln.getAccuracy() != null) ? vuln.getAccuracy() : 0.0;
        // Confidence is an int (1-5), so we cast it to double for the calculation.
        double confidence = vuln.getConfidence();
        double probability = (vuln.getProbability() != null) ? vuln.getProbability() : 0.0;

        // Correct formula: (accuracy * confidence * probability) / 25.0
        double likelihoodValue = (accuracy * confidence * probability) / 25.0;
        vuln.setLikelihood(String.format("%.5f", likelihoodValue));

        double impact = (vuln.getImpact() != null) ? vuln.getImpact() : 0.0;
        String priority;
        if (impact >= 2.5) {
            priority = (likelihoodValue >= 2.5) ? "Critical" : "High";
        } else {
            priority = (likelihoodValue >= 2.5) ? "Medium" : "Low";
        }
        vuln.setPriority(priority);

        // 4. Derive package name (logic remains the same)
        if (vuln.getProjectName() == null || vuln.getProjectName().isEmpty()) {
            if (vuln.getFiles() != null && !vuln.getFiles().isEmpty()) {
                String firstFilePath = vuln.getFiles().get(0).getName();
                vuln.setProjectName(initPackageName(firstFilePath));
            }
        }
    }

    /**
     * Initializes package name from a string (e.g., filename or project name).
     * Mimics AbstractIssueParseHandler's initPackageName.
     *
     * @param input String to derive package from
     * @return Derived package name or empty if invalid
     */
    private String initPackageName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        int separatorIndex = input.indexOf('/');
        // If separatorIndex is -1 (not found), it returns the original 'input' string. This is safe.
        return separatorIndex > 0 ? input.substring(0, separatorIndex) : input;
    }

    /**
     * Constructs analyzer.category string for fallbacks.
     *
     * @param analyzer Analyzer name
     * @param category Category
     * @return Combined string or empty if either is null
     */
    private String getAnalyzerDotCategory(String analyzer, String category) {
        if (analyzer == null || category == null) {
            return "";
        }
        return analyzer + "." + category;
    }
}