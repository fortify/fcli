package com.fortify.cli.aviator.ssc.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetOptionMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetDescriptor;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetHelper;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for the AviatorSSCAuditCommand to encapsulate
 * result message formatting, JSON output construction, and pre-audit checks.
 */
public final class AviatorSSCAuditHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCAuditHelper.class);
    private static final int PAGE_LIMIT = 200;
    private AviatorSSCAuditHelper() {}

    /**
     * Builds the final JSON result node for the command output.
     * @param av The SSCAppVersionDescriptor.
     * @param artifactId The ID of the uploaded artifact, or a status string.
     * @param action The final action string for the output.
     * @return An ObjectNode representing the result.
     */
    public static ObjectNode buildResultNode(SSCAppVersionDescriptor av, String artifactId, String action) {
        ObjectNode result = av.asObjectNode();
        result.put("id", av.getVersionId());
        result.put("applicationName", av.getApplicationName());
        result.put("name", av.getVersionName());
        result.put("artifactId", artifactId);
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

    /**
     * Generates a detailed action string based on the FPRAuditResult.
     * This is used for the 'action' column in the output.
     * @param auditResult The result from the Aviator audit.
     * @return A descriptive string of the outcome.
     */
    public static String getDetailedAction(FPRAuditResult auditResult) {
        switch (auditResult.getStatus()) {
            case "SKIPPED":
                String reason = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown reason";
                return "SKIPPED (" + reason + ")";
            case "FAILED":
                String message = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown error";
                return "FAILED (" + message + ")";
            case "PARTIALLY_AUDITED":
                return String.format("PARTIALLY_AUDITED (%d/%d audited)",
                        auditResult.getIssuesSuccessfullyAudited(),
                        auditResult.getTotalIssuesToAudit());
            case "AUDITED":
                return "AUDITED";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Generates a progress message for the user based on the FPRAuditResult.
     * @param auditResult The result from the Aviator audit.
     * @return A user-friendly progress message.
     */
    public static String getProgressMessage(FPRAuditResult auditResult) {
        switch (auditResult.getStatus()) {
            case "SKIPPED":
                return "No issues to audit, skipping upload";
            case "FAILED":
                String message = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown error";
                return "Audit failed: " + message;
            case "PARTIALLY_AUDITED":
            case "AUDITED":
                return auditResult.getUpdatedFile() != null
                        ? "Status: Uploading audited FPR to SSC"
                        : "Audit completed but no file updated";
            default:
                return "Unknown audit status";
        }
    }

    /**
     * Queries SSC to get the number of auditable issues for a given application version.
     */
    public static long getAuditableIssueCount(UnirestInstance unirest, SSCAppVersionDescriptor av, AviatorLoggerImpl logger, boolean noFilterSet, SSCIssueFilterSetOptionMixin filterSetOptions, List<String> folderNames) {
        logger.progress("Status: Checking for auditable issues...");

        GetRequest request = unirest.get(SSCUrls.PROJECT_VERSION_ISSUES(av.getVersionId()))
                .queryString("limit", PAGE_LIMIT)
                .queryString("embed", "auditValues")
                .queryString("qm", "issues")
                .queryString("q", "audited:false");

        // Apply filter set if specified
        SSCIssueFilterSetDescriptor filterSetDescriptor = null;
        if (!noFilterSet) {
            SSCIssueFilterSetHelper filterSetHelper = new SSCIssueFilterSetHelper(unirest, av.getVersionId());
            filterSetDescriptor = filterSetHelper.getDescriptorByTitleOrId(filterSetOptions.getFilterSetTitleOrId(), false);
            if (filterSetDescriptor != null) {
                logger.progress("Status: Applying filter set '%s' for issue count check", filterSetDescriptor.getTitle());
                request.queryString("filterset", filterSetDescriptor.getGuid());
            }
        }

        // Apply folder filter if specified
        if (folderNames != null && !folderNames.isEmpty()) {
            String folderFilter = getFolderFilter(noFilterSet, filterSetDescriptor, folderNames);
            request.queryString("filter", folderFilter);
            logger.progress("Status: Applying folder filter for: %s", String.join(", ", folderNames));
        }

        long totalAuditableCount = 0;
        int start = 0;
        long totalFromServer = -1;

        try {
            do {
                JsonNode response = request.queryString("start", start).asObject(JsonNode.class).getBody();
                if (response == null || !response.has("data")) {
                    LOG.warn("Invalid response received from issue check; proceeding with FPR download.");
                    logger.progress("WARN: Invalid response from issue check. Proceeding with FPR download.");
                    return -1;
                }
                if (totalFromServer == -1) {
                    totalFromServer = response.get("count").asLong(0);
                }

                ArrayNode issues = (ArrayNode) response.get("data");
                if (issues != null && !issues.isEmpty()) {
                    long auditableOnPage = JsonHelper.stream(issues)
                            .filter(issue -> !isProcessedByAviator(issue))
                            .count();
                    totalAuditableCount += auditableOnPage;
                    start += issues.size();
                } else {
                    break; // No more issues
                }
            } while (start < totalFromServer);

            logger.progress("Status: Found %d auditable issues.", totalAuditableCount);
            return totalAuditableCount;
        } catch (UnexpectedHttpResponseException e) {
            LOG.warn("Failed to retrieve auditable issue count from SSC (HTTP {}). This may happen on older SSC versions. Defaulting to downloading the FPR.", e.getStatus(), e);
            logger.progress("WARN: Failed to retrieve issue count from SSC. Proceeding with FPR download as a fallback.");
            return -1;
        }
    }

    /**
     * Checks if an issue has the 'Aviator status' custom tag set.
     */
    private static boolean isProcessedByAviator(JsonNode issue) {
        JsonNode auditValues = issue.path("_embed").path("auditValues");
        if (!auditValues.isArray()) {
            return false;
        }
        String statusTagGuid = AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid();
        return JsonHelper.stream((ArrayNode) auditValues)
                .anyMatch(tagValue ->
                        statusTagGuid.equals(tagValue.path("customTagGuid").asText()) &&
                                tagValue.has("customTagIndex")
                );
    }

    /**
     * Helper method to construct the folder filter string for the API request.
     */
    private static String getFolderFilter(boolean noFilterSet, SSCIssueFilterSetDescriptor filterSetDescriptor, List<String> folderNames) {
        if (noFilterSet) {
            throw new FcliSimpleException("--folder option cannot be used with --no-filterset.");
        }
        if (filterSetDescriptor == null) {
            throw new FcliSimpleException("--folder option requires an active filter set. Use --filterset or ensure a default filter set exists.");
        }

        final JsonNode foldersNode = filterSetDescriptor.asJsonNode().get("folders");
        if (foldersNode == null || !foldersNode.isArray()) {
            throw new FcliSimpleException("Could not retrieve folders for the specified filter set: " + filterSetDescriptor.getTitle());
        }

        Map<String, String> folderNameToGuidMap = new HashMap<>();
        JsonHelper.stream((ArrayNode) foldersNode).forEach(folder ->
                folderNameToGuidMap.put(folder.get("name").asText().toLowerCase(), folder.get("guid").asText())
        );

        final SSCIssueFilterSetDescriptor finalFilterSetDescriptor = filterSetDescriptor;
        return folderNames.stream()
                .map(String::toLowerCase)
                .map(folderName -> {
                    String guid = folderNameToGuidMap.get(folderName);
                    if (guid == null) {
                        String availableFolders = JsonHelper.stream((ArrayNode) foldersNode).map(f -> f.get("name").asText()).collect(Collectors.joining(", "));
                        throw new FcliSimpleException(String.format("Folder '%s' not found in filter set '%s'. Available folders: %s", folderName, finalFilterSetDescriptor.getTitle(), availableFolders));
                    }
                    return String.format("FOLDER:%s", guid);
                })
                .collect(Collectors.joining(" OR "));
    }
}