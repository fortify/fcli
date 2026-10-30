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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDPagingHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionBooleanSettingDetector;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionCause;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionChangedSettingDetector;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionMetric;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionMetricDetector;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionMetricDetector.MetricRule;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionRankedSettingDetector;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionTargetHostMatcher;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionTimeboxDetector;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeIndex;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeIndex.Kind;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeIndex.SiteTreeUrl;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeIndex.UrlMatch;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeMatcher;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrls;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDAttributionChangeIndex;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDIssueEvidenceMatcher;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDScanConditionDetector;

import kong.unirest.UnirestInstance;
import lombok.extern.slf4j.Slf4j;

/** DAST-specific scan comparison: scan summary settings and URL changes based on scan site trees */
@Slf4j
public final class FoDScanCompareDastSupport implements IFoDScanCompareSupport {
    private static final String DETAILS = "/dynamicScanSummaryDetails/";
    private static final List<FoDScanCompareField> SETTING_FIELDS = toFields(
            "dynamicSiteURL", "restrictToDirectoryAndSubdirectories", "allowSameHostRedirects",
            "allowFormSubmissions", "hasFormsAuthentication", "requiresNetworkAuthentication",
            "webServiceType", "userAgentType", "concurrentRequestThreadsType", "policyName", "timeBoxInHours");
    private static final List<FoDScanCompareField> STATISTIC_FIELDS = toFields(
            "crawlSessions", "failedRequests", "logouts", "macroPlaybacks");
    private static final Comparator<UrlMatch> URL_ORDER = Comparator.comparing(UrlMatch::url);
    private final int maxUrlChanges;
    private final long maxSiteTreeBytes;

    /**
     * @param maxUrlChanges Maximum number of URLs to list for each of added, changed and removed URLs
     * @param maxSiteTreeSizeMb Maximum site tree size in MB; site tree comparison is skipped for larger site trees
     */
    public FoDScanCompareDastSupport(int maxUrlChanges, int maxSiteTreeSizeMb) {
        this.maxUrlChanges = FoDScanCompareHelper.requireMinimum("--max-url-changes", maxUrlChanges, 0);
        this.maxSiteTreeBytes = FoDScanCompareHelper.requireMinimum("--max-site-tree-size", maxSiteTreeSizeMb, 1)
                * FoDScanCompareHelper.BYTES_PER_MB;
    }

    @Override
    public FoDScanType getScanType() {
        return FoDScanType.Dynamic;
    }

    @Override
    public Set<String> getIgnoredAssessmentTypes() {
        return Set.of();
    }

    @Override
    public List<FoDScanCompareField> getSettingFields() {
        return SETTING_FIELDS;
    }

    @Override
    public List<FoDScanCompareField> getStatisticFields() {
        return STATISTIC_FIELDS;
    }

    @Override
    public IFoDScanCompareChanges loadChanges(UnirestInstance unirest, FoDScanComparePair pair) {
        // URL changes can't be compared without the current site tree, so avoid loading the previous site tree
        var current = loadSiteTree(unirest, pair.targetScanId());
        var previous = current.isComplete() ? loadSiteTree(unirest, pair.previousScanId()) : SiteTree.NOT_LOADED;
        return compareSiteTrees(pair.previousScanId(), previous, pair.targetScanId(), current);
    }

    /**
     * Compare the given site trees. URL changes are determined by {@link FoDAttributionUrlChangeIndex} only, such that
     * listed URL changes, change counts and issue attributions are all based on the same (normalized) URL identity.
     */
    IFoDScanCompareChanges compareSiteTrees(String previousScanId, SiteTree previous, String currentScanId, SiteTree current) {
        var skipReason = StringUtils.firstNonBlank(getSkipReason(current, currentScanId), getSkipReason(previous, previousScanId));
        var currentUrls = current.isComplete() ? getUrls(current.entries()) : null;
        var previousUrls = previous.isComplete() ? getUrls(previous.entries()) : null;
        var index = skipReason != null ? null
                : new FoDAttributionUrlChangeIndex(previousUrls.values(), currentUrls.values());
        return new DastChanges(current.available(), previous.available(), skipReason,
                getStatistics(previous.entries(), previousUrls), getStatistics(current.entries(), currentUrls),
                index, maxUrlChanges);
    }

