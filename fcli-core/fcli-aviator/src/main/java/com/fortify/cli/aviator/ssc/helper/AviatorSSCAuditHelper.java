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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetOptionMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetDescriptor;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueGroupDescriptor;
import com.fortify.cli.ssc.issue.helper.SSCIssueGroupHelper;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

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
     * All fields are always present to ensure a stable, predictable output structure.
     * Fields not applicable to the current action are set to null.
     *
     * <p>Structure:
     * <pre>
     * id, applicationName, versionName, artifactId, __action__
     * operation:
     *   audit: { message, submitted, succeeded, skipped }
     * state:
     *   aviator: { availableQuotaBefore, availableQuotaAfter }
     *   ssc: { issuesByCategory }
     * </pre>
     *
     * @param av The SSCAppVersionDescriptor.
     * @param artifactId The ID of the uploaded artifact, or null if not applicable.
     * @param action The final action string for the output.
     * @return An ObjectNode representing the result.
     */
    public static ObjectNode buildResultNode(SSCAppVersionDescriptor av, String artifactId, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("id", av.getVersionId());
        result.put("applicationName", av.getApplicationName());
        result.put("versionName", av.getVersionName());
        if (artifactId != null) {
            result.put("artifactId", artifactId);
        } else {
            result.putNull("artifactId");
        }
        result.put(IActionCommandResultSupplier.actionFieldName, action);

        // operation.audit — null by default, populated by setAuditStats or setOperationMessage
        ObjectNode operation = JsonHelper.getObjectMapper().createObjectNode();
        operation.putNull("audit");
        result.set("operation", operation);

        // state.aviator and state.ssc — null sub-objects by default
        ObjectNode state = JsonHelper.getObjectMapper().createObjectNode();
        ObjectNode aviatorState = JsonHelper.getObjectMapper().createObjectNode();
        aviatorState.putNull("availableQuotaBefore");
        aviatorState.putNull("availableQuotaAfter");
        state.set("aviator", aviatorState);
        ObjectNode sscState = JsonHelper.getObjectMapper().createObjectNode();
        sscState.putNull("issuesByCategory");
        state.set("ssc", sscState);
        result.set("state", state);

        return result;
    }

    /**
     * Populates the {@code operation.audit} object on the result node with stats from the FPR audit.
     * Sets message, submitted, succeeded, and skipped fields.
     *
     * @param result The result node to update.
     * @param auditResult The result from the Aviator audit.
     */
    public static void setAuditStats(ObjectNode result, FPRAuditResult auditResult) {
        ObjectNode audit = JsonHelper.getObjectMapper().createObjectNode();
        String message;
        switch (auditResult.getStatus()) {
            case "AUDITED":
                message = "Audit completed successfully";
                break;
            case "PARTIALLY_AUDITED":
                message = auditResult.getMessage() != null ? auditResult.getMessage() : "Audit partially completed";
                break;
            case "SKIPPED":
                message = auditResult.getMessage() != null ? auditResult.getMessage() : "No issues to audit";
                break;
            case "FAILED":
                message = auditResult.getMessage() != null ? auditResult.getMessage() : "Audit failed";
                break;
            default:
                message = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown audit status";
                break;
        }
        audit.put("message", message);
        audit.put("submitted", auditResult.getTotalIssuesToAudit());
        audit.put("succeeded", auditResult.getIssuesSuccessfullyAudited());
        audit.put("skipped", Math.max(0, auditResult.getTotalIssuesToAudit() - auditResult.getIssuesSuccessfullyAudited()));
        ((ObjectNode) result.get("operation")).set("audit", audit);
    }

    /**
     * Sets only the {@code operation.audit.message} field without audit stats.
     * Used for code paths that don't perform an actual audit (SKIPPED, FAILED, QUOTA_EXCEEDED, etc.).
     *
     * @param result The result node to update.
     * @param message The descriptive message.
     */
    public static void setOperationMessage(ObjectNode result, String message) {
        ObjectNode audit = JsonHelper.getObjectMapper().createObjectNode();
        audit.put("message", message);
        audit.putNull("submitted");
        audit.putNull("succeeded");
        audit.putNull("skipped");
        ((ObjectNode) result.get("operation")).set("audit", audit);
    }

    /**
     * Sets the {@code state.aviator.availableQuotaBefore} field.
     *
     * @param result The result node to update.
     * @param quota The quota value.
     */
    public static void setAvailableQuotaBefore(ObjectNode result, long quota) {
        ((ObjectNode) result.path("state").path("aviator")).put("availableQuotaBefore", quota);
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

        LOG.debug("Starting auditable issue count for SSC version {} (application='{}', version='{}') with pageLimit={}, noFilterSet={}",
                av.getVersionId(), av.getApplicationName(), av.getVersionName(), PAGE_LIMIT, noFilterSet);

        // Apply filter set if specified
        SSCIssueFilterSetDescriptor filterSetDescriptor = null;
        String filterSetGuid = null;
        if (!noFilterSet) {
            SSCIssueFilterSetHelper filterSetHelper = new SSCIssueFilterSetHelper(unirest, av.getVersionId());
            filterSetDescriptor = filterSetHelper.getDescriptorByTitleOrId(filterSetOptions.getFilterSetTitleOrId(), false);
            if (filterSetDescriptor != null) {
                logger.progress("Status: Applying filter set '%s' for issue count check", filterSetDescriptor.getTitle());
            filterSetGuid = filterSetDescriptor.getGuid();
                LOG.debug("Applied SSC filter set '{}' with guid {} while counting auditable issues for version {}",
                filterSetDescriptor.getTitle(), filterSetGuid, av.getVersionId());
            } else {
                LOG.debug("No SSC filter set resolved from options while counting auditable issues for version {}",
                        av.getVersionId());
            }
        }

        // Apply folder filter if specified
        String folderFilter = null;
        if (folderNames != null && !folderNames.isEmpty()) {
            folderFilter = getFolderFilter(noFilterSet, filterSetDescriptor, folderNames);
            logger.progress("Status: Applying folder filter for: %s", String.join(", ", folderNames));
            LOG.debug("Applied folder filter '{}' for folders {} while counting auditable issues for version {}",
                    folderFilter, folderNames, av.getVersionId());
        }

        long totalAuditableCount = 0;
        int start = 0;
        long totalFromServer = -1;

        try {
            do {
                GetRequest request = unirest.get(SSCUrls.PROJECT_VERSION_ISSUES(av.getVersionId()))
                        .queryString("limit", PAGE_LIMIT)
                        .queryString("embed", "auditValues")
                        .queryString("qm", "issues")
                        .queryString("q", "audited:false")
                        .queryString("start", start);
                if (filterSetGuid != null) {
                    request.queryString("filterset", filterSetGuid);
                }
                if (folderFilter != null) {
                    request.queryString("filter", folderFilter);
                }
                LOG.debug("Requesting SSC issues page for version {} with start={} and limit={}",
                        av.getVersionId(), start, PAGE_LIMIT);
                JsonNode response = request.asObject(JsonNode.class).getBody();
                if (response == null || !response.has("data")) {
                    LOG.warn("Invalid response received from issue check; proceeding with FPR download.");
                    logger.progress("WARN: Invalid response from issue check. Proceeding with FPR download.");
                    return -1;
                }
                if (totalFromServer == -1) {
                    totalFromServer = response.get("count").asLong(0);
                    LOG.debug("SSC reported {} total issues matching the initial auditable count query for version {}",
                            totalFromServer, av.getVersionId());
                }

                ArrayNode issues = (ArrayNode) response.get("data");
                if (issues != null && !issues.isEmpty()) {
                    long auditableOnPage = JsonHelper.stream(issues)
                            .filter(issue -> !isProcessedByAviator(issue))
                            .count();
                    totalAuditableCount += auditableOnPage;
                    LOG.debug("Processed SSC issues page for version {}: pageStart={}, pageSize={}, auditableOnPage={}, cumulativeAuditableCount={}, totalFromServer={}",
                            av.getVersionId(), start, issues.size(), auditableOnPage, totalAuditableCount, totalFromServer);
                    start += issues.size();
                } else {
                    LOG.debug("SSC returned no more issues for version {} at start={}; stopping pagination with cumulativeAuditableCount={} and totalFromServer={}",
                            av.getVersionId(), start, totalAuditableCount, totalFromServer);
                    break; // No more issues
                }
            } while (start < totalFromServer);

            LOG.debug("Completed auditable issue count for version {}: totalAuditableCount={}, totalFromServer={}",
                    av.getVersionId(), totalAuditableCount, totalFromServer);
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

    /**
     * Sentinel value indicating quota retrieval failed.
     */
    public static final long QUOTA_UNKNOWN = Long.MIN_VALUE;

    /**
     * Sentinel value indicating the application was not found on the server.
     */
    public static final long QUOTA_APP_NOT_FOUND = Long.MIN_VALUE + 1;

    /**
     * Retrieves the available quota for the given Aviator application using the developer token.
     * Returns the quota value from the server, {@link #QUOTA_APP_NOT_FOUND} if the app doesn't exist,
     * or {@link #QUOTA_UNKNOWN} if retrieval fails for other reasons.
     * A server-returned value of -1 means unlimited quota.
     */
    public static long getAvailableQuota(String aviatorUrl, String aviatorToken, String appName,
                                          AviatorLoggerImpl logger) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(aviatorUrl, logger,
                Constants.DEFAULT_PING_INTERVAL_SECONDS)) {
            Application app = client.getApplicationByToken(aviatorToken, appName);
            long quota = app.getQuota();
            logger.progress("Status: Available Aviator quota for app '%s': %s", appName,
                    quota < 0 ? "unlimited" : String.valueOf(quota));
            return quota;
        } catch (AviatorSimpleException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                LOG.info("Application '{}' not found on Aviator server.", appName);
                return QUOTA_APP_NOT_FOUND;
            }
            LOG.warn("Failed to retrieve quota for application '{}': {}", appName, e.getMessage());
            logger.progress("WARN: Could not retrieve Aviator quota for app '%s'. Proceeding without quota check.", appName);
            return QUOTA_UNKNOWN;
        } catch (Exception e) {
            LOG.warn("Failed to retrieve quota for application '{}': {}", appName, e.getMessage());
            logger.progress("WARN: Could not retrieve Aviator quota for app '%s'. Proceeding without quota check.", appName);
            return QUOTA_UNKNOWN;
        }
    }

    /**
     * Retrieves the default initial quota for new applications from the tenant.
     * Returns the default quota, or {@link #QUOTA_UNKNOWN} if retrieval fails.
     */
    public static long getDefaultQuota(String aviatorUrl, String aviatorToken,
                                        AviatorLoggerImpl logger) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(aviatorUrl, logger,
                Constants.DEFAULT_PING_INTERVAL_SECONDS)) {
            long quota = client.getDefaultQuota(aviatorToken);
            logger.progress("Status: Default Aviator quota for new applications: %s",
                    quota < 0 ? "unlimited" : String.valueOf(quota));
            return quota;
        } catch (Exception e) {
            LOG.warn("Failed to retrieve default quota: {}", e.getMessage());
            logger.progress("WARN: Could not retrieve default Aviator quota.");
            return QUOTA_UNKNOWN;
        }
    }

    /**
     * Returns the top N SAST categories ordered by truly-unaudited issue count (descending).
     * Uses the SSC issueGroups API with:
     *   - groupingtype = dynamically resolved "Category" GUID
     *   - filter = dynamically resolved "Aviator status:Not Set" technical filter
     * Each entry contains "categoryName" and "unauditedCount".
     */
    public static List<Map<String, Object>> getTopUnauditedCategories(
            UnirestInstance unirest, SSCAppVersionDescriptor av,
            AviatorLoggerImpl logger, int topN) {

        try {
            return getTopUnauditedCategoriesInternal(unirest, av, logger, topN);
        } catch (Exception e) {
            LOG.warn("Failed to retrieve top unaudited categories for {}:{}: {}",
                av.getApplicationName(), av.getVersionName(), e.getMessage());
            logger.progress("WARN: Could not retrieve category breakdown from SSC.");
            return List.of();
        }
    }

    private static List<Map<String, Object>> getTopUnauditedCategoriesInternal(
            UnirestInstance unirest, SSCAppVersionDescriptor av,
            AviatorLoggerImpl logger, int topN) {

        String versionId = av.getVersionId();

        // Resolve "Category" grouping GUID dynamically
        SSCIssueGroupHelper groupHelper = new SSCIssueGroupHelper(unirest, versionId);
        SSCIssueGroupDescriptor categoryDescriptor = groupHelper.getDescriptorByDisplayNameOrId("Category", true);
        String categoryGroupGuid = categoryDescriptor.getGuid();

        // Resolve "Aviator status:Not Set" filter dynamically
        String aviatorStatusFilter = null;
        try {
            SSCIssueFilterHelper filterHelper = new SSCIssueFilterHelper(unirest, versionId);
            aviatorStatusFilter = filterHelper.getFilter("Aviator status:Not Set");
        } catch (FcliSimpleException e) {
            // Tag doesn't exist on this version — all issues are unprocessed
            LOG.debug("Aviator status tag not found for version {}. All issues considered unprocessed.", versionId);
        }

        // Call issueGroups API
        GetRequest request = unirest.get(SSCUrls.PROJECT_VERSION_ISSUE_GROUPS(versionId))
            .queryString("limit", "-1")
            .queryString("qm", "issues")
            .queryString("groupingtype", categoryGroupGuid);

        if (aviatorStatusFilter != null) {
            request.queryString("filter", aviatorStatusFilter);
        }

        JsonNode response = request.asObject(JsonNode.class).getBody();
        ArrayNode groups = (ArrayNode) response.get("data");

        // Calculate truly-unaudited count per category and sort descending
        List<Map<String, Object>> categories = new ArrayList<>();
        if (groups != null) {
            for (JsonNode group : groups) {
                String name = group.path("id").asText("Unknown");
                long visibleCount = group.path("visibleCount").asLong(0);
                long auditedCount = group.path("auditedCount").asLong(0);
                long unaudited = visibleCount - auditedCount;
                if (unaudited > 0) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("categoryName", name);
                    entry.put("totalIssues", visibleCount);
                    entry.put("auditedIssues", auditedCount);
                    entry.put("unauditedCount", unaudited);
                    categories.add(entry);
                }
            }
        }

        // Sort descending by unauditedCount
        categories.sort((a, b) -> Long.compare(
            (long) b.get("unauditedCount"),
            (long) a.get("unauditedCount")
        ));

        return categories.subList(0, Math.min(topN, categories.size()));
    }

    /**
     * Builds the JSON result node when a version is skipped due to exceeding quota.
     * Sets {@code state.aviator.availableQuotaBefore} and populates {@code state.ssc.issuesByCategory}
     * with total, audited, and unaudited counts per category.
     */
    public static ObjectNode buildQuotaExceededResultNode(
            SSCAppVersionDescriptor av, long openIssueCount, long availableQuota,
            List<Map<String, Object>> topCategories) {
        ObjectNode result = buildResultNode(av, null, "QUOTA_EXCEEDED");
        setOperationMessage(result, String.format("Open issues (%d) exceed available quota (%d)", openIssueCount, availableQuota));
        setAvailableQuotaBefore(result, availableQuota);
        ((ObjectNode) result.path("state").path("ssc")).set("issuesByCategory", buildIssuesByCategoryArray(topCategories));
        return result;
    }

    /**
     * Converts the category list into a JSON array for the {@code issuesByCategory} field.
     */
    private static ArrayNode buildIssuesByCategoryArray(List<Map<String, Object>> categories) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (categories != null) {
            for (Map<String, Object> cat : categories) {
                ObjectNode catNode = JsonHelper.getObjectMapper().createObjectNode();
                catNode.put("category", (String) cat.get("categoryName"));
                catNode.put("total", (long) cat.get("totalIssues"));
                catNode.put("audited", (long) cat.get("auditedIssues"));
                catNode.put("unaudited", (long) cat.get("unauditedCount"));
                array.add(catNode);
            }
        }
        return array;
    }

    /**
     * Formats a human-readable message for quota exceeded scenarios.
     */
    public static String formatQuotaExceededMessage(
            SSCAppVersionDescriptor av, long openIssueCount, long availableQuota,
            List<Map<String, Object>> topCategories) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Quota exceeded for %s:%s -- Open issues: %d, Available quota: %d%n",
            av.getApplicationName(), av.getVersionName(), openIssueCount, availableQuota));
        sb.append("Top SAST categories by unaudited issue count:\n");
        if (topCategories != null) {
            int rank = 1;
            for (Map<String, Object> cat : topCategories) {
                sb.append(String.format("  %d. %s (total: %d, audited: %d, unaudited: %d)%n",
                    rank++, cat.get("categoryName"), cat.get("totalIssues"),
                    cat.get("auditedIssues"), cat.get("unauditedCount")));
            }
        }
        return sb.toString();
    }
}