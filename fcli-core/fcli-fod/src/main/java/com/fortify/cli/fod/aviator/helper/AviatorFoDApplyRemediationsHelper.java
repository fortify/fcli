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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

/**
 * Helper for FoD apply-remediations result JSON construction.
 */
public final class AviatorFoDApplyRemediationsHelper {
    private AviatorFoDApplyRemediationsHelper() {}

    public static ObjectNode buildResultNode(FoDReleaseDescriptor rd, int totalRemediation, int appliedRemediation,
            int skippedRemediation, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("releaseId", rd.getReleaseId());
        result.put("applicationName", rd.getApplicationName());
        result.put("releaseName", rd.getReleaseName());
        result.put("totalRemediation", totalRemediation);
        result.put("appliedRemediation", appliedRemediation);
        result.put("skippedRemediation", skippedRemediation);
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

    public static ObjectNode buildResultNode(FoDReleaseDescriptor rd, int totalRemediation, int appliedRemediation,
            int skippedRemediation, Set<String> modifiedFiles, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("releaseId", rd.getReleaseId());
        result.put("applicationName", rd.getApplicationName());
        result.put("releaseName", rd.getReleaseName());
        result.put("totalRemediation", totalRemediation);
        result.put("appliedRemediation", appliedRemediation);
        result.put("skippedRemediation", skippedRemediation);
        result.set("modifiedFiles", toArrayNode(modifiedFiles));
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

    /**
     * Result shape for --from-cache: durable cache zip path and zip-relative entry paths only
     * (never ephemeral extract-dir absolute paths).
     */
    public static ObjectNode buildCacheResultNode(CacheResultData resultData) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("releaseId", resultData.releaseIds() != null && !resultData.releaseIds().isEmpty() ? resultData.releaseIds().get(0) : "N/A");
        result.put("applicationName", "N/A");
        result.put("releaseName", "N/A");
        result.put("file", resultData.cacheZip().toString());
        result.put("totalRemediation", resultData.totalRemediation());
        result.put("appliedRemediation", resultData.appliedRemediation());
        result.put("skippedRemediation", resultData.skippedRemediation());
        result.set("modifiedFiles", toArrayNode(resultData.modifiedFiles()));
        result.set("entries", toStringArrayNode(resultData.entryPaths()));
        result.set("releaseIds", toStringArrayNode(resultData.releaseIds()));
        result.put(IActionCommandResultSupplier.actionFieldName, resultData.action());
        return result;
    }

    public record CacheResultData(
            Path cacheZip,
            List<String> entryPaths,
            List<String> releaseIds,
            int totalRemediation,
            int appliedRemediation,
            int skippedRemediation,
            Set<String> modifiedFiles,
            String action) {}

    private static ArrayNode toArrayNode(Set<String> files) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (files != null) {
            files.forEach(array::add);
        }
        return array;
    }

    private static ArrayNode toStringArrayNode(List<String> values) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (values != null) {
            values.forEach(array::add);
        }
        return array;
    }
}
