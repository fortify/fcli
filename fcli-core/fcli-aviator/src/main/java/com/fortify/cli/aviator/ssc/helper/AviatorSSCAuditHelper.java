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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.audit.DastAuditFprResult;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.grpc.AviatorRequestPolicy;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;
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
    private static final String SSC_SCAN_TYPE_SAST = "SCA";
    public static final String NO_SAST_RESULTS_MESSAGE =
            "Application version does not contain SAST (Fortify SCA) scan results, which are required by this operation.";
    private AviatorSSCAuditHelper() {}

    /**
     * Presence of SSC candidates for an FPR download. UNKNOWN preserves the
     * existing download fallback when SSC cannot complete the check.
     */
    public enum AuditableIssuePresence { NONE, PRESENT, UNKNOWN }

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
        audit.put("submitted", auditResult.getIssuesSubmitted());
        audit.put("succeeded", auditResult.getIssuesSuccessfullyAudited());
        audit.put("skipped", auditResult.getIssuesSkipped());
        audit.put("skippedReasons", formatSkippedReasons(auditResult.getSkippedByReason()));
        audit.set("skippedByReason", toObjectNode(auditResult.getSkippedByReason()));
        audit.put("remediationGenerationSkipped", auditResult.getRemediationGenerationSkipped());
        audit.put("remediationGenerationSkippedReasons", formatSkippedReasons(auditResult.getRemediationGenerationSkippedByReason()));
        audit.set("remediationGenerationSkippedByReason", toObjectNode(auditResult.getRemediationGenerationSkippedByReason()));
        ((ObjectNode) result.get("operation")).set("audit", audit);
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

    /**
     * Populates the standard audit output envelope with DAST-specific statistics.
     *
     * @param result The result node created by {@link #buildResultNode}.
     * @param auditResult The DAST FPR audit result.
     */
    public static void setDastAuditStats(ObjectNode result, DastAuditFprResult auditResult) {
        ObjectNode audit = JsonHelper.getObjectMapper().createObjectNode();
        audit.put("message", getDastAuditMessage(auditResult));
        audit.put("submitted", auditResult.submitted());
        audit.put("succeeded", auditResult.succeeded());
        audit.put("skipped", auditResult.skipped());
        audit.put("failed", auditResult.failed());
        ((ObjectNode) result.get("operation")).set("audit", audit);
        result.remove("state");
    }

    private static String getDastAuditMessage(DastAuditFprResult auditResult) {
        return switch (auditResult.status()) {
            case AUDITED -> "DAST audit completed successfully";
            case PARTIALLY_AUDITED -> auditResult.message() != null
                ? auditResult.message() : "DAST audit partially completed";
            case SKIPPED -> auditResult.message() != null
                ? auditResult.message() : "No DAST findings to audit";
            case FAILED -> auditResult.message() != null
                ? auditResult.message() : "DAST audit failed";
        };
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
                return (auditResult.getMessage() != null ? auditResult.getMessage() : "No issues to audit")
                        + ", skipping upload";
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
     * Coarse SSC download gate. Force re-audit counts Aviator-processed issues and
     * legacy Analysis-tag candidates even when SSC marks them audited. The Analysis tag
     * is only a compatibility signal; final ownership and human-triage decisions are
     * applied from TagHistory in the FPR.
     */
    public static long getAuditableIssueCount(UnirestInstance unirest, SSCAppVersionDescriptor av, AviatorLoggerImpl logger,
            boolean noFilterSet, String filterSetTitleOrId, List<String> folderNames, boolean forceReaudit) {
        logger.progress("Status: Checking for auditable issues...");
        LOG.debug("Starting auditable issue count for SSC version {} (application='{}', version='{}') with pageLimit={}, noFilterSet={}, forceReaudit={}",
            av.getVersionId(), av.getApplicationName(), av.getVersionName(), PAGE_LIMIT, noFilterSet, forceReaudit);

        AuditableIssueCountQuery query = resolveAuditableIssueCountQuery(
                unirest, av, logger, noFilterSet, filterSetTitleOrId, folderNames, forceReaudit);
        if (query.folderFilter() == null) {
            long serverCount = forceReaudit ? countForceIssuesOnServer(query) : countNormalIssuesOnServer(query);
            if (serverCount >= 0) {
                logger.progress("Status: Found %d auditable issues.", serverCount);
                return serverCount;
            }
        }
        return countEligibleIssues(query, logger, false);
    }

    /**
     * Checks whether any SSC issue is eligible for the requested audit mode,
     * stopping at the first candidate. A presence result is never an issue total
     * and must not be used for quota comparisons.
     */
    public static AuditableIssuePresence getAuditableIssuePresence(UnirestInstance unirest, SSCAppVersionDescriptor av,
            AviatorLoggerImpl logger, boolean noFilterSet, String filterSetTitleOrId, List<String> folderNames,
            boolean forceReaudit) {
        logger.progress("Status: Checking for auditable issues...");
        AuditableIssueCountQuery query = resolveAuditableIssueCountQuery(
                unirest, av, logger, noFilterSet, filterSetTitleOrId, folderNames, forceReaudit);
        long count = countEligibleIssues(query, logger, true);
        return count < 0 ? AuditableIssuePresence.UNKNOWN
                : count == 0 ? AuditableIssuePresence.NONE : AuditableIssuePresence.PRESENT;
    }

    /**
     * Fails if the application version has no SAST (Fortify SCA) scan results. Intended for the case where the
     * preflight finds nothing to audit: that shortcut returns before the FPR is downloaded and validated, so
     * a version containing other results only (for example DAST) would otherwise be reported as "nothing to
     * audit" instead of being rejected. Only the presence of the expected scan type is checked, as SSC may
     * support any number of other scan types through parser plugins.
     */
    public static void failIfNoSastResults(UnirestInstance unirest, SSCAppVersionDescriptor av) {
        int start = 0;
        while (true) {
            JsonNode response = unirest.get(SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()))
                    .queryString("start", start).queryString("limit", PAGE_LIMIT).queryString("embed", "scans")
                    .asObject(JsonNode.class).getBody();
            JsonNode artifacts = response.path("data");
            if (!artifacts.isArray() || artifacts.isEmpty()) { break; }
            for (JsonNode artifact : artifacts) {
                if (SSCArtifactHelper.hasEmbeddedScanType(artifact, SSC_SCAN_TYPE_SAST)) { return; }
            }
            start += PAGE_LIMIT;
            if (start >= response.path("count").asInt(0)) { break; }
        }
        throw new AviatorSimpleException(NO_SAST_RESULTS_MESSAGE);
    }

    private record AuditableIssueCountQuery(UnirestInstance unirest, SSCAppVersionDescriptor av, boolean noFilterSet,
            String filterSetGuid, String folderFilter, boolean forceReaudit) {}

    private record CustomTagSelectors(String notSetFilter, List<String> setFilters) {}

    /**
     * If no audited issue lacks Analysis, every issue in this scope is a force
     * candidate under the existing SSC predicate. This proves an exact count
     * without an OR expression. Other populations retain the full client check.
     */
    private static long countForceIssuesOnServer(AuditableIssueCountQuery query) {
        try {
            CustomTagSelectors analysis = resolveCustomTagSelectors(query, Constants.ANALYSIS_TAG_ID);
            if (analysis == null || !hasVerifiedSuppressionExclusion(query)) {
                return -1;
            }
            long total = fetchServerIssueCount(createServerCountRequest(query, null),
                    issue -> isEligibleForRequestedAudit(issue, true));
            if (total <= 0) {
                return total;
            }
            long auditedWithoutAnalysis = fetchServerIssueCount(
                    createServerCountRequest(query, analysis.notSetFilter()).queryString("q", "audited:true"),
                    issue -> issue.path("audited").asBoolean(false) && !hasAnalysisTag(issue));
            return auditedWithoutAnalysis == 0 ? total : -1;
        } catch (RuntimeException e) {
            LOG.debug("SSC force count unavailable for version {}; using paginated eligibility check", query.av().getVersionId(), e);
            return -1;
        }
    }

    /**
     * Single selectors are supported by SSC versions that silently ignore grouped
     * expressions. Verify that Not Set and every defined value partition the
     * unaudited population before trusting a server count for quota decisions.
     */
    private static long countNormalIssuesOnServer(AuditableIssueCountQuery query) {
        try {
            CustomTagSelectors selectors = resolveCustomTagSelectors(query, AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid());
            if (selectors == null || !hasVerifiedSuppressionExclusion(query)) {
                return -1;
            }
            long total = fetchServerIssueCount(createServerCountRequest(query, null),
                    issue -> !issue.path("audited").asBoolean(false) && !issue.path("suppressed").asBoolean(false));
            if (total <= 0) {
                return total;
            }
            long unprocessed = fetchServerIssueCount(createServerCountRequest(query, selectors.notSetFilter()),
                    issue -> isEligibleForRequestedAudit(issue, false));
            if (unprocessed < 0 || unprocessed > total) {
                return -1;
            }
            long remaining = total - unprocessed;
            for (String filter : selectors.setFilters()) {
                String index = filter.substring(filter.lastIndexOf(':') + 1);
                long processed = fetchServerIssueCount(createServerCountRequest(query, filter), issue ->
                        !issue.path("audited").asBoolean(false) && !issue.path("suppressed").asBoolean(false)
                        && isProcessedByAviator(issue) && JsonHelper.stream((ArrayNode) issue.path("_embed").path("auditValues"))
                                .anyMatch(tag -> AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid().equals(
                                        tag.path("customTagGuid").asText()) && index.equals(tag.path("customTagIndex").asText())));
                if (processed < 0 || processed > remaining) {
                    return -1;
                }
                remaining -= processed;
            }
            if (remaining != 0) {
                LOG.debug("SSC Aviator status counts do not partition the unaudited population for version {}; using pagination",
                        query.av().getVersionId());
                return -1;
            }
            return unprocessed;
        } catch (RuntimeException e) {
            // This optional optimization must not make older or malformed selector
            // responses fail a count that the existing issue endpoint can perform.
            LOG.debug("SSC server count unavailable for version {}; using paginated eligibility check", query.av().getVersionId(), e);
            return -1;
        }
    }

    private static CustomTagSelectors resolveCustomTagSelectors(AuditableIssueCountQuery query, String guid) {
        var filters = new SSCIssueFilterHelper(query.unirest(), query.av().getVersionId()).getFilterNodes();
        String notSet = null;
        List<String> setFilters = new ArrayList<>();
        for (JsonNode filter : filters) {
            if (!"CUSTOMTAG".equals(filter.path("entityType").asText())
                    || !guid.equals(filter.path("guid").asText()) || !guid.equals(filter.path("value").asText())) {
                continue;
            }
            if (!"LIST".equals(filter.path("filterSelectorType").asText()) || !filter.path("selector").hasNonNull("value")) {
                return null;
            }
            String value = filter.path("selector").path("value").asText();
            if (value.isEmpty()) {
                notSet = filter.path("technicalFilter").asText();
            } else if (value.matches("[0-9]+")) {
                setFilters.add(filter.path("technicalFilter").asText());
            } else {
                return null;
            }
        }
        return notSet == null || setFilters.isEmpty() ? null
                : new CustomTagSelectors(notSet, setFilters.stream().distinct().toList());
    }

    /**
     * Verify visibility using a positive control in the same AV/filter-set scope.
     * An empty inclusion result cannot establish support: the flag or query could
     * have been ignored. Do not cache this observation across scopes or runs.
     */
    private static boolean hasVerifiedSuppressionExclusion(AuditableIssueCountQuery query) {
        long included = fetchServerIssueCount(createSuppressionProbeRequest(query, true),
                issue -> issue.path("suppressed").asBoolean(false));
        if (included <= 0) {
            LOG.debug("No suppressed positive control for version {}; using paginated eligibility check",
                    query.av().getVersionId());
            return false;
        }
        long excluded = fetchServerIssueCount(createSuppressionProbeRequest(query, false),
                issue -> issue.path("suppressed").asBoolean(false));
        if (excluded != 0) {
            LOG.debug("SSC suppression exclusion could not be verified for version {}; using pagination",
                    query.av().getVersionId());
            return false;
        }
        return true;
    }

    private static GetRequest createSuppressionProbeRequest(AuditableIssueCountQuery query, boolean includeSuppressed) {
        // Start from the scope alone so q=audited:false is not combined with, or
        // duplicated by, q=suppressed:true in normal-audit mode.
        return createScopedIssuesRequest(query)
                .queryString("q", "suppressed:true").queryString("showsuppressed", Boolean.toString(includeSuppressed))
                .queryString("limit", 1).queryString("start", 0).queryString("embed", "auditValues");
    }

    private static GetRequest createServerCountRequest(AuditableIssueCountQuery query, String filter) {
        GetRequest request = createAuditableIssuesRequest(query)
                .queryString("limit", 1).queryString("start", 0).queryString("embed", "auditValues")
                // Only the preview is checked client-side; exclude suppressed issues
                // from the entire server count rather than relying on SSC defaults.
                .queryString("showsuppressed", "false");
        if (filter != null) {
            request.queryString("filter", filter);
        }
        return request;
    }

    private static long fetchServerIssueCount(GetRequest request, Predicate<JsonNode> expectedIssue) {
        JsonNode response = request.asObject(JsonNode.class).getBody();
        long count = getReportedIssueCount(response);
        if (count < 0) {
            return -1;
        }
        ArrayNode issues = (ArrayNode) response.path("data");
        if (issues.size() != (count == 0 ? 0 : 1)) {
            return -1;
        }
        return JsonHelper.stream(issues).allMatch(issue -> issue.path("_embed").path("auditValues").isArray()
                && expectedIssue.test(issue)) ? count : -1;
    }

    private static long getReportedIssueCount(JsonNode response) {
        if (response == null || !response.path("data").isArray() || response.path("responseCode").asInt(200) != 200) {
            return -1;
        }
        JsonNode count = response.path("count");
        if (!count.isIntegralNumber() || !count.canConvertToLong() || count.longValue() < 0
                || response.path("data").size() > count.longValue()) {
            return -1;
        }
        return count.longValue();
    }

    private static long issueCountUnavailable(AviatorLoggerImpl logger, String reason) {
        LOG.warn("SSC issue count unavailable: {}.", reason);
        logger.progress("WARN: SSC issue count unavailable: %s.", reason);
        return -1;
    }

    private static AuditableIssueCountQuery resolveAuditableIssueCountQuery(UnirestInstance unirest, SSCAppVersionDescriptor av,
            AviatorLoggerImpl logger, boolean noFilterSet, String filterSetTitleOrId, List<String> folderNames,
            boolean forceReaudit) {
        String effectiveFilterSetTitleOrId = filterSetTitleOrId != null && !filterSetTitleOrId.isBlank()
                ? filterSetTitleOrId
                : null;
        boolean effectiveNoFilterSet = noFilterSet && effectiveFilterSetTitleOrId == null;
        SSCIssueFilterSetDescriptor filterSetDescriptor = resolveFilterSet(
                unirest, av, logger, effectiveNoFilterSet, effectiveFilterSetTitleOrId);
        String filterSetGuid = filterSetDescriptor != null ? filterSetDescriptor.getGuid() : null;
        String folderFilter = resolveFolderFilter(logger, av, effectiveNoFilterSet, filterSetDescriptor, folderNames);
        return new AuditableIssueCountQuery(unirest, av, effectiveNoFilterSet, filterSetGuid, folderFilter, forceReaudit);
    }

    private static SSCIssueFilterSetDescriptor resolveFilterSet(UnirestInstance unirest, SSCAppVersionDescriptor av,
            AviatorLoggerImpl logger, boolean noFilterSet, String filterSetTitleOrId) {
        if (noFilterSet) {
            return null;
        }
        SSCIssueFilterSetHelper filterSetHelper = new SSCIssueFilterSetHelper(unirest, av.getVersionId());
        SSCIssueFilterSetDescriptor filterSetDescriptor = filterSetHelper.getDescriptorByTitleOrId(filterSetTitleOrId, false);
        if (filterSetDescriptor != null) {
            logger.progress("Status: Applying filter set '%s' for issue count check", filterSetDescriptor.getTitle());
            LOG.debug("Applied SSC filter set '{}' with guid {} while counting auditable issues for version {}",
                    filterSetDescriptor.getTitle(), filterSetDescriptor.getGuid(), av.getVersionId());
        } else {
            LOG.debug("No SSC filter set resolved from options while counting auditable issues for version {}",
                    av.getVersionId());
        }
        return filterSetDescriptor;
    }

    private static String resolveFolderFilter(AviatorLoggerImpl logger, SSCAppVersionDescriptor av, boolean noFilterSet,
            SSCIssueFilterSetDescriptor filterSetDescriptor, List<String> folderNames) {
        if (folderNames == null || folderNames.isEmpty()) {
            return null;
        }
        String folderFilter = getFolderFilter(noFilterSet, filterSetDescriptor, folderNames);
        logger.progress("Status: Applying folder filter for: %s", String.join(", ", folderNames));
        LOG.debug("Applied folder filter '{}' for folders {} while counting auditable issues for version {}",
                folderFilter, folderNames, av.getVersionId());
        return folderFilter;
    }

    private static long countEligibleIssues(AuditableIssueCountQuery query, AviatorLoggerImpl logger, boolean stopAfterFirst) {
        try {
            long totalAuditableCount = 0;
            long start = 0;
            long totalFromServer = -1;
            do {
                JsonNode response = fetchAuditableIssuesPage(query, start);
                long reportedCount = getReportedIssueCount(response);
                if (reportedCount < 0) {
                    return issueCountUnavailable(logger, "invalid response from the issue check");
                }
                if (totalFromServer == -1) {
                    totalFromServer = reportedCount;
                    LOG.debug("SSC reported {} total issues matching the initial auditable count query for version {}",
                            totalFromServer, query.av().getVersionId());
                } else if (reportedCount != totalFromServer) {
                    return issueCountUnavailable(logger, "reported count changed during pagination");
                }
                ArrayNode issues = (ArrayNode) response.get("data");
                if (start > totalFromServer || issues.size() > totalFromServer - start) {
                    return issueCountUnavailable(logger, "page size disagrees with the reported count");
                }
                if (issues.isEmpty() && start < totalFromServer) {
                    return issueCountUnavailable(logger, "pagination ended before the reported count");
                }
                if (issues.isEmpty()) {
                    LOG.debug("SSC returned no more issues for version {} at start={}; stopping pagination with cumulativeAuditableCount={} and totalFromServer={}",
                            query.av().getVersionId(), start, totalAuditableCount, totalFromServer);
                    break;
                }
                long auditableOnPage = countEligibleOnPage(issues, query.forceReaudit(), stopAfterFirst);
                if (stopAfterFirst && auditableOnPage > 0) {
                    LOG.debug("Found an auditable SSC issue for version {} at pageStart={}; stopping presence check",
                            query.av().getVersionId(), start);
                    logger.progress("Status: Found auditable issues.");
                    return 1;
                }
                if (stopAfterFirst && start == 0 && !query.forceReaudit() && query.folderFilter() == null
                        && issues.size() < totalFromServer) {
                    // Avoid scanning a large processed population just to prove
                    // absence. Failed server verification still continues pagination.
                    long serverCount = countNormalIssuesOnServer(query);
                    if (serverCount >= 0) {
                        logger.progress("Status: Found %d auditable issues.", serverCount);
                        return serverCount;
                    }
                }
                totalAuditableCount += auditableOnPage;
                LOG.debug("Processed SSC issues page for version {}: pageStart={}, pageSize={}, auditableOnPage={}, cumulativeAuditableCount={}, totalFromServer={}",
                        query.av().getVersionId(), start, issues.size(), auditableOnPage, totalAuditableCount, totalFromServer);
                start += issues.size();
            } while (start < totalFromServer);

            LOG.debug("Completed auditable issue count for version {}: totalAuditableCount={}, totalFromServer={}",
                    query.av().getVersionId(), totalAuditableCount, totalFromServer);
            logger.progress("Status: Found %d auditable issues.", totalAuditableCount);
            return totalAuditableCount;
        } catch (UnexpectedHttpResponseException e) {
            LOG.debug("Failed to retrieve SSC issue count (HTTP {})", e.getStatus(), e);
            return issueCountUnavailable(logger, String.format("HTTP %d from the issue check", e.getStatus()));
        }
    }

    private static JsonNode fetchAuditableIssuesPage(AuditableIssueCountQuery query, long start) {
        LOG.debug("Requesting SSC issues page for version {} with start={} and limit={}",
                query.av().getVersionId(), start, PAGE_LIMIT);
        return createAuditableIssuesRequest(query)
                .queryString("limit", PAGE_LIMIT)
                .queryString("embed", "auditValues")
                .queryString("start", start).asObject(JsonNode.class).getBody();
    }

    private static GetRequest createAuditableIssuesRequest(AuditableIssueCountQuery query) {
        GetRequest request = createScopedIssuesRequest(query);
        // Normal audit only needs unaudited issues. Force re-audit must also
        // retrieve Aviator-processed issues that SSC may already mark audited.
        if (!query.forceReaudit()) {
            request.queryString("q", "audited:false");
        }
        return request;
    }

    private static GetRequest createScopedIssuesRequest(AuditableIssueCountQuery query) {
        GetRequest request = query.unirest().get(SSCUrls.PROJECT_VERSION_ISSUES(query.av().getVersionId()))
                .queryString("qm", "issues");
        if (query.noFilterSet()) {
            request.queryString("showhidden", "true");
        }
        if (query.filterSetGuid() != null) {
            request.queryString("filterset", query.filterSetGuid());
        }
        if (query.folderFilter() != null) {
            request.queryString("filter", query.folderFilter());
        }
        return request;
    }

    private static long countEligibleOnPage(ArrayNode issues, boolean forceReaudit, boolean stopAfterFirst) {
        var eligibleIssues = JsonHelper.stream(issues).filter(issue -> isEligibleForRequestedAudit(issue, forceReaudit));
        return stopAfterFirst ? (eligibleIssues.findFirst().isPresent() ? 1 : 0) : eligibleIssues.count();
    }

    /**
     * Force re-audit includes previously processed Aviator issues and legacy
     * Analysis-tag candidates even when SSC already marks them audited. Normal audit
     * still excludes processed issues.
     */
    private static boolean isEligibleForRequestedAudit(JsonNode issue, boolean forceReaudit) {
        if (issue.path("suppressed").asBoolean(false)) {
            return false;
        }
        if (isProcessedByAviator(issue)) {
            return forceReaudit;
        }
        if (!issue.path("audited").asBoolean(false)) {
            return true;
        }
        return forceReaudit && hasAnalysisTag(issue);
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
     * Legacy Aviator versions wrote audit results to the Analysis tag without the
     * explicit Aviator status tag. This only identifies an FPR download candidate;
     * TagHistory is required to determine who wrote the result.
     */
    private static boolean hasAnalysisTag(JsonNode issue) {
        JsonNode auditValues = issue.path("_embed").path("auditValues");
        if (!auditValues.isArray()) {
            return false;
        }
        return JsonHelper.stream((ArrayNode) auditValues)
                .anyMatch(tagValue -> Constants.ANALYSIS_TAG_ID.equalsIgnoreCase(
                        tagValue.path("customTagGuid").asText()));
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
                Constants.DEFAULT_PING_INTERVAL_SECONDS, AviatorRequestPolicy.WAIT_FOR_READY)) {
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
            logger.progress("WARN: Aviator quota unavailable for app '%s'.", appName);
            return QUOTA_UNKNOWN;
        } catch (Exception e) {
            LOG.warn("Failed to retrieve quota for application '{}': {}", appName, e.getMessage());
            logger.progress("WARN: Aviator quota unavailable for app '%s'.", appName);
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
                Constants.DEFAULT_PING_INTERVAL_SECONDS, AviatorRequestPolicy.WAIT_FOR_READY)) {
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
     * Returns the top N SAST categories for the requested audit mode.
     * Normal audit ranks by unaudited issues with Aviator status not set.
     * Force re-audit ranks by visible issues, including Aviator-processed ones.
     * Each entry contains "categoryName" and "unauditedCount" (eligible count for that mode).
     */
    public static List<Map<String, Object>> getTopUnauditedCategories(
            UnirestInstance unirest, SSCAppVersionDescriptor av,
            AviatorLoggerImpl logger, int topN, boolean forceReaudit) {

        try {
            return getTopUnauditedCategoriesInternal(unirest, av, logger, topN, forceReaudit);
        } catch (Exception e) {
            LOG.warn("Failed to retrieve top unaudited categories for {}:{}: {}",
                av.getApplicationName(), av.getVersionName(), e.getMessage());
            logger.progress("WARN: Could not retrieve category breakdown from SSC.");
            return List.of();
        }
    }

    private static List<Map<String, Object>> getTopUnauditedCategoriesInternal(
            UnirestInstance unirest, SSCAppVersionDescriptor av,
            AviatorLoggerImpl logger, int topN, boolean forceReaudit) {

        String versionId = av.getVersionId();

        // Resolve "Category" grouping GUID dynamically
        SSCIssueGroupHelper groupHelper = new SSCIssueGroupHelper(unirest, versionId);
        SSCIssueGroupDescriptor categoryDescriptor = groupHelper.getDescriptorByDisplayNameOrId("Category", true);
        String categoryGroupGuid = categoryDescriptor.getGuid();

        // Resolve "Aviator status:Not Set" filter dynamically
        String aviatorStatusFilter = null;
        if (!forceReaudit) {
            try {
                SSCIssueFilterHelper filterHelper = new SSCIssueFilterHelper(unirest, versionId);
                aviatorStatusFilter = filterHelper.getFilter("Aviator status:Not Set");
            } catch (FcliSimpleException e) {
                // Tag doesn't exist on this version — all issues are unprocessed
                LOG.debug("Aviator status tag not found for version {}. All issues considered unprocessed.", versionId);
            }
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

        // Rank categories by issues eligible for the requested audit mode.
        List<Map<String, Object>> categories = new ArrayList<>();
        if (groups != null) {
            for (JsonNode group : groups) {
                String name = group.path("id").asText("Unknown");
                long visibleCount = group.path("visibleCount").asLong(0);
                long auditedCount = group.path("auditedCount").asLong(0);
                // Force re-audit sends Aviator-processed (already audited) issues, so
                // visible count is the quota-relevant size for the category.
                long eligibleCount = forceReaudit ? visibleCount : visibleCount - auditedCount;
                if (eligibleCount > 0) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("categoryName", name);
                    entry.put("totalIssues", visibleCount);
                    entry.put("auditedIssues", auditedCount);
                    entry.put("unauditedCount", eligibleCount);
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
