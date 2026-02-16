/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.aviator.fpr.processor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.*;


/**
 * Tracks and logs memory consumption during FVDL parsing.
 * Provides detailed metrics on JVM memory usage and data structure sizes.
 */
public class MemoryTracker {
    private static final Logger logger = LoggerFactory.getLogger(MemoryTracker.class);

    private long peakMemoryBeforeParsing = 0;
    private long peakMemoryPass1 = 0;
    private long peakMemoryPass2 = 0;
    private long peakMemoryPostProcessing = 0;

    /**
     * Get current used memory without triggering GC.
     */
    public long getCurrentUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Initialize baseline memory before parsing.
     */
    public void initializeBaseline() {
        peakMemoryBeforeParsing = getCurrentUsedMemory();
    }

    /**
     * Update pass 1 peak memory.
     */
    public void updatePass1Peak() {
        peakMemoryPass1 = Math.max(peakMemoryPass1, getCurrentUsedMemory());
    }

    /**
     * Initialize pass 1 peak.
     */
    public void initializePass1Peak() {
        peakMemoryPass1 = getCurrentUsedMemory();
    }

    /**
     * Update pass 2 peak memory.
     */
    public void updatePass2Peak() {
        peakMemoryPass2 = Math.max(peakMemoryPass2, getCurrentUsedMemory());
    }

    /**
     * Initialize pass 2 peak.
     */
    public void initializePass2Peak() {
        peakMemoryPass2 = getCurrentUsedMemory();
    }

    /**
     * Update post-processing peak memory.
     */
    public void updatePostProcessingPeak() {
        peakMemoryPostProcessing = Math.max(peakMemoryPostProcessing, getCurrentUsedMemory());
    }

    /**
     * Initialize post-processing peak.
     */
    public void initializePostProcessingPeak() {
        peakMemoryPostProcessing = getCurrentUsedMemory();
    }

    public long getPeakMemoryPass1() {
        return peakMemoryPass1;
    }

    public long getPeakMemoryPass2() {
        return peakMemoryPass2;
    }

    public long getPeakMemoryPostProcessing() {
        return peakMemoryPostProcessing;
    }

    /**
     * Log memory consumption with detailed metrics.
     */
    public void logMemoryConsumption(String phase) {
        logMemoryConsumptionWithPeak(phase, 0, null, null, null);
    }

    /**
     * Log memory consumption with peak tracking and data structure breakdown.
     */
    public void logMemoryConsumptionWithPeak(String phase, long peakMemoryDuringPhase,
                                             FVDLMetadata fvdlMetadata,
                                             List<StreamedVulnerability> rawVulnerabilities,
                                             List<Vulnerability> vulnerabilities) {
        Runtime runtime = Runtime.getRuntime();

        // Force garbage collection for accurate readings
        runtime.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        double usedMemoryMB = usedMemory / (1024.0 * 1024.0);
        double totalMemoryMB = totalMemory / (1024.0 * 1024.0);
        double maxMemoryMB = maxMemory / (1024.0 * 1024.0);
        double freeMemoryMB = freeMemory / (1024.0 * 1024.0);
        double peakMemoryMB = peakMemoryDuringPhase / (1024.0 * 1024.0);

        double usedPercentage = (usedMemory * 100.0) / maxMemory;
        double peakPercentage = (peakMemoryDuringPhase * 100.0) / maxMemory;

        logger.info("=== Memory Consumption {} ===", phase);
        logger.info("    Used Memory:  {} MB", String.format("%.2f", usedMemoryMB));
        logger.info("    Free Memory:  {} MB", String.format("%.2f", freeMemoryMB));
        logger.info("    Total Memory: {} MB", String.format("%.2f", totalMemoryMB));
        logger.info("    Max Memory:   {} MB", String.format("%.2f", maxMemoryMB));
        logger.info("    Usage:        {}% of max memory", String.format("%.2f", usedPercentage));

        if (peakMemoryDuringPhase > 0) {
            logger.info("    Peak Memory:  {} MB ({}% of max)",
                String.format("%.2f", peakMemoryMB),
                String.format("%.2f", peakPercentage));
            double peakDeltaMB = (peakMemoryDuringPhase - usedMemory) / (1024.0 * 1024.0);
            logger.info("    Peak Delta:   {} MB (peak was {} higher than current)",
                String.format("%.2f", peakDeltaMB),
                peakDeltaMB > 0 ? String.format("%.2f MB", peakDeltaMB) : "not higher");
        }

        if (fvdlMetadata != null) {
            logDataStructureSizes(fvdlMetadata, rawVulnerabilities, vulnerabilities);
        }

        logger.info("=================================");
    }