    @Override
    public List<IFoDScanConditionDetector> getConditionDetectors() {
        return List.of(
                new FoDAttributionChangedSettingDetector(FoDAttributionCause.TARGET_CHANGE, FoDAttributionUrls::normalizeKey,
                        new FoDAttributionChangedSettingDetector.Setting("dynamicSiteURL", DETAILS + "dynamicSiteURL")),
                new FoDAttributionBooleanSettingDetector(FoDAttributionCause.SCOPE_CHANGE,
                        booleanSetting("restrictToDirectoryAndSubdirectories", false),
                        booleanSetting("allowFormSubmissions", true),
                        booleanSetting("allowSameHostRedirects", true)),
                new FoDAttributionBooleanSettingDetector(FoDAttributionCause.AUTH_CONFIG_CHANGE,
                        booleanSetting("hasFormsAuthentication", true),
                        booleanSetting("requiresNetworkAuthentication", true)),
                new FoDAttributionRankedSettingDetector(FoDAttributionCause.SCAN_POLICY_CHANGE, "policyName", DETAILS + "policyName",
                        Map.of("standard", 3, "criticalsandhighs", 2, "passivescan", 1, "passive", 1)),
                new FoDAttributionMetricDetector(FoDAttributionCause.COVERAGE_CHANGE,
                        MetricRule.size("urlCount"), MetricRule.size("requestCount"), MetricRule.counter("crawlSessions")),
                new FoDAttributionMetricDetector(FoDAttributionCause.AVAILABILITY_CHANGE,
                        MetricRule.degradationRatio("failedRequests", "crawlSessions"),
                        MetricRule.degradationRatio("failedRequestCount", "requestCount")),
                new FoDAttributionMetricDetector(FoDAttributionCause.AUTHENTICATION_CHANGE,
                        MetricRule.degradationCounter("logouts"), MetricRule.degradationCounter("macroPlaybacks")),
                new FoDAttributionTimeboxDetector(DETAILS + "timeBoxInHours", DETAILS + "elapsedTimeSpan"));
    }

    @Override
    public List<IFoDIssueEvidenceMatcher> getIssueMatchers() {
        return List.of(
                new FoDAttributionTargetHostMatcher(DETAILS + "dynamicSiteURL"),
                new FoDAttributionUrlChangeMatcher());
    }

    @Override
    public boolean isIssueRequestLookupSupported() {
        return true;
    }

    private static FoDAttributionBooleanSettingDetector.Setting booleanSetting(String name, boolean trueIsBroader) {
        return new FoDAttributionBooleanSettingDetector.Setting(name, DETAILS + name, trueIsBroader);
    }

    private record DastChanges(Boolean currentAvailable, Boolean previousAvailable, String skipReason,
            Map<String, Long> previousStatistics, Map<String, Long> currentStatistics,
            FoDAttributionUrlChangeIndex index, int maxUrlChanges) implements IFoDScanCompareChanges {
        @Override
        public void addTo(ObjectNode result, Predicate<String> includeChange) {
            result.put("currentSiteTreeAvailable", currentAvailable);
            result.put("previousSiteTreeAvailable", previousAvailable);
            result.put("siteTreeComparisonSkipped", skipReason != null);
            result.put("siteTreeComparisonSkippedReason", skipReason);
            addSiteTreeStatistics(result, previousStatistics, currentStatistics);
            if ( index == null ) { return; }
            addUrlList(result, "addedUrl", Kind.ADDED, includeChange, m -> toJson(m.current()));
            addUrlList(result, "removedUrl", Kind.REMOVED, includeChange, m -> toJson(m.previous()));
            addUrlList(result, "changedUrl", Kind.CHANGED, includeChange, FoDScanCompareDastSupport::toChangeJson);
        }

        private void addUrlList(ObjectNode result, String name, Kind kind, Predicate<String> includeChange,
                Function<UrlMatch, JsonNode> mapper) {
            var list = new FoDScanCompareLimitedList<UrlMatch>(maxUrlChanges, URL_ORDER);
            index.getMatches(kind).forEach(m -> list.add(m, includeChange.test(m.url())));
            list.addTo(result, name, mapper);
        }

        @Override
        public IFoDAttributionChangeIndex getChangeIndex() {
            return index;
        }

        @Override
        public String getSkipReason() {
            return skipReason;
        }

        @Override
        public List<FoDScanCompareChangeSet> getChangeSets() {
            if ( index == null ) { return List.of(); }
            return Stream.of(Kind.ADDED, Kind.CHANGED, Kind.REMOVED)
                    .map(kind -> new FoDScanCompareChangeSet("URL", index.getMatches(kind).stream().map(UrlMatch::url).toList()))
                    .toList();
        }

        @Override
        public Map<String, FoDAttributionMetric> getMetrics() {
            var result = new LinkedHashMap<String, FoDAttributionMetric>();
            if ( previousStatistics != null && currentStatistics != null ) {
                previousStatistics.forEach((name, value) -> result.put(name,
                        new FoDAttributionMetric(value.doubleValue(), toDouble(currentStatistics.get(name)))));
            }
            return result;
        }

        private static Double toDouble(Long value) {
            return value == null ? null : value.doubleValue();
        }
    }

