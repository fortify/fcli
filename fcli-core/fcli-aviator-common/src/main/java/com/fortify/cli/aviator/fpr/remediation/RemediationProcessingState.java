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
package com.fortify.cli.aviator.fpr.remediation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.fpr.remediation.preview.FilePreview;
import com.fortify.cli.aviator.fpr.remediation.preview.PreviewDetail;

final class RemediationProcessingState {
    private static final String POSSIBLY_REMEDIATED_REASON = "Possibly remediated by a sibling fix";
    private static final String REQUESTED_ISSUE_NOT_FOUND = "Requested issue not found in remediations";

    private final RemediationProcessingOptions options;
    private final Set<String> seenIssueIds = new LinkedHashSet<>();
    private final Set<String> satisfiedIssueIds = new LinkedHashSet<>();
    private final Set<String> appliedIssueIds = new LinkedHashSet<>();
    private final Set<String> identicalIssueIds = new LinkedHashSet<>();
    private final Set<String> supersededIssueIds = new LinkedHashSet<>();
    private final Set<String> possiblyRemediatedIssueIds = new LinkedHashSet<>();
    private final Map<String, List<String>> coveredByPriorFixIssues = new LinkedHashMap<>();
    private final Map<String, List<String>> supersededByPriorFixIssues = new LinkedHashMap<>();
    private final Set<String> dependentIssueIds = new LinkedHashSet<>();
    private final Set<String> declinedFixIds = new LinkedHashSet<>();
    private final Set<String> implicitIssueIds = new LinkedHashSet<>();
    private final Set<String> implicitlyAppliedIssueIds = new LinkedHashSet<>();
    private final Set<String> implicitlySatisfiedIssueIds = new LinkedHashSet<>();
    private final Set<String> modifiedFiles = new LinkedHashSet<>();
    private final Map<String, Integer> skippedByReason = new LinkedHashMap<>();
    private final Map<String, String> issueSkipReasons = new LinkedHashMap<>();
    private final Map<String, String> descriptionsByIssue = new LinkedHashMap<>();
    private final Map<String, PreviewDetail> previewDetailsByIssue = new LinkedHashMap<>();
    private int xmlEntryCount;
    private int appliedRemediations;
    private int identicalRemediations;
    private int supersededRemediations;
    private int possiblyRemediatedRemediations;

    RemediationProcessingState(RemediationProcessingOptions options) {
        this.options = options;
    }

    void setXmlEntryCount(int xmlEntryCount) {
        this.xmlEntryCount = xmlEntryCount;
    }

    boolean shouldProcess(String instanceId) {
        return !options.isFiltered() || options.issueIdFilter().contains(instanceId)
            || implicitIssueIds.contains(instanceId) || dependentIssueIds.contains(instanceId);
    }

    /** A fix that is requested, or approved to be applied implicitly, and so resolves the issues that reference it. */
    boolean isFixSelected(String instanceId) {
        return !options.isFiltered() || options.issueIdFilter().contains(instanceId)
            || implicitIssueIds.contains(instanceId);
    }

    /** Issues without a fix of their own that a selected fix resolves; they are processed and counted with it. */
    void addDependentIssueId(String instanceId) {
        if (options.isFiltered() && instanceId != null) {
            dependentIssueIds.add(instanceId);
        }
    }

    void recordDeclinedFix(String instanceId) {
        declinedFixIds.add(instanceId);
    }

    /**
     * Fixes that a requested issue is covered by must be applied even if the user did not request
     * them; they are tracked as implicit so they don't distort the requested-issue counts.
     */
    void addImplicitIssueIds(Collection<String> instanceIds) {
        if (!options.isFiltered()) {
            return;
        }
        for (String instanceId : instanceIds) {
            if (instanceId != null && !options.issueIdFilter().contains(instanceId)) {
                implicitIssueIds.add(instanceId);
            }
        }
    }

    private boolean isImplicit(String instanceId) {
        return instanceId != null && implicitIssueIds.contains(instanceId);
    }

    void recordSeen(String instanceId) {
        if (options.isFiltered() && instanceId != null) {
            seenIssueIds.add(instanceId);
        }
    }

