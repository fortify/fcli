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
package com.fortify.cli.aviator._common.remediations_cache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliTechnicalException;

import lombok.extern.slf4j.Slf4j;

/**
 * Single apply-remediations loop for any {@link IRemediationsFprSource}
 * (cache zip entries or online downloads). Soft-skips on {@link AviatorSimpleException}.
 */
@Slf4j
public final class RemediationsApplyHelper {
    private RemediationsApplyHelper() {}

    public record ApplyResult(
            List<String> processedEntries,
            List<String> processedIds,
            int skipped,
            List<RemediationMetric> metrics) {}

    /**
     * Applies or previews remediations for each FPR source entry until done or the issue-id filter is exhausted.
     * Caller owns {@code fprSource} lifecycle (try-with-resources).
     */
    public static ApplyResult apply(
            IRemediationsFprSource fprSource,
            IApplyRemediationsOptions options,
            Set<String> issueIdFilter,
            IAviatorLogger logger) {
        Accumulator acc = new Accumulator(issueIdFilter);
        fprSource.forEachEntry((fprPath, label, id, index, total) -> {
            if (acc.remaining != null && acc.remaining.isEmpty()) {
                return false;
            }
            RemediationMetric metric = applyOne(
                    fprPath, label, index, total,
                    options.getSourceCodeDirectory(), logger, acc.remaining,
                    options.isPreviewMode());
            if (metric == null) {
                acc.skipped++;
            } else {
                acc.metrics.add(metric);
                acc.processedEntries.add(label);
                acc.processedIds.add(id != null ? id : "");
                acc.remaining = AviatorRemediationMetricsHelper.getRemainingIssueIds(acc.remaining, metric);
            }
            return true;
        });
        return acc.toResult();
    }

    /** @see AviatorRemediationMetricsHelper#actionLabel(RemediationMetric) */
    public static String actionLabel(RemediationMetric metric) {
        return AviatorRemediationMetricsHelper.actionLabel(metric);
    }

    private static RemediationMetric applyOne(
            Path fprPath,
            String entryLabel,
            int index,
            int total,
            String sourceCodeDirectory,
            IAviatorLogger logger,
            Set<String> issueFilter,
            boolean previewMode) {
        logger.progress("Processing FPR " + index + "/" + total + " (" + entryLabel + ")");
        logger.progress("Status: Processing FPR with Aviator for " + (previewMode ? "Previewing" : "Applying") + " Auto Remediations");
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            return ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger, issueFilter, previewMode);
        } catch (AviatorSimpleException e) {
            log.warn("Skipping entry {} as {}", entryLabel, e.getMessage());
            return null;
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to close FPR handle for entry " + entryLabel, e);
        }
    }

    /** Sequential-loop state (not concurrent — avoids Atomic* only to satisfy lambda capture rules). */
    private static final class Accumulator {
        private final List<RemediationMetric> metrics = new ArrayList<>();
        private final List<String> processedEntries = new ArrayList<>();
        private final List<String> processedIds = new ArrayList<>();
        private int skipped;
        private Set<String> remaining;

        private Accumulator(Set<String> issueIdFilter) {
            this.remaining = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);
        }

        private ApplyResult toResult() {
            return new ApplyResult(
                    List.copyOf(processedEntries),
                    List.copyOf(processedIds),
                    skipped,
                    List.copyOf(metrics));
        }
    }
}
