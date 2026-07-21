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

import org.slf4j.Logger;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliTechnicalException;

/**
 * Shared apply-remediations logic for cache zip entries (SSC and FoD).
 * SHA-256 is verified by {@link RemediationsCacheReader#getOrderedFprPaths()} (no extra local FPR pass).
 */
public final class RemediationsCacheApplyHelper {
    private RemediationsCacheApplyHelper() {}

    public enum EntryIdKind {
        ARTIFACT_ID,
        RELEASE_ID
    }

    public record ApplyResult(
            List<String> processedEntries,
            List<String> processedIds,
            int skipped,
            List<RemediationMetric> metrics) {}

    /**
     * Applies remediations for each ordered cache FPR until done or issue-id filter is exhausted.
     */
    public static ApplyResult applyEntries(
            RemediationsCacheReader cacheReader,
            String expectedProduct,
            String sourceCodeDirectory,
            IAviatorLogger logger,
            Set<String> issueIdFilter,
            EntryIdKind idKind,
            Logger skipLog) {
        cacheReader.requireProduct(expectedProduct);

        List<Path> fprPaths = cacheReader.getOrderedFprPaths();
        List<String> entryPaths = cacheReader.getOrderedEntryPaths();
        List<String> entryIds = idKind == EntryIdKind.ARTIFACT_ID
                ? cacheReader.getOrderedArtifactIds()
                : cacheReader.getOrderedReleaseIds();

        List<RemediationMetric> metrics = new ArrayList<>();
        List<String> processedEntries = new ArrayList<>();
        List<String> processedIds = new ArrayList<>();
        int skipped = 0;
        Set<String> remaining = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);

        for (int i = 0; i < fprPaths.size(); i++) {
            if (remaining != null && remaining.isEmpty()) {
                break;
            }
            String entryLabel = i < entryPaths.size() ? entryPaths.get(i) : fprPaths.get(i).getFileName().toString();
            RemediationMetric metric = applyOneEntry(
                    fprPaths.get(i), entryLabel, i + 1, fprPaths.size(), sourceCodeDirectory, logger, remaining, skipLog);
            if (metric == null) {
                skipped++;
            } else {
                metrics.add(metric);
                processedEntries.add(entryLabel);
                processedIds.add(i < entryIds.size() ? entryIds.get(i) : "");
                remaining = AviatorRemediationMetricsHelper.getRemainingIssueIds(remaining, metric);
            }
        }
        return new ApplyResult(
                List.copyOf(processedEntries), List.copyOf(processedIds), skipped, List.copyOf(metrics));
    }

    public static String actionLabel(RemediationMetric metric) {
        return metric != null && metric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
    }

    private static RemediationMetric applyOneEntry(
            Path fprPath,
            String entryLabel,
            int index,
            int total,
            String sourceCodeDirectory,
            IAviatorLogger logger,
            Set<String> issueFilter,
            Logger skipLog) {
        logger.progress("Processing FPR " + index + "/" + total + " (" + entryLabel + ")");
        logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            return ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger, issueFilter);
        } catch (AviatorSimpleException e) {
            skipLog.warn("Skipping cache entry {} as {}", entryLabel, e.getMessage());
            return null;
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to close FPR handle for cache entry " + entryLabel, e);
        }
    }
}