    void recordDescription(String instanceId, String description) {
        if (instanceId != null && !instanceId.isBlank() && description != null) {
            descriptionsByIssue.put(instanceId, description);
        }
    }

    void recordApplied(String instanceId) {
        appliedRemediations++;
        recordSatisfied(instanceId);
        appliedIssueIds.add(instanceId);
        issueSkipReasons.remove(instanceId);
        markImplicitlySatisfied(instanceId);
    }

    private void markImplicitlySatisfied(String instanceId) {
        if (isImplicit(instanceId)) {
            implicitlyAppliedIssueIds.add(instanceId);
            implicitlySatisfiedIssueIds.add(instanceId);
        }
    }

    void recordPreviewAvailable(String instanceId, Map<String, FilePreview> files) {
        recordApplied(instanceId);
        for (FilePreview filePreview : files.values()) {
            modifiedFiles.add(filePreview.path());
        }
        previewDetailsByIssue.put(instanceId,
            PreviewDetail.available(instanceId, descriptionsByIssue.get(instanceId), files));
    }

    void recordIdentical(String instanceId) {
        identicalRemediations++;
        identicalIssueIds.add(instanceId);
        recordSatisfied(instanceId);
        markImplicitlySatisfied(instanceId);
    }

    void recordSuperseded(String instanceId) {
        supersededRemediations++;
        supersededIssueIds.add(instanceId);
        recordSatisfied(instanceId);
        markImplicitlySatisfied(instanceId);
    }

    void recordPossiblyRemediated(String instanceId) {
        if (isImplicit(instanceId)) {
            return;
        }
        possiblyRemediatedRemediations++;
        possiblyRemediatedIssueIds.add(instanceId);
        issueSkipReasons.put(instanceId, POSSIBLY_REMEDIATED_REASON);
    }

    void recordSkipped(String instanceId, SkipReason reason) {
        recordSkipped(instanceId, reason.displayName());
    }

    void recordSkipped(String instanceId, String reason) {
        if (isImplicit(instanceId)) {
            return;
        }
        skippedByReason.merge(reason, 1, Integer::sum);
        if (instanceId != null && !instanceId.isBlank()) {
            issueSkipReasons.put(instanceId, reason);
            if (options.isPreview()) {
                previewDetailsByIssue.put(instanceId,
                    PreviewDetail.skipped(instanceId, descriptionsByIssue.get(instanceId)));
            }
        }
    }

    /**
     * Resolves an issue that has no fix of its own because another issue's fix already covers it exactly. Must run
     * after all fixes were processed. Reported as covered if a covering fix was applied (or was itself already
     * satisfied); otherwise skipped, because nothing in the sources addresses this issue.
     */
    void recordCoveredByPriorFix(String instanceId, List<String> coveredByInstanceIds) {
        if (!anyFixSatisfied(coveredByInstanceIds)) {
            recordSkipped(instanceId, unresolvedReason(coveredByInstanceIds));
            return;
        }
        coveredByPriorFixIssues.put(instanceId, List.copyOf(coveredByInstanceIds));
        recordSatisfied(instanceId);
        if (options.isPreview()) {
            previewDetailsByIssue.put(instanceId,
                PreviewDetail.coveredByPriorFix(instanceId, descriptionsByIssue.get(instanceId)));
        }
    }

    /**
     * Resolves an issue whose own fix is contained in a wider fix (the issue has no fix of its own). Counted as
     * superseded, which is a kind of remediated, once the wider fix was applied; skipped otherwise.
     */
    void recordSupersededByPriorFix(String instanceId, List<String> supersededByInstanceIds) {
        if (!anyFixSatisfied(supersededByInstanceIds)) {
            recordSkipped(instanceId, unresolvedReason(supersededByInstanceIds));
            return;
        }
        supersededRemediations++;
        supersededIssueIds.add(instanceId);
        supersededByPriorFixIssues.put(instanceId, List.copyOf(supersededByInstanceIds));
        recordSatisfied(instanceId);
        if (options.isPreview()) {
            previewDetailsByIssue.put(instanceId,
                PreviewDetail.supersededByPriorFix(instanceId, descriptionsByIssue.get(instanceId)));
        }
    }

