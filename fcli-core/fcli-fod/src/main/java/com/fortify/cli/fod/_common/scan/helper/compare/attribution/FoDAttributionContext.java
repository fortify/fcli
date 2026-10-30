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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import lombok.Builder;
import lombok.Getter;

/**
 * All data available for attributing issue status transitions: scan summaries, metrics,
 * scan type-specific changes, and all new, reopened and fixed issues.
 */
public final class FoDAttributionContext {
    @Getter private final JsonNode previousSummary;
    @Getter private final JsonNode currentSummary;
    @Getter private final int changeThresholdPercent;
    private final Map<String, FoDAttributionMetric> metrics;
    private final IFoDAttributionChangeIndex changeIndex;
    @Getter private final List<FoDAttributionIssue> issues;
    @Getter private final IFoDIssueRequestLookup requestLookup;
    @Getter private final IFoDIssueTraceLookup traceLookup;
    private final Set<String> referencedChanges = new LinkedHashSet<>();
    private final Map<Object, Object> cache = new HashMap<>();

    @Builder
    private FoDAttributionContext(JsonNode previousSummary, JsonNode currentSummary, int changeThresholdPercent,
            Map<String, FoDAttributionMetric> metrics, IFoDAttributionChangeIndex changeIndex,
            List<FoDAttributionIssue> issues, IFoDIssueRequestLookup requestLookup, IFoDIssueTraceLookup traceLookup) {
        this.previousSummary = previousSummary == null ? NullNode.getInstance() : previousSummary;
        this.currentSummary = currentSummary == null ? NullNode.getInstance() : currentSummary;
        this.changeThresholdPercent = changeThresholdPercent;
        this.metrics = metrics == null ? Map.of() : metrics;
        this.changeIndex = changeIndex;
        this.issues = issues == null ? List.of() : issues;
        this.requestLookup = requestLookup;
        this.traceLookup = traceLookup;
    }

    /** @return Metric with the given name, or null if not available */
    public FoDAttributionMetric getMetric(String name) {
        return name == null ? null : metrics.get(name);
    }

    public JsonNode getPreviousValue(String jsonPointer) {
        return previousSummary.at(jsonPointer);
    }

    public JsonNode getCurrentValue(String jsonPointer) {
        return currentSummary.at(jsonPointer);
    }

    public String getPreviousText(String jsonPointer) {
        return asText(getPreviousValue(jsonPointer));
    }

    public String getCurrentText(String jsonPointer) {
        return asText(getCurrentValue(jsonPointer));
    }

    /** @return Change index of the given type, or null if no such change index is available */
    public <T extends IFoDAttributionChangeIndex> T getChangeIndex(Class<T> type) {
        return type.isInstance(changeIndex) ? type.cast(changeIndex) : null;
    }

    public IFoDAttributionChangeIndex getChangeIndex() {
        return changeIndex;
    }

    /** Record that the given change (file path or URL, as listed in the compare output) was referenced by an attribution */
    public void referenceChange(String change) {
        if ( change != null ) { referencedChanges.add(change); }
    }

    public Set<String> getReferencedChanges() {
        return Collections.unmodifiableSet(referencedChanges);
    }

    /** Get or compute a cached value, allowing matchers to compute indexes over all issues only once */
    @SuppressWarnings("unchecked")
    public <T> T getCached(Object key, Supplier<T> supplier) {
        return (T)cache.computeIfAbsent(key, k -> supplier.get());
    }

    private static String asText(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : node.asText();
    }
}
