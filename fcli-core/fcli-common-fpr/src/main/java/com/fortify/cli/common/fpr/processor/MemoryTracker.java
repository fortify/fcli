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
package com.fortify.cli.common.fpr.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.fpr.model.FVDLMetadata;

/**
 * Logs JVM heap use and parser data structure sizes per parsing phase at TRACE level.
 * Never triggers garbage collection; values are point-in-time samples, not exact peaks.
 */
final class MemoryTracker {
    private static final Logger logger = LoggerFactory.getLogger(MemoryTracker.class);
    private static final long MB = 1024L * 1024L;

    private long sampledPeakBytes;

    void sample() {
        if (logger.isTraceEnabled()) {
            sampledPeakBytes = Math.max(sampledPeakBytes, usedBytes());
        }
    }

    void logPhase(String phase, FVDLMetadata fvdlMetadata, int vulnerabilityCount) {
        if (!logger.isTraceEnabled()) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long used = usedBytes();
        long peak = Math.max(sampledPeakBytes, used);
        logger.trace("Memory after {}: used={} MB, sampledPeak={} MB, committed={} MB, max={} MB; "
                + "nodePool={}, tracePool={}, descriptions={}, rules={}, vulnerabilities={}",
            phase, used / MB, peak / MB, runtime.totalMemory() / MB, runtime.maxMemory() / MB,
            fvdlMetadata.getNodePool().size(), fvdlMetadata.getTracePool().size(),
            fvdlMetadata.getDescriptionCache().size(), fvdlMetadata.getRuleMetadata().size(),
            vulnerabilityCount);
        sampledPeakBytes = 0;
    }

    private static long usedBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