    private boolean anyFixSatisfied(List<String> fixIds) {
        return fixIds.stream().anyMatch(satisfiedIssueIds::contains);
    }

    private String unresolvedReason(List<String> fixIds) {
        String joined = String.join(", ", fixIds);
        return fixIds.stream().anyMatch(declinedFixIds::contains)
            ? "Fix " + joined + " was not confirmed; confirm it interactively or use --confirm"
            : "Prior fix " + joined + " was not applied";
    }

    Set<String> modifiedFiles() {
        return modifiedFiles;
    }

    Map<String, Integer> skippedByReason() {
        return skippedByReason;
    }

    RemediationMetric toMetric() {
        recordMissingRequestedIssues();
        // Every issue that was requested, resolved, or reported with a skip reason is part of the total, so the skip
        // details always add up (an issue that depends on a selected fix that failed has a skip reason but was not requested).
        Set<String> filteredTotal = new LinkedHashSet<>(options.issueIdFilter());
        filteredTotal.addAll(satisfiedIssueIds);
        filteredTotal.addAll(issueSkipReasons.keySet());
        int totalRemediations = options.isFiltered() ? filteredTotal.size() : xmlEntryCount;
        int applied = options.isFiltered() ? appliedIssueIds.size() : appliedRemediations;
        int skipped = options.isFiltered()
            ? totalRemediations - satisfiedIssueIds.size()
            : totalRemediations - appliedRemediations - identicalRemediations - supersededRemediations
                - possiblyRemediatedRemediations - coveredByPriorFixIssues.size();
        return RemediationMetric.builder()
                .totalRemediations(totalRemediations)
                .appliedRemediations(applied)
                .identicalRemediations(identicalRemediations)
                .supersededRemediations(supersededRemediations)
                .possiblyRemediatedRemediations(possiblyRemediatedRemediations)
                .coveredByPriorFixRemediations(coveredByPriorFixIssues.size())
                .supersedesPriorFixRemediations(supersededByPriorFixIssues.size())
                .remediatedRemediations(satisfiedIssueIds.size())
                .skippedRemediations(skipped)
                .modifiedFiles(modifiedFiles)
                .skippedByReason(skippedByReason)
                .executionMode(options.executionMode())
                .requestedIssueIds(options.issueIdFilter())
                .seenIssueIds(seenIssueIds)
                .satisfiedIssueIds(satisfiedIssueIds)
                .appliedIssueIds(appliedIssueIds)
                .identicalIssueIds(identicalIssueIds)
                .supersededIssueIds(supersededIssueIds)
                .possiblyRemediatedIssueIds(possiblyRemediatedIssueIds)
                .coveredByPriorFixIssues(coveredByPriorFixIssues)
                .supersededByPriorFixIssues(supersededByPriorFixIssues)
                .implicitlyAppliedIssueIds(implicitlyAppliedIssueIds)
                .issueSkipReasons(issueSkipReasons)
                .previewDetails(new ArrayList<>(previewDetailsByIssue.values()))
                .build();
    }

    private void recordMissingRequestedIssues() {
        if (!options.isFiltered()) {
            return;
        }
        for (String requestedIssueId : options.issueIdFilter()) {
            if (seenIssueIds.contains(requestedIssueId) || satisfiedIssueIds.contains(requestedIssueId)
                    || issueSkipReasons.putIfAbsent(requestedIssueId, REQUESTED_ISSUE_NOT_FOUND) != null) {
                continue;
            }
            skippedByReason.merge(REQUESTED_ISSUE_NOT_FOUND, 1, Integer::sum);
            if (options.isPreview()) {
                previewDetailsByIssue.put(requestedIssueId,
                    PreviewDetail.skipped(requestedIssueId, null));
            }
        }
    }

    private void recordSatisfied(String instanceId) {
        if (instanceId != null && !instanceId.isBlank()) {
            satisfiedIssueIds.add(instanceId);
            issueSkipReasons.remove(instanceId);
        }
    }
}