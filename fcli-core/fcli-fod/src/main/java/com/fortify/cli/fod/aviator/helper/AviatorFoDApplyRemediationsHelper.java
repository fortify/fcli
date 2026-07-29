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
package com.fortify.cli.fod.aviator.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

public class AviatorFoDApplyRemediationsHelper {
    public AviatorFoDApplyRemediationsHelper() {}

    /**
     * Builds the final JSON result node for the command output.
     * @param rd The SSCAppVersionDescriptor.
     * @param totalRemediation Total no. of Remediations
     * @param appliedRemediation Remediations that has been applied successfully
     * @param skippedRemediation Remediations that has been skipped
     * @param action Final action.
     * @return An ObjectNode representing the result.
     */
    public static ObjectNode buildResultNode(FoDReleaseDescriptor rd, int totalRemediation, int appliedRemediation, int skippedRemediation,
            String action) {
        return buildResultNode(rd, totalRemediation, appliedRemediation, skippedRemediation, Set.of(), Map.of(), action);
    }

    public static ObjectNode buildResultNode(FoDReleaseDescriptor rd, int totalRemediation, int appliedRemediation, int skippedRemediation,
            Set<String> modifiedFiles, String action) {
        return buildResultNode(rd, totalRemediation, appliedRemediation, skippedRemediation, modifiedFiles, Map.of(), action);
    }

    public static ObjectNode buildResultNode(FoDReleaseDescriptor rd, int totalRemediation, int appliedRemediation, int skippedRemediation,
            Set<String> modifiedFiles, Map<String, Integer> skippedByReason, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("releaseId", rd.getReleaseId());
        result.put("applicationName", rd.getApplicationName());
        result.put("releaseName", rd.getReleaseName());
        result.put("totalRemediation", totalRemediation);
        result.put("appliedRemediation", appliedRemediation);
        result.put("skippedRemediation", skippedRemediation);
        result.put("skippedReasons", formatSkippedReasons(skippedByReason));
        result.set("skippedByReason", toObjectNode(skippedByReason));
        result.set("modifiedFiles", toArrayNode(modifiedFiles));
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

    private static ArrayNode toArrayNode(Set<String> files) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (files != null) {
            files.forEach(array::add);
        }
        return array;
    }

    private static ObjectNode toObjectNode(Map<String, Integer> skippedByReason) {
        ObjectNode object = JsonHelper.getObjectMapper().createObjectNode();
        if (skippedByReason != null) {
            skippedByReason.forEach(object::put);
        }
        return object;
    }

    private static String formatSkippedReasons(Map<String, Integer> skippedByReason) {
        if (skippedByReason == null || skippedByReason.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        skippedByReason.forEach((reason, count) -> parts.add(reason + "=" + count));
        return String.join(", ", parts);
    }
}
