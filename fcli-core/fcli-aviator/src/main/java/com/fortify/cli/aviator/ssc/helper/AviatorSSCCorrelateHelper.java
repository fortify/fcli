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
package com.fortify.cli.aviator.ssc.helper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.aviator.grpc.CorrelationResult;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

/**
 * Stateless utility helpers for the correlate-sast-dast command:
 * output JSON construction, suppression check, and FPR path validation.
 */
public final class AviatorSSCCorrelateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCCorrelateHelper.class);

    private AviatorSSCCorrelateHelper() {}

    /**
     * Builds the final JSON output node for the correlate-sast-dast command.
     * Submitted and succeeded describe Phase 1 SAST finding requests, matching
     * SAST audit semantics. Correlated counts newly confirmed Phase 2 pairs.
     */
    public static ObjectNode buildOutputJson(SSCAppVersionDescriptor av,
                                              String artifactId,
                                              CorrelationResult correlationResult,
                                              String actionResult) {
        return buildOutputJson(av, artifactId, correlationResult, actionResult, null);
    }

    public static ObjectNode buildOutputJson(SSCAppVersionDescriptor av,
                                              String artifactId,
                                              CorrelationResult correlationResult,
                                              String actionResult,
                                              String actionMessage) {
        return buildOutputJson(
            av,
            artifactId,
            correlationResult.submittedCorrelationRequests(),
            correlationResult.successfulCorrelationResponses(),
            correlationResult.skippedCorrelationResponses(),
            correlationResult.failedCorrelationResponses(),
            correlationResult.confirmedPairs(),
            actionResult,
            actionMessage
        );
    }

    public static ObjectNode buildOutputJson(SSCAppVersionDescriptor av,
                                              String artifactId,
                                              int submitted,
                                              int succeeded,
                                              int skipped,
                                              int failed,
                                              List<CorrelatedPair> newPairs,
                                              String actionResult) {
        return buildOutputJson(av, artifactId, submitted, succeeded, skipped, failed, newPairs, actionResult, null);
    }

    public static ObjectNode buildOutputJson(SSCAppVersionDescriptor av,
                                              String artifactId,
                                              int submitted,
                                              int succeeded,
                                              int skipped,
                                              int failed,
                                              List<CorrelatedPair> newPairs,
                                              String actionResult,
                                              String actionMessage) {
        int correlated = newPairs.size();
        int normalizedSucceeded = Math.max(0, Math.min(succeeded, submitted));
        int normalizedSkipped = Math.max(0, Math.min(skipped, submitted - normalizedSucceeded));
        int normalizedFailed = Math.max(0, Math.min(failed,
            submitted - normalizedSucceeded - normalizedSkipped));

        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("id", av.getVersionId());
        result.put("applicationName", av.getApplicationName());
        result.put("versionName", av.getVersionName());
        if (artifactId != null) {
            result.put("artifactId", artifactId);
        } else {
            result.putNull("artifactId");
        }
        result.put(IActionCommandResultSupplier.actionFieldName, actionResult);

        ObjectNode operation = result.putObject("operation");
        ObjectNode correlate = operation.putObject("correlate");

        if (actionMessage != null) {
            correlate.put("message", actionMessage);
        } else if (submitted > 0) {
            String message = String.format(
                "%d SAST findings submitted: %d succeeded, %d skipped, %d failed; %d correlated pairs confirmed",
                submitted, normalizedSucceeded, normalizedSkipped, normalizedFailed, correlated);
            correlate.put("message", message);
        } else {
            correlate.putNull("message");
        }
        if (submitted > 0) {
            correlate.put("submitted", submitted);
            correlate.put("succeeded", normalizedSucceeded);
            correlate.put("skipped", normalizedSkipped);
            correlate.put("failed", normalizedFailed);
        } else {
            correlate.putNull("submitted");
            correlate.putNull("succeeded");
            correlate.putNull("skipped");
            correlate.putNull("failed");
        }
        correlate.put("correlated", correlated);

        return result;
    }

    /**
     * Returns true if the given vulnerability is marked as suppressed in the audit map.
     */
    public static boolean isVulnerabilitySuppressed(Vulnerability vuln, Map<String, AuditIssue> auditIssueMap) {
        if (auditIssueMap == null || vuln.getInstanceID() == null) {
            return false;
        }
        AuditIssue auditIssue = auditIssueMap.get(vuln.getInstanceID());
        return auditIssue != null && auditIssue.isSuppressed();
    }

    /**
     * Returns whether the latest successful SAST and DAST scans are from the same
     * Aviator-generated mixed artifact.
     */
    public static boolean isUnchangedSinceCorrelation(
            SSCArtifactDescriptor sastArtifact,
            SSCArtifactDescriptor dastArtifact) {
        return sastArtifact.getId().equals(dastArtifact.getId())
            && SSCArtifactHelper.isAviatorArtifact(dastArtifact);
    }

    /**
     * Validates that the downloaded FPR path is non-null and points to an existing regular file.
     */
    public static void validateDownloadedFpr(Path fprPath, String label) {
        LOG.debug("Validate Download FPR {}", label);
        if (fprPath == null) {
            throw new FcliSimpleException(label + " FPR path is null; download may have failed");
        }
        if (!Files.exists(fprPath)) {
            throw new FcliSimpleException(label + " FPR file does not exist: " + fprPath);
        }
        if (!Files.isRegularFile(fprPath)) {
            throw new FcliSimpleException(label + " FPR path is not a regular file: " + fprPath);
        }
    }
}
