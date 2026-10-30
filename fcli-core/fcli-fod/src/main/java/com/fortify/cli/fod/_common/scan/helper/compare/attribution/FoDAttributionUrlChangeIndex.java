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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Index of URL changes between two site trees, keyed by normalized URL. Issue locations with
 * an origin (scheme, host and port) are matched exactly; relative issue locations are matched
 * by path only.
 */
public final class FoDAttributionUrlChangeIndex implements IFoDAttributionChangeIndex {
    public enum Kind { ADDED, REMOVED, CHANGED, UNCHANGED }

    /** Site tree URL with all methods and status codes observed for that URL */
    public record SiteTreeUrl(String url, Set<String> methods, Set<String> statusCodes) {}

    public record UrlMatch(Kind kind, SiteTreeUrl previous, SiteTreeUrl current) {
        public String url() {
            return current != null ? current.url() : previous.url();
        }
    }

    private final Map<String, UrlMatch> byKey = new LinkedHashMap<>();
    private final Map<String, List<UrlMatch>> byPath = new HashMap<>();
    private final TreeMap<String, List<UrlMatch>> changedByPath = new TreeMap<>();

    public FoDAttributionUrlChangeIndex(Collection<SiteTreeUrl> previous, Collection<SiteTreeUrl> current) {
        var previousByKey = groupByKey(previous);
        var currentByKey = groupByKey(current);
        var keys = new TreeSet<String>(previousByKey.keySet());
        keys.addAll(currentByKey.keySet());
        for ( var key : keys ) {
            var p = previousByKey.get(key);
            var c = currentByKey.get(key);
            var kind = p == null ? Kind.ADDED : c == null ? Kind.REMOVED
                    : p.methods().equals(c.methods()) && p.statusCodes().equals(c.statusCodes()) ? Kind.UNCHANGED : Kind.CHANGED;
            var match = new UrlMatch(kind, p, c);
            byKey.put(key, match);
            var path = FoDAttributionUrls.normalize(match.url()).path();
            byPath.computeIfAbsent(path, k -> new ArrayList<>()).add(match);
            if ( kind != Kind.UNCHANGED ) { changedByPath.computeIfAbsent(path, k -> new ArrayList<>()).add(match); }
        }
    }

    /** @return Site tree URLs matching the given issue location */
    public List<UrlMatch> lookup(String location) {
        var normalized = FoDAttributionUrls.normalize(location);
        if ( normalized == null ) { return List.of(); }
        if ( normalized.origin() == null ) { return byPath.getOrDefault(normalized.path(), List.of()); }
        var match = byKey.get(normalized.key());
        return match == null ? List.of() : List.of(match);
    }

    /**
     * Find changed URLs that are related to the given issue location, being either below the issue
     * location, or in the same directory as the issue location (excluding the root directory, as
     * any URL would be related to the root directory).
     *
     * @return Related changed URLs, or empty list if none
     */
    public List<UrlMatch> findRelatedChanges(String location) {
        var normalized = FoDAttributionUrls.normalize(location);
        if ( normalized == null ) { return List.of(); }
        var path = normalized.path();
        var queryIdx = path.indexOf('?');
        if ( queryIdx >= 0 ) { path = path.substring(0, queryIdx); }
        var descendants = findChangedWithPrefix(path.endsWith("/") ? path : path + "/");
        if ( !descendants.isEmpty() ) { return descendants; }
        var parent = path.substring(0, path.lastIndexOf('/') + 1);
        return parent.length() <= 1 ? List.of() : findChangedWithPrefix(parent);
    }

    @Override
    public boolean isLocationChanged(String location) {
        return lookup(location).stream().anyMatch(m -> m.kind() != Kind.UNCHANGED);
    }

    private List<UrlMatch> findChangedWithPrefix(String prefix) {
        if ( "/".equals(prefix) ) { return List.of(); }
        return changedByPath.subMap(prefix, prefix + Character.MAX_VALUE).values().stream()
                .flatMap(List::stream).toList();
    }

    private static Map<String, SiteTreeUrl> groupByKey(Collection<SiteTreeUrl> urls) {
        var result = new HashMap<String, SiteTreeUrl>();
        for ( var url : urls ) {
            result.merge(FoDAttributionUrls.normalizeKey(url.url()), url, FoDAttributionUrlChangeIndex::merge);
        }
        return result;
    }

    private static SiteTreeUrl merge(SiteTreeUrl a, SiteTreeUrl b) {
        var methods = new TreeSet<>(a.methods());
        methods.addAll(b.methods());
        var statusCodes = new TreeSet<>(a.statusCodes());
        statusCodes.addAll(b.statusCodes());
        return new SiteTreeUrl(a.url(), methods, statusCodes);
    }
}
