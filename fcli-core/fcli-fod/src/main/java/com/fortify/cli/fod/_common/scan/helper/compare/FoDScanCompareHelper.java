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
package com.fortify.cli.fod._common.scan.helper.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDPagingHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanStatus;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionContext;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionEngine;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionMetric;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionResult;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionTransition;
import com.fortify.cli.fod.issue.helper.FoDIssueHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;

/** Scan type-independent functionality for comparing the latest scan against the previous scan of the same type */
public final class FoDScanCompareHelper {
    static final long BYTES_PER_MB = 1024L * 1024L;
    private static final String ISSUE_FIELDS = "id,vulnId,instanceId,severity,severityString,category,"
            + "primaryLocation,primaryLocationFull,lineNumber,status,scanId,closedStatus,introducedDate";
    // Highest severity first; category and id provide a stable order for truncated lists
    static final Comparator<JsonNode> ISSUE_ORDER = Comparator
            .comparingInt((JsonNode i) -> i.path("severity").asInt()).reversed()
            .thenComparing(i -> StringUtils.defaultString(getText(i, "category")))
            .thenComparing(i -> StringUtils.defaultString(getText(i, "id")));

    private FoDScanCompareHelper() {}

    /**
     * Compare the most recent completed scan of the given release against the previous completed scan of the same
     * scan type and assessment type, returning the full compare result.
     */
    public static ObjectNode compare(UnirestInstance unirest, FoDReleaseDescriptor release, IFoDScanCompareSupport support,
            FoDScanCompareOptions options) {
        var releaseId = release.getReleaseId();
        var pair = getScanPair(unirest, releaseId, support, options.ignoreCanceled());
        var comparison = new Comparison(pair, loadIssues(unirest, releaseId, support, pair), support.loadChanges(unirest, pair));
        var attributionResult = options.attribution()
                ? attributeIssues(unirest, releaseId, support, comparison, options.changeThresholdPercent())
                : null;
        var issues = comparison.issues();
        var changes = comparison.changes();
        var result = createResult(release, pair);
        var changedSettings = getChangedSettingNames(pair, support.getSettingFields());
        result.put("summary", new FoDScanCompareSummary(issues, changedSettings, changes, attributionResult).getText());
        issues.addTo(result, options.maxIssues());
        addSettingChanges(result, pair, support.getSettingFields());
        addStatistics(result, pair, support.getStatisticFields());
        result.put("changeDetailsLimited", options.limitChangeDetails());
        changes.addTo(result, getChangeFilter(options.limitChangeDetails(), attributionResult));
        if ( attributionResult != null ) {
            attributionResult.addTo(result);
        } else {
            result.put("attributionEnabled", false);
        }
        return result;
    }

    // With limitChangeDetails, only changes referenced by issue attributions are listed
    static Predicate<String> getChangeFilter(boolean limitChangeDetails, FoDAttributionResult attributionResult) {
        if ( !limitChangeDetails ) { return change -> true; }
        if ( attributionResult == null ) { return change -> false; }
        var referenced = attributionResult.getReferencedChanges();
        return referenced::contains;
    }

    /** @throws FcliSimpleException if the given option value is less than the given minimum */
    static int requireMinimum(String optionName, int value, int minimum) {
        if ( value < minimum ) {
            throw new FcliSimpleException(String.format("Option %s must be at least %d", optionName, minimum));
        }
        return value;
    }

    /** @throws FcliSimpleException if the given option value is outside the given range */
    static int requireRange(String optionName, int value, int minimum, int maximum) {
        if ( value < minimum || value > maximum ) {
            throw new FcliSimpleException(String.format("Option %s must be between %d and %d", optionName, minimum, maximum));
        }
        return value;
    }

    private static FoDScanComparePair getScanPair(UnirestInstance unirest, String releaseId, IFoDScanCompareSupport support,
            boolean ignoreCanceled) {
        var selection = selectScans(getCandidateScans(unirest, releaseId, support), support, ignoreCanceled);
        var targetScanId = getText(selection.target(), "scanId");
        var previousScanId = getText(selection.previous(), "scanId");
        return new FoDScanComparePair(
                targetScanId, getScanSummary(unirest, targetScanId),
                previousScanId, getScanSummary(unirest, previousScanId));
    }