    private String getSkipReason(SiteTree siteTree, String scanId) {
        if ( !siteTree.isLoaded() ) { return null; }
        if ( siteTree.tooLarge() ) {
            return String.format("Site tree for scan %s exceeds maximum size of %d MB; use --max-site-tree-size to increase this limit",
                    scanId, maxSiteTreeBytes / FoDScanCompareHelper.BYTES_PER_MB);
        }
        return siteTree.available() ? null : String.format("Site tree not available for scan %s", scanId);
    }

    // Site trees are not available for some DAST Automated scans. Paging stops once the
    // (approximate, based on serialized JSON) site tree size exceeds maxSiteTreeBytes.
    private SiteTree loadSiteTree(UnirestInstance unirest, String scanId) {
        var request = unirest.get(FoDUrls.SCAN_SITE_TREE).routeParam("scanId", scanId);
        var entries = new ArrayList<JsonNode>();
        var size = new AtomicLong();
        try {
            FoDPagingHelper.processItems(unirest, request, entry -> {
                if ( size.addAndGet(entry.toString().length()) > maxSiteTreeBytes ) { return Break.TRUE; }
                entries.add(entry);
                return Break.FALSE;
            });
        } catch ( UnexpectedHttpResponseException e ) {
            if ( !FoDScanCompareHelper.isUnavailable(e) ) { throw e; }
            log.debug("No site tree available for scan {} (HTTP status {})", scanId, e.getStatus());
            return SiteTree.UNAVAILABLE;
        }
        if ( size.get() > maxSiteTreeBytes ) {
            log.debug("Site tree for scan {} exceeds {} bytes", scanId, maxSiteTreeBytes);
            return SiteTree.TOO_LARGE;
        }
        return SiteTree.of(entries);
    }

    /**
     * @param available Whether the site tree is available; null if the site tree wasn't loaded
     * @param entries Site tree entries; null if the site tree is unavailable, too large or not loaded
     */
    record SiteTree(Boolean available, boolean tooLarge, List<JsonNode> entries) {
        static final SiteTree UNAVAILABLE = new SiteTree(false, false, null);
        static final SiteTree TOO_LARGE = new SiteTree(true, true, null);
        static final SiteTree NOT_LOADED = new SiteTree(null, false, null);

        static SiteTree of(List<JsonNode> entries) {
            return new SiteTree(true, false, entries);
        }

        boolean isLoaded() {
            return available != null;
        }

        boolean isComplete() {
            return Boolean.TRUE.equals(available) && !tooLarge;
        }
    }

    private static List<FoDScanCompareField> toFields(String... names) {
        return Stream.of(names).map(name -> new FoDScanCompareField(name, DETAILS + name)).toList();
    }

    private static void addSiteTreeStatistics(ObjectNode result, Map<String, Long> previous, Map<String, Long> current) {
        var statistics = JsonHelper.getObjectMapper().createArrayNode();
        var names = previous != null ? previous.keySet() : current != null ? current.keySet() : Set.<String>of();
        for ( var name : names ) {
            statistics.addObject().put("name", name)
                    .put("previousValue", previous == null ? null : previous.get(name))
                    .put("currentValue", current == null ? null : current.get(name));
        }
        result.set("siteTreeStatistics", statistics);
    }

