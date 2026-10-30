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
    private static final Comparator<SiteTreeUrl> URL_ORDER = Comparator.comparing(SiteTreeUrl::url);
    private final int maxUrlChanges;
    private final long maxSiteTreeBytes;

    /**
     * @param maxUrlChanges Maximum number of URLs to list for each of added, changed and removed URLs
     * @param maxSiteTreeSizeMb Maximum site tree size in MB; site tree comparison is skipped for larger site trees
     */
    public FoDScanCompareDastSupport(int maxUrlChanges, int maxSiteTreeSizeMb) {
        this.maxUrlChanges = maxUrlChanges;
        this.maxSiteTreeBytes = maxSiteTreeSizeMb * FoDScanCompareHelper.BYTES_PER_MB;
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
    public void addScanTypeDeltas(UnirestInstance unirest, FoDScanComparePair pair, ObjectNode result) {
        var current = loadSiteTree(unirest, pair.targetScanId());
        var previous = loadSiteTree(unirest, pair.previousScanId());
        result.put("currentSiteTreeAvailable", current.available());
        result.put("previousSiteTreeAvailable", previous.available());
        var skipReason = StringUtils.firstNonBlank(
                getSkipReason(current, pair.targetScanId()), getSkipReason(previous, pair.previousScanId()));
        result.put("siteTreeComparisonSkipped", skipReason != null);
        result.put("siteTreeComparisonSkippedReason", skipReason);
        var currentUrls = current.isComplete() ? getUrls(current.entries()) : null;
        var previousUrls = previous.isComplete() ? getUrls(previous.entries()) : null;
        addSiteTreeStatistics(result, getStatistics(previous.entries(), previousUrls), getStatistics(current.entries(), currentUrls));
        if ( skipReason == null ) {
            addUrlDeltas(result, previousUrls, currentUrls);
        }
    }

    private String getSkipReason(SiteTree siteTree, String scanId) {
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
        return new SiteTree(true, false, entries);
    }

    private record SiteTree(boolean available, boolean tooLarge, List<JsonNode> entries) {
        static final SiteTree UNAVAILABLE = new SiteTree(false, false, null);
        static final SiteTree TOO_LARGE = new SiteTree(true, true, null);

        boolean isComplete() {
            return available && !tooLarge;
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

    private void addUrlDeltas(ObjectNode result, Map<String, SiteTreeUrl> previous, Map<String, SiteTreeUrl> current) {
        var added = new FoDScanCompareLimitedList<SiteTreeUrl>(maxUrlChanges, URL_ORDER);
        var removed = new FoDScanCompareLimitedList<SiteTreeUrl>(maxUrlChanges, URL_ORDER);
        var changed = new FoDScanCompareLimitedList<UrlChange>(maxUrlChanges, Comparator.comparing(c -> c.current().url()));
        current.values().stream().filter(u -> !previous.containsKey(u.url())).forEach(added::add);
        previous.values().stream().filter(u -> !current.containsKey(u.url())).forEach(removed::add);
        current.values().stream().filter(u -> previous.containsKey(u.url())).forEach(u -> {
            var p = previous.get(u.url());
            if ( !p.methods().equals(u.methods()) || !p.statusCodes().equals(u.statusCodes()) ) {
                changed.add(new UrlChange(p, u));
            }
        });
        added.addTo(result, "addedUrl", SiteTreeUrl::toJson);
        removed.addTo(result, "removedUrl", SiteTreeUrl::toJson);
        changed.addTo(result, "changedUrl", UrlChange::toJson);
    }

    private record UrlChange(SiteTreeUrl previous, SiteTreeUrl current) {
        JsonNode toJson() {
            var change = JsonHelper.getObjectMapper().createObjectNode().put("url", current.url());
            change.set("previousMethods", toArray(previous.methods()));
            change.set("currentMethods", toArray(current.methods()));
            change.set("previousStatusCodes", toArray(previous.statusCodes()));
            change.set("currentStatusCodes", toArray(current.statusCodes()));
            return change;
        }
    }

    private static Map<String, SiteTreeUrl> getUrls(List<JsonNode> entries) {
        return entries.stream().collect(Collectors.groupingBy(FoDScanCompareDastSupport::toUrl, TreeMap::new,
                Collectors.collectingAndThen(Collectors.toList(), SiteTreeUrl::of)));
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

    private record SiteTreeUrl(String url, Set<String> methods, Set<String> statusCodes) {
        static SiteTreeUrl of(List<JsonNode> entries) {
            return new SiteTreeUrl(toUrl(entries.get(0)),
                    collect(entries, "method"), collect(entries, "statusCode"));
        }

        private static Set<String> collect(List<JsonNode> entries, String fieldName) {
            return entries.stream().map(e -> FoDScanCompareHelper.getText(e, fieldName))
                    .filter(StringUtils::isNotBlank).collect(Collectors.toCollection(TreeSet::new));
        }

        ObjectNode toJson() {
            var node = JsonHelper.getObjectMapper().createObjectNode().put("url", url);
            node.set("methods", toArray(methods));
            node.set("statusCodes", toArray(statusCodes));
            return node;
        }
    }
}