    /**
     * Select the most recent completed scan and the previous completed scan of the same assessment type
     * from the given scans, which must be sorted by descending start date. As issue deltas are based on
     * current issue states, this fails if any other scan that may have affected issue states was completed
     * in between, or if the most recent completed scan is an imported scan.
     */
    static FoDScanCompareSelection selectScans(List<JsonNode> scans, IFoDScanCompareSupport support, boolean ignoreCanceled) {
        var scanTypeName = support.getScanType().name();
        int targetIdx = IntStream.range(0, scans.size()).filter(i -> isCompleted(scans.get(i))).findFirst()
                .orElseThrow(() -> new FcliSimpleException(String.format("No completed %s scans found on release", scanTypeName)));
        var target = scans.get(targetIdx);
        var targetScanId = getText(target, "scanId");
        var assessmentTypeId = getText(target, "assessmentTypeId");
        var assessmentTypeName = getText(target, "assessmentTypeName");
        if ( isImported(target) ) {
            throw new FcliSimpleException(String.format(
                    "Most recent completed %s scan %s is an imported '%s' scan, which cannot be compared; "
                    + "run a new %s scan to compare against the previous scan",
                    scanTypeName, targetScanId, assessmentTypeName, scanTypeName));
        }
        for ( int i = 0; i < targetIdx; i++ ) {
            var scan = scans.get(i);
            if ( !ignoreCanceled && isCanceled(scan) && isSameAssessmentType(scan, assessmentTypeId) ) {
                throw new FcliSimpleException(String.format(
                        "%s scan %s with assessment type '%s' was canceled after the most recent completed scan %s; "
                        + "use --ignore-canceled to compare anyway",
                        scanTypeName, getText(scan, "scanId"), assessmentTypeName, targetScanId));
            }
        }
        for ( int i = targetIdx + 1; i < scans.size(); i++ ) {
            var scan = scans.get(i);
            var sameAssessmentType = isSameAssessmentType(scan, assessmentTypeId);
            if ( isCompleted(scan) ) {
                if ( sameAssessmentType ) { return new FoDScanCompareSelection(target, scan); }
                throw new FcliSimpleException(String.format(
                        "Completed %s scan %s with assessment type '%s' was run between scan %s and the previous completed '%s' scan, "
                        + "so issue changes cannot be reliably attributed to scan %s; run a new '%s' scan to obtain comparable scans",
                        scanTypeName, getText(scan, "scanId"), getText(scan, "assessmentTypeName"), targetScanId,
                        assessmentTypeName, targetScanId, assessmentTypeName));
            }
            if ( !ignoreCanceled && isCanceled(scan) && sameAssessmentType ) {
                throw new FcliSimpleException(String.format(
                        "%s scan %s with assessment type '%s' was canceled between scan %s and the previous completed scan; "
                        + "use --ignore-canceled to compare anyway",
                        scanTypeName, getText(scan, "scanId"), assessmentTypeName, targetScanId));
            }
        }
        throw new FcliSimpleException(String.format(
                "No previous completed %s scan with assessment type '%s' found to compare against scan %s",
                scanTypeName, assessmentTypeName, targetScanId));
    }

    private static ObjectNode createResult(FoDReleaseDescriptor release, FoDScanComparePair pair) {
        var target = pair.target();
        var previous = pair.previous();
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("applicationName", release.getApplicationName())
                .put("applicationId", release.getApplicationId())
                .put("microserviceName", release.getMicroserviceName())
                .put("releaseName", release.getReleaseName())
                .put("releaseId", release.getReleaseId())
                .put("scanType", getText(target, "scanType"))
                .put("assessmentType", getText(target, "assessmentTypeName"))
                .put("scanId", pair.targetScanId())
                .put("scanStartedDateTime", getText(target, "startedDateTime"))
                .put("scanCompletedDateTime", getText(target, "completedDateTime"))
                .put("previousScanId", pair.previousScanId())
                .put("previousScanStartedDateTime", getText(previous, "startedDateTime"))
                .put("previousScanCompletedDateTime", getText(previous, "completedDateTime"));
    }

    /**
     * Load all new and reopened issues found in the target scan, and all issues that were found in the previous
     * scan but fixed in the target scan. All issues are retained, such that issue attribution and attribution
     * summaries cover all issues; issue lists are only limited when adding them to the compare result.
     */
    private static FoDScanCompareIssues loadIssues(UnirestInstance unirest, String releaseId, IFoDScanCompareSupport support,
            FoDScanComparePair pair) {
        var scanTypeName = support.getScanType().name();
        var fields = getIssueFields(support);
        var targetScanId = pair.targetScanId();
        var newIssues = new ArrayList<ObjectNode>();
        var reopenedIssues = new ArrayList<ObjectNode>();
        processIssues(unirest, releaseId, false, fields,
                String.format("scantype:%s+status:New|Reopen+scanId:%s", scanTypeName, targetScanId), issue -> {
                    var transition = getTargetScanTransition(issue, targetScanId);
                    if ( transition == FoDAttributionTransition.NEW ) { newIssues.add(issue); }
                    else if ( transition == FoDAttributionTransition.REOPENED ) { reopenedIssues.add(issue); }
                });
        var previousScanId = pair.previousScanId();
        var fixedIssues = new ArrayList<ObjectNode>();
        processIssues(unirest, releaseId, true, fields,
                String.format("scantype:%s+scanId:%s", scanTypeName, previousScanId), issue -> {
                    if ( isFixedAfterScan(issue, previousScanId) ) { fixedIssues.add(issue); }
                });
        return new FoDScanCompareIssues(newIssues, reopenedIssues, fixedIssues);
    }

    /**
     * Issues are also checked client-side, rather than relying on server-side filters only.
     *
     * @return NEW or REOPENED if the given issue was newly found or reopened in the given target scan, null otherwise
     */
    static FoDAttributionTransition getTargetScanTransition(JsonNode issue, String targetScanId) {
        if ( !targetScanId.equals(getText(issue, "scanId")) ) { return null; }
        if ( hasStatus(issue, "New") ) { return FoDAttributionTransition.NEW; }
        return hasStatus(issue, "Reopen") ? FoDAttributionTransition.REOPENED : null;
    }

    /**
     * For fixed issues, scanId refers to the last scan in which the issue was found
     *
     * @return Whether the given issue is fixed, and was last found in the given (previous) scan
     */
    static boolean isFixedAfterScan(JsonNode issue, String previousScanId) {
        return FoDIssueHelper.isFixed(issue) && previousScanId.equals(getText(issue, "scanId"));
    }

    private static String getIssueFields(IFoDScanCompareSupport support) {
        var additionalFields = support.getAdditionalIssueFields();
        return additionalFields.isEmpty() ? ISSUE_FIELDS : ISSUE_FIELDS + "," + String.join(",", additionalFields);
    }

    /**
     * Attribute all issue status transitions to scan-level conditions and scan type-specific changes,
     * adding an 'attribution' property to each issue.
     *
     * @param changeThresholdPercent Minimum relative change of scan metrics to be considered significant
     */
    private static FoDAttributionResult attributeIssues(UnirestInstance unirest, String releaseId, IFoDScanCompareSupport support,
            Comparison comparison, int changeThresholdPercent) {
        var pair = comparison.pair();
        var changes = comparison.changes();
        var metrics = new LinkedHashMap<String, FoDAttributionMetric>();
        for ( var field : support.getStatisticFields() ) {
            metrics.put(field.name(), FoDAttributionMetric.of(getValue(pair.previous(), field.jsonPointer()),
                    getValue(pair.target(), field.jsonPointer())));
        }
        metrics.putAll(changes.getMetrics());
        var context = FoDAttributionContext.builder()
                .previousSummary(pair.previous())
                .currentSummary(pair.target())
                .changeThresholdPercent(changeThresholdPercent)
                .metrics(metrics)
                .changeIndex(changes.getChangeIndex())
                .issues(comparison.issues().toAttributionIssues())
                .requestLookup(support.isIssueRequestLookupSupported() ? new FoDScanCompareIssueRequestLookup(unirest, releaseId) : null)
                .traceLookup(support.isIssueTraceLookupSupported() ? new FoDScanCompareIssueTraceLookup(unirest, releaseId) : null)
                .build();
        return new FoDAttributionEngine(support.getConditionDetectors(), support.getIssueMatchers()).attribute(context);
    }

    /** Add changed scan settings as 'settingChanges', together with 'settingChangeCount' */
    private static void addSettingChanges(ObjectNode result, FoDScanComparePair pair, List<FoDScanCompareField> fields) {
        var changes = JsonHelper.getObjectMapper().createArrayNode();
        for ( var field : getChangedSettings(pair, fields) ) {
            addPreviousAndCurrent(changes, field.name(),
                    getValue(pair.previous(), field.jsonPointer()), getValue(pair.target(), field.jsonPointer()));
        }
        result.put("settingChangeCount", changes.size());
        result.set("settingChanges", changes);
    }

    /** @return Names of scan settings that differ between both scans */
    private static List<String> getChangedSettingNames(FoDScanComparePair pair, List<FoDScanCompareField> fields) {
        return getChangedSettings(pair, fields).stream().map(FoDScanCompareField::name).toList();
    }

    private static List<FoDScanCompareField> getChangedSettings(FoDScanComparePair pair, List<FoDScanCompareField> fields) {
        return fields.stream()
                .filter(f -> !getValue(pair.previous(), f.jsonPointer()).equals(getValue(pair.target(), f.jsonPointer())))
                .toList();
    }

    /** Add previous and current values for all given scan statistics as 'scanStatistics' */
    private static void addStatistics(ObjectNode result, FoDScanComparePair pair, List<FoDScanCompareField> fields) {
        var statistics = JsonHelper.getObjectMapper().createArrayNode();
        for ( var field : fields ) {
            addPreviousAndCurrent(statistics, field.name(),
                    getValue(pair.previous(), field.jsonPointer()), getValue(pair.target(), field.jsonPointer()));
        }
        result.set("scanStatistics", statistics);
    }

    /**
     * Whether the given exception indicates that optional scan data, like a scan manifest or site tree,
     * is not available or not accessible for a scan, in which case this data can be skipped
     */
    static boolean isUnavailable(UnexpectedHttpResponseException e) {
        var status = e.getStatus();
        return status == 400 || status == 403 || status == 404 || status == 422;
    }

    /**
     * Add sorted values as '{name}s', limited to the given maximum number of values and to values matching the
     * given predicate, together with '{name}Count' (reflecting all values) and '{name}sTruncated'
     */
    static void addValueList(ObjectNode result, String name, Collection<String> values, int maxItems, Predicate<String> include) {
        var list = new FoDScanCompareLimitedList<String>(maxItems, Comparator.naturalOrder());
        values.forEach(v -> list.add(v, include.test(v)));
        list.addTo(result, name, TextNode::valueOf);
    }

    static String getText(JsonNode node, String fieldName) {
        var value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Load scans of the configured scan type, sorted by descending start date. Paging stops once two
     * completed scans have been found, as {@link #selectScans(List, IFoDScanCompareSupport, boolean)}
     * never needs to look beyond the second completed scan.
     */
    private static List<JsonNode> getCandidateScans(UnirestInstance unirest, String releaseId, IFoDScanCompareSupport support) {
        var scanTypeName = support.getScanType().name();
        var ignoredAssessmentTypes = support.getIgnoredAssessmentTypes();
        var request = FoDScanHelper.addDefaultScanListParams(
                unirest.get(FoDUrls.RELEASE_SCANS).routeParam("relId", releaseId));
        var scans = new ArrayList<JsonNode>();
        var completedCount = new AtomicInteger();
        FoDPagingHelper.processItems(unirest, request, scan -> {
            if ( scanTypeName.equals(getText(scan, "scanType"))
                    && StringUtils.isNotBlank(getText(scan, "scanId"))
                    && !ignoredAssessmentTypes.contains(StringUtils.lowerCase(getText(scan, "assessmentTypeName"))) ) {
                scans.add(scan);
                if ( isCompleted(scan) && completedCount.incrementAndGet() >= 2 ) { return Break.TRUE; }
            }
            return Break.FALSE;
        });
        return scans;
    }

    private static JsonNode getScanSummary(UnirestInstance unirest, String scanId) {
        var summary = unirest.get(FoDUrls.SCAN_SUMMARY).routeParam("scanId", scanId).asObject(ObjectNode.class).getBody();
        if ( summary == null ) {
            throw new FcliSimpleException(String.format("Unable to load scan summary for scan %s", scanId));
        }
        return summary;
    }

    private static void processIssues(UnirestInstance unirest, String releaseId, boolean includeFixed, String fields,
            String filters, Consumer<ObjectNode> consumer) {
        var request = unirest.get(FoDUrls.VULNERABILITIES)
                .routeParam("relId", releaseId)
                .queryString("excludeFilters", "true")
                .queryString("includeSuppressed", "false")
                .queryString("includeFixed", Boolean.toString(includeFixed))
                .queryString("fields", fields)
                .queryString("filters", filters);
        FoDPagingHelper.processItems(unirest, request, issue -> {
            if ( issue instanceof ObjectNode objectNode ) { consumer.accept(objectNode); }
            return Break.FALSE;
        });
    }

    private static boolean isImported(JsonNode scan) {
        return StringUtils.lowerCase(StringUtils.defaultString(getText(scan, "assessmentTypeName"))).contains("(imported)");
    }

    private static void addPreviousAndCurrent(ArrayNode array, String name, JsonNode previousValue, JsonNode currentValue) {
        var entry = array.addObject().put("name", name);
        entry.set("previousValue", previousValue);
        entry.set("currentValue", currentValue);
    }

    private static boolean isSameAssessmentType(JsonNode scan, String assessmentTypeId) {
        return Objects.equals(assessmentTypeId, getText(scan, "assessmentTypeId"));
    }

    private static boolean hasStatus(JsonNode issue, String status) {
        return status.equalsIgnoreCase(getText(issue, "status"));
    }

    private static boolean isCompleted(JsonNode scan) {
        return FoDScanStatus.Completed.name().equalsIgnoreCase(getText(scan, "analysisStatusType"));
    }

    private static boolean isCanceled(JsonNode scan) {
        return FoDScanStatus.Canceled.name().equalsIgnoreCase(getText(scan, "analysisStatusType"));
    }

    private static JsonNode getValue(JsonNode summary, String jsonPointer) {
        var value = summary.at(jsonPointer);
        return value.isMissingNode() ? NullNode.getInstance() : value;
    }

    /** Scan pair together with the issue and scan type-specific changes between both scans */
    private record Comparison(FoDScanComparePair pair, FoDScanCompareIssues issues, IFoDScanCompareChanges changes) {}
}