    private static Map<String, Long> getStatistics(List<JsonNode> entries, Map<String, SiteTreeUrl> urls) {
        if ( entries == null || urls == null ) { return null; }
        var responseTimes = entries.stream().mapToLong(e -> getLong(e, "responseTimeDelta")).filter(t -> t >= 0).toArray();
        var result = new LinkedHashMap<String, Long>();
        result.put("requestCount", (long)entries.size());
        result.put("urlCount", (long)urls.size());
        result.put("hostCount", entries.stream().map(e -> FoDScanCompareHelper.getText(e, "host")).distinct().count());
        result.put("successResponseCount", countStatus(entries, 200, 299));
        result.put("redirectResponseCount", countStatus(entries, 300, 399));
        result.put("clientErrorResponseCount", countStatus(entries, 400, 499));
        result.put("serverErrorResponseCount", countStatus(entries, 500, 599));
        result.put("failedRequestCount", countStatus(entries, 0, 0));
        result.put("averageResponseTimeMs", Math.round(LongStream.of(responseTimes).average().orElse(0)));
        return result;
    }

    private static long countStatus(List<JsonNode> entries, int min, int max) {
        return entries.stream().mapToLong(e -> getLong(e, "statusCode")).filter(s -> s >= min && s <= max).count();
    }

    private static JsonNode toChangeJson(UrlMatch match) {
        var change = JsonHelper.getObjectMapper().createObjectNode().put("url", match.url());
        change.set("previousMethods", toArray(match.previous().methods()));
        change.set("currentMethods", toArray(match.current().methods()));
        change.set("previousStatusCodes", toArray(match.previous().statusCodes()));
        change.set("currentStatusCodes", toArray(match.current().statusCodes()));
        return change;
    }

    // Grouped by normalized URL, consistent with how FoDAttributionUrlChangeIndex matches URLs
    private static Map<String, SiteTreeUrl> getUrls(List<JsonNode> entries) {
        return entries.stream().collect(Collectors.groupingBy(e -> FoDAttributionUrls.normalizeKey(toUrl(e)), TreeMap::new,
                Collectors.collectingAndThen(Collectors.toList(), FoDScanCompareDastSupport::toSiteTreeUrl)));
    }

    // Omits default ports for readability
    private static String toUrl(JsonNode entry) {
        var scheme = StringUtils.defaultString(FoDScanCompareHelper.getText(entry, "scheme"));
        var port = StringUtils.defaultString(FoDScanCompareHelper.getText(entry, "port"));
        var isDefaultPort = port.isBlank() || ("http".equalsIgnoreCase(scheme) && "80".equals(port))
                || ("https".equalsIgnoreCase(scheme) && "443".equals(port));
        return String.format("%s://%s%s%s", scheme,
                StringUtils.defaultString(FoDScanCompareHelper.getText(entry, "host")),
                isDefaultPort ? "" : ":" + port,
                StringUtils.defaultString(FoDScanCompareHelper.getText(entry, "path")));
    }

    private static long getLong(JsonNode entry, String fieldName) {
        return NumberUtils.toLong(FoDScanCompareHelper.getText(entry, fieldName), -1);
    }

    private static ArrayNode toArray(Collection<String> values) {
        var array = JsonHelper.getObjectMapper().createArrayNode();
        values.forEach(array::add);
        return array;
    }

    private static SiteTreeUrl toSiteTreeUrl(List<JsonNode> entries) {
        return new SiteTreeUrl(toUrl(entries.get(0)), collect(entries, "method"), collect(entries, "statusCode"));
    }

    private static Set<String> collect(List<JsonNode> entries, String fieldName) {
        return entries.stream().map(e -> FoDScanCompareHelper.getText(e, fieldName))
                .filter(StringUtils::isNotBlank).collect(Collectors.toCollection(TreeSet::new));
    }

    private static ObjectNode toJson(SiteTreeUrl url) {
        var node = JsonHelper.getObjectMapper().createObjectNode().put("url", url.url());
        node.set("methods", toArray(url.methods()));
        node.set("statusCodes", toArray(url.statusCodes()));
        return node;
    }
}