    /**
     * Log the sizes of key data structures.
     */
    private void logDataStructureSizes(FVDLMetadata fvdlMetadata,
                                       List<StreamedVulnerability> rawVulnerabilities,
                                       List<Vulnerability> vulnerabilities) {
        logger.info("    --- Data Structure Sizes ---");

        int nodePoolSize = fvdlMetadata.getNodePool().size();
        long estimatedNodePoolMemory = estimateNodePoolMemory(nodePoolSize);
        logger.info("    NodePool:           {} nodes (~{} MB)",
            nodePoolSize,
            String.format("%.2f", estimatedNodePoolMemory / (1024.0 * 1024.0)));

        int tracePoolSize = fvdlMetadata.getTracePool().size();
        long estimatedTracePoolMemory = estimateTracePoolMemory(tracePoolSize);
        logger.info("    TracePool:          {} traces (~{} MB)",
            tracePoolSize,
            String.format("%.2f", estimatedTracePoolMemory / (1024.0 * 1024.0)));

        int descriptionCacheSize = fvdlMetadata.getDescriptionCache().size();
        long estimatedDescriptionMemory = estimateDescriptionCacheMemory(descriptionCacheSize);
        logger.info("    DescriptionCache:   {} entries (~{} MB)",
            descriptionCacheSize,
            String.format("%.2f", estimatedDescriptionMemory / (1024.0 * 1024.0)));

        int ruleMetadataSize = fvdlMetadata.getRuleMetadata().size();
        long estimatedRuleMetadataMemory = estimateRuleMetadataMemory(ruleMetadataSize);
        logger.info("    RuleMetadata:       {} rules (~{} MB)",
            ruleMetadataSize,
            String.format("%.2f", estimatedRuleMetadataMemory / (1024.0 * 1024.0)));

        if (rawVulnerabilities != null && !rawVulnerabilities.isEmpty()) {
            long estimatedVulnMemory = estimateVulnerabilitiesMemory(rawVulnerabilities.size());
            logger.info("    Raw Vulnerabilities: {} vulns (~{} MB)",
                rawVulnerabilities.size(),
                String.format("%.2f", estimatedVulnMemory / (1024.0 * 1024.0)));
        }

        if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
            long estimatedEnrichedVulnMemory = estimateEnrichedVulnerabilitiesMemory(vulnerabilities.size());
            logger.info("    Enriched Vulnerabilities: {} vulns (~{} MB)",
                vulnerabilities.size(),
                String.format("%.2f", estimatedEnrichedVulnMemory / (1024.0 * 1024.0)));
        }

        long totalEstimated = estimatedNodePoolMemory + estimatedTracePoolMemory +
            estimatedDescriptionMemory + estimatedRuleMetadataMemory;
        if (rawVulnerabilities != null && !rawVulnerabilities.isEmpty()) {
            totalEstimated += estimateVulnerabilitiesMemory(rawVulnerabilities.size());
        }
        if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
            totalEstimated += estimateEnrichedVulnerabilitiesMemory(vulnerabilities.size());
        }

        logger.info("    Total Estimated:    ~{} MB",
            String.format("%.2f", totalEstimated / (1024.0 * 1024.0)));
    }

    private long estimateNodePoolMemory(int nodeCount) {
        return (long) (nodeCount * 1.5 * 1024);
    }

    private long estimateTracePoolMemory(int traceCount) {
        return (long) (traceCount * 0.5 * 1024);
    }

    private long estimateDescriptionCacheMemory(int descCount) {
        return (long) (descCount * 3 * 1024);
    }

    private long estimateRuleMetadataMemory(int ruleCount) {
        return (long) (ruleCount * 0.5 * 1024);
    }

    private long estimateVulnerabilitiesMemory(int vulnCount) {
        return (long) (vulnCount * 3 * 1024);
    }

    private long estimateEnrichedVulnerabilitiesMemory(int vulnCount) {
        return (long) (vulnCount * 20 * 1024);
    }

    public long estimateDescriptionCacheMemory(FVDLMetadata fvdlMetadata) {
        return estimateDescriptionCacheMemory(fvdlMetadata.getDescriptionCache().size());
    }

    public long estimateRuleMetadataMemory(FVDLMetadata fvdlMetadata) {
        return estimateRuleMetadataMemory(fvdlMetadata.getRuleMetadata().size());
    }

    public long estimateVulnerabilitiesMemory(List<StreamedVulnerability> rawVulnerabilities) {
        return estimateVulnerabilitiesMemory(rawVulnerabilities.size());
    }

    public long estimateNodePoolMemory(FVDLMetadata fvdlMetadata) {
        return estimateNodePoolMemory(fvdlMetadata.getNodePool().size());
    }

    public long estimateTracePoolMemory(FVDLMetadata fvdlMetadata) {
        return estimateTracePoolMemory(fvdlMetadata.getTracePool().size());
    }
}
