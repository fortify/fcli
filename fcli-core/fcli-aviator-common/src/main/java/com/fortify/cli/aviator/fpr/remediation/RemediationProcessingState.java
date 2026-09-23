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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fortify.cli.aviator.fpr.remediation.model.Hunk;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.fpr.remediation.preview.ChangeDetail;
import com.fortify.cli.aviator.fpr.remediation.preview.FilePreview;
import com.fortify.cli.aviator.fpr.remediation.preview.PreviewDetail;
import com.fortify.cli.aviator.fpr.remediation.preview.PreviewFileChange;
import com.fortify.cli.aviator.fpr.remediation.writer.PendingFileWrite;
import com.fortify.cli.aviator.fpr.remediation.writer.PreparedFileChanges;
import com.fortify.cli.aviator.fpr.remediation.writer.PreparedHunkChange;

final class RemediationProcessingState {
    private static final String IDENTICAL_REASON = "Identical remediation already satisfied";
    private static final String SUPERSEDED_REASON = "Superseded by a broader remediation";
    private static final String POSSIBLY_REMEDIATED_REASON = "Possibly remediated by a sibling fix";
    private static final String REQUESTED_ISSUE_NOT_FOUND = "Requested issue not found in remediations";

    private final RemediationProcessingOptions options;
    private final Set<String> seenIssueIds = new LinkedHashSet<>();
    private final Set<String> satisfiedIssueIds = new LinkedHashSet<>();
    private final Set<String> appliedIssueIds = new LinkedHashSet<>();
    private final Set<String> identicalIssueIds = new LinkedHashSet<>();
    private final Set<String> supersededIssueIds = new LinkedHashSet<>();
    private final Set<String> possiblyRemediatedIssueIds = new LinkedHashSet<>();
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
        return !options.isFiltered() || options.issueIdFilter().contains(instanceId);
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

    void recordApplied(String instanceId, PreparedFileChanges prepared) {
        appliedRemediations++;
        recordSatisfied(instanceId);
        appliedIssueIds.add(instanceId);
        issueSkipReasons.remove(instanceId);
        if (options.isPreview()) {
            previewDetailsByIssue.put(instanceId,
                PreviewDetail.available(instanceId, descriptionsByIssue.get(instanceId), toFilePreviews(prepared)));
        }
    }

    void recordIdentical(String instanceId) {
        identicalRemediations++;
        identicalIssueIds.add(instanceId);
        recordSatisfied(instanceId);
        recordPreviewClassification(instanceId, IDENTICAL_REASON);
    }

    void recordSuperseded(String instanceId) {
        supersededRemediations++;
        supersededIssueIds.add(instanceId);
        recordSatisfied(instanceId);
        recordPreviewClassification(instanceId, SUPERSEDED_REASON);
    }

    void recordPossiblyRemediated(String instanceId) {
        possiblyRemediatedRemediations++;
        possiblyRemediatedIssueIds.add(instanceId);
        issueSkipReasons.put(instanceId, POSSIBLY_REMEDIATED_REASON);
        recordPreviewClassification(instanceId, POSSIBLY_REMEDIATED_REASON);
    }

    void recordSkipped(String instanceId, SkipReason reason) {
        recordSkipped(instanceId, reason.displayName());
    }

    void recordSkipped(String instanceId, String reason) {
        skippedByReason.merge(reason, 1, Integer::sum);
        if (instanceId != null && !instanceId.isBlank()) {
            issueSkipReasons.put(instanceId, reason);
            if (options.isPreview()) {
                previewDetailsByIssue.put(instanceId,
                    PreviewDetail.skipped(instanceId, descriptionsByIssue.get(instanceId), reason));
            }
        }
    }

    Set<String> modifiedFiles() {
        return modifiedFiles;
    }

    Map<String, Integer> skippedByReason() {
        return skippedByReason;
    }

    RemediationMetric toMetric() {
        recordMissingRequestedIssues();
        int totalRemediations = options.isFiltered() ? options.issueIdFilter().size() : xmlEntryCount;
        int applied = options.isFiltered() ? appliedIssueIds.size() : appliedRemediations;
        int skipped = options.isFiltered()
            ? totalRemediations - satisfiedIssueIds.size()
            : totalRemediations - appliedRemediations - identicalRemediations - supersededRemediations
                - possiblyRemediatedRemediations;
        return RemediationMetric.builder()
                .totalRemediations(totalRemediations)
                .appliedRemediations(applied)
                .identicalRemediations(identicalRemediations)
                .supersededRemediations(supersededRemediations)
                .possiblyRemediatedRemediations(possiblyRemediatedRemediations)
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
                    PreviewDetail.skipped(requestedIssueId, null, REQUESTED_ISSUE_NOT_FOUND));
            }
        }
    }

    private void recordSatisfied(String instanceId) {
        if (instanceId != null && !instanceId.isBlank()) {
            satisfiedIssueIds.add(instanceId);
            issueSkipReasons.remove(instanceId);
        }
    }

    private void recordPreviewClassification(String instanceId, String reason) {
        if (options.isPreview() && instanceId != null && !instanceId.isBlank()) {
            previewDetailsByIssue.put(instanceId,
                PreviewDetail.skipped(instanceId, descriptionsByIssue.get(instanceId), reason));
        }
    }

    private Map<String, FilePreview> toFilePreviews(PreparedFileChanges prepared) {
        Map<String, String> encodings = new LinkedHashMap<>();
        for (PendingFileWrite pendingWrite : prepared.pendingWrites().values()) {
            encodings.put(pendingWrite.filename(), pendingWrite.charset().name());
        }

        Map<String, List<PreviewFileChange>> changesByFile = new LinkedHashMap<>();
        for (PreparedHunkChange preparedChange : prepared.preparedHunkChanges()) {
            Hunk hunk = preparedChange.hunk();
            ChangeDetail detail = ChangeDetail.builder()
                    .changeIndex(preparedChange.changeIndex())
                    .lineFrom(preparedChange.actualLineFrom())
                    .lineTo(preparedChange.actualLineTo())
                    .originalCode(hunk.requiredOriginalCode())
                    .newCode(hunk.requiredNewCode())
                    .contextLinesBefore(hunk.contextBeforeOrZero())
                    .contextLinesAfter(hunk.contextAfterOrZero())
                    .contextContent(hunk.requiredContextText())
                    .fuzzyMatched(preparedChange.fuzzyMatched())
                    .build();
            changesByFile.computeIfAbsent(preparedChange.filename(), ignored -> new ArrayList<>())
                    .add(detail.toPreviewFileChange());
        }

        Map<String, FilePreview> result = new LinkedHashMap<>();
        changesByFile.forEach((filename, changes) ->
            result.put(filename, new FilePreview(filename, encodings.get(filename), List.copyOf(changes))));
        return result;
    }
}