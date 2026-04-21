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
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;

/**
 * Stateless utility helpers for the correlate-sast-dast command:
 * output JSON construction, suppression check, and FPR path validation.
 */
public final class AviatorSSCCorrelateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCCorrelateHelper.class);

    private AviatorSSCCorrelateHelper() {}

    /**
     * Builds the final JSON output node for the correlate-sast-dast command.
     */
    public static ObjectNode buildOutputJson(SSCAppVersionDescriptor av,
                                              String artifactId,
                                              int submitted,
                                              List<CorrelatedPair> newPairs,
                                              String actionResult) {
        int correlated = newPairs.size();
        int skipped = submitted - correlated;

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

        if (submitted > 0) {
            String message = String.format("%d SAST findings submitted, %d correlated pairs confirmed",
                    submitted, correlated);
            correlate.put("message", message);
            correlate.put("submitted", submitted);
            correlate.put("skipped", skipped);
        } else {
            correlate.putNull("message");
            correlate.putNull("submitted");
            correlate.putNull("skipped");
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
