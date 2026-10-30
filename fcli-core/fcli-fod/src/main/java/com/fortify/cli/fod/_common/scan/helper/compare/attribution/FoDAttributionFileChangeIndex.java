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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

/**
 * Index of file changes between two scan manifests. Files are considered moved if a removed file
 * and an added file have the same file name and the same (non-empty) manifest fingerprint.
 * Issue locations are matched against manifest paths case-insensitively, either exactly or by
 * path suffix, as manifest paths may have a root prefix (like '/Src/') that issue locations don't have.
 */
public final class FoDAttributionFileChangeIndex implements IFoDAttributionChangeIndex {
    public enum Kind { ADDED, REMOVED, CHANGED, MOVED, UNCHANGED }

    /**
     * @param path Manifest path as listed in the compare output
     * @param movedFrom For moved files, the manifest path in the previous scan
     * @param movedTo For moved files, the manifest path in the current scan
     */
    public record FileMatch(Kind kind, String path, String movedFrom, String movedTo) {}

    private final Map<String, FileMatch> byNormalizedPath = new LinkedHashMap<>();
    private final Map<String, List<String>> normalizedPathsByName = new HashMap<>();
    /** Manifest paths that are only present in the current manifest, including moved files */
    @Getter private final List<String> addedPaths;
    /** Manifest paths that are only present in the previous manifest, including moved files */
    @Getter private final List<String> removedPaths;
    /** Manifest paths that are present in both manifests, but with a different fingerprint */
    @Getter private final List<String> changedPaths;
    @Getter private final int previousFileCount;
    @Getter private final int movedCount;

    /** @param previous/current Manifest path to fingerprint (non-timestamp manifest columns, like file size) */
    public FoDAttributionFileChangeIndex(Map<String, String> previous, Map<String, String> current) {
        this.previousFileCount = previous.size();
        this.addedPaths = current.keySet().stream().filter(p -> !previous.containsKey(p)).toList();
        this.removedPaths = previous.keySet().stream().filter(p -> !current.containsKey(p)).toList();
        this.changedPaths = current.keySet().stream()
                .filter(p -> previous.containsKey(p) && !Objects.equals(previous.get(p), current.get(p))).toList();
        var moves = findMoves(previous, current, addedPaths, removedPaths);
        this.movedCount = moves.size();
        var movedSources = new HashMap<String, String>();
        moves.forEach((from, to) -> movedSources.put(to, from));
        current.forEach((path, fingerprint) -> {
            if ( previous.containsKey(path) ) {
                add(new FileMatch(Objects.equals(previous.get(path), fingerprint) ? Kind.UNCHANGED : Kind.CHANGED, path, null, null));
            } else if ( movedSources.containsKey(path) ) {
                add(new FileMatch(Kind.MOVED, path, movedSources.get(path), path));
            } else {
                add(new FileMatch(Kind.ADDED, path, null, null));
            }
        });
        removedPaths.forEach(path -> add(moves.containsKey(path)
                ? new FileMatch(Kind.MOVED, path, path, moves.get(path))
                : new FileMatch(Kind.REMOVED, path, null, null)));
    }

    /** @return Manifest files matching the given issue location; exact matches take precedence over suffix matches */
    public List<FileMatch> lookup(String location) {
        var normalized = normalize(location);
        if ( normalized.isEmpty() ) { return List.of(); }
        var candidates = normalizedPathsByName.getOrDefault(getName(normalized), List.of());
        if ( candidates.contains(normalized) ) { return List.of(byNormalizedPath.get(normalized)); }
        return candidates.stream()
                .filter(p -> p.endsWith("/" + normalized) || normalized.endsWith("/" + p))
                .map(byNormalizedPath::get)
                .toList();
    }

    @Override
    public boolean isLocationChanged(String location) {
        return lookup(location).stream().anyMatch(m -> m.kind() != Kind.UNCHANGED);
    }

    private void add(FileMatch match) {
        var normalized = normalize(match.path());
        if ( byNormalizedPath.putIfAbsent(normalized, match) == null ) {
            normalizedPathsByName.computeIfAbsent(getName(normalized), k -> new ArrayList<>()).add(normalized);
        }
    }

    // Pairs removed and added files with the same name and fingerprint; returns removed path to added path
    private static Map<String, String> findMoves(Map<String, String> previous, Map<String, String> current,
            Collection<String> added, Collection<String> removed) {
        var addedByKey = new HashMap<String, Deque<String>>();
        added.forEach(path -> {
            var key = getMoveKey(path, current.get(path));
            if ( key != null ) { addedByKey.computeIfAbsent(key, k -> new ArrayDeque<>()).add(path); }
        });
        var result = new LinkedHashMap<String, String>();
        removed.forEach(path -> {
            var candidates = addedByKey.get(getMoveKey(path, previous.get(path)));
            if ( candidates != null && !candidates.isEmpty() ) { result.put(path, candidates.poll()); }
        });
        return result;
    }

    private static String getMoveKey(String path, String fingerprint) {
        return StringUtils.isBlank(fingerprint) ? null : getName(normalize(path)) + "|" + fingerprint;
    }

    static String normalize(String path) {
        if ( path == null ) { return ""; }
        var result = path.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        result = StringUtils.stripStart(result, "/");
        return result.startsWith("./") ? result.substring(2) : result;
    }

    private static String getName(String normalizedPath) {
        return StringUtils.substringAfterLast("/" + normalizedPath, "/");
    }
}
