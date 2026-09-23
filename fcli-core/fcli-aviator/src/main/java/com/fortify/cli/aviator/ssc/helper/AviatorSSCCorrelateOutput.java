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

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.grpc.CorrelationResult;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;

import lombok.Builder;

/**
 * Builder-backed correlation command output model.
 *
 * @param appVersion SSC application version
 * @param artifactId uploaded artifact identifier, or {@code null}
 * @param correlationResult correlation stream result
 * @param actionResult action result exposed to action consumers
 * @param message optional user-facing result message
 */
@Builder
public record AviatorSSCCorrelateOutput(
    SSCAppVersionDescriptor appVersion,
    String artifactId,
    CorrelationResult correlationResult,
    String actionResult,
    String message
) {
    /**
     * Builds the standard JSON output for an SSC SAST-DAST correlation run.
     *
     * @return correlation command output
     */
    public ObjectNode toJsonNode() {
        Objects.requireNonNull(appVersion, "appVersion must not be null");
        Objects.requireNonNull(correlationResult, "correlationResult must not be null");
        Objects.requireNonNull(actionResult, "actionResult must not be null");

        int submitted = correlationResult.submittedCorrelationRequests();
        int normalizedSucceeded = Math.max(0,
            Math.min(correlationResult.successfulCorrelationResponses(), submitted));
        int normalizedSkipped = Math.max(0,
            Math.min(correlationResult.skippedCorrelationResponses(), submitted - normalizedSucceeded));
        int normalizedFailed = Math.max(0, Math.min(correlationResult.failedCorrelationResponses(),
            submitted - normalizedSucceeded - normalizedSkipped));
        int correlated = correlationResult.confirmedPairs().size();

        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("id", appVersion.getVersionId());
        result.put("applicationName", appVersion.getApplicationName());
        result.put("versionName", appVersion.getVersionName());
        if (artifactId == null) {
            result.putNull("artifactId");
        } else {
            result.put("artifactId", artifactId);
        }
        result.put(IActionCommandResultSupplier.actionFieldName, actionResult);

        ObjectNode correlate = result.putObject("operation").putObject("correlate");
        addMessage(correlate, submitted, normalizedSucceeded, normalizedSkipped, normalizedFailed, correlated);
        addStatistics(correlate, submitted, normalizedSucceeded, normalizedSkipped, normalizedFailed);
        correlate.put("correlated", correlated);
        return result;
    }

    private void addMessage(ObjectNode correlate, int submitted, int succeeded, int skipped, int failed,
            int correlated) {
        if (message != null) {
            correlate.put("message", message);
        } else if (submitted > 0) {
            correlate.put("message", String.format(
                "%d SAST findings submitted: %d succeeded, %d skipped, %d failed; %d correlated pairs confirmed",
                submitted, succeeded, skipped, failed, correlated));
        } else {
            correlate.putNull("message");
        }
    }

    private static void addStatistics(ObjectNode correlate, int submitted, int succeeded, int skipped, int failed) {
        if (submitted > 0) {
            correlate.put("submitted", submitted);
            correlate.put("succeeded", succeeded);
            correlate.put("skipped", skipped);
            correlate.put("failed", failed);
        } else {
            correlate.putNull("submitted");
            correlate.putNull("succeeded");
            correlate.putNull("skipped");
            correlate.putNull("failed");
        }
    }
}