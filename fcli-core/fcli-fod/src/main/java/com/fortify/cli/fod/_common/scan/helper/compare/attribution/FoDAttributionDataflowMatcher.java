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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionFileChangeIndex.FileMatch;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionFileChangeIndex.Kind;

/**
 * Matches files on SAST issue dataflow traces against scan manifest file changes. Only issues with a
 * dataflow source at an unchanged sink location are considered, as sink location changes are handled by
 * {@link FoDAttributionFileChangeMatcher}; this also avoids trace lookups for issues that are already
 * directly attributed. Added or removed source files directly explain issue status changes; changed or
 * moved source files, and changes to files with interim dataflow steps, may explain issue status changes.
 */
public final class FoDAttributionDataflowMatcher implements IFoDIssueEvidenceMatcher {
    enum Role { SOURCE, INTERIM }

    /** @param matches Manifest files matching the trace location, of which at least one was changed */
    record TraceFileMatch(Role role, String location, List<FileMatch> matches) {}

    @Override
    public List<FoDIssueEvidence> match(FoDAttributionContext context, FoDAttributionIssue issue) {
        var descriptionsByCause = new LinkedHashMap<FoDAttributionCause, List<String>>();
        for ( var traceFile : getChangedTraceFiles(context, issue) ) {
            traceFile.matches().forEach(m -> FoDAttributionFileChangeMatcher.reference(context, m));
            var kinds = traceFile.matches().stream().map(FileMatch::kind).distinct().toList();
            var cause = kinds.size() > 1 ? FoDAttributionCause.FILE_MATCH_AMBIGUOUS : getCause(traceFile.role(), kinds.get(0));
            descriptionsByCause.computeIfAbsent(cause, k -> new ArrayList<>()).add(describe(traceFile, kinds.size() > 1));
        }
        return descriptionsByCause.entrySet().stream().map(FoDAttributionDataflowMatcher::toEvidence).toList();
    }

    /**
     * @return Changed files on the dataflow of the given issue, excluding the sink file; empty if the issue has no
     *         dataflow source, if its sink location was changed, or if no manifest files were changed at all. Results
     *         are cached per issue, as multiple matchers depend on them.
     */
    static List<TraceFileMatch> getChangedTraceFiles(FoDAttributionContext context, FoDAttributionIssue issue) {
        Map<FoDAttributionIssue, List<TraceFileMatch>> cache = context.getCached(TraceFileMatch.class, IdentityHashMap::new);
        return cache.computeIfAbsent(issue, i -> loadChangedTraceFiles(context, i));
    }

    private static List<TraceFileMatch> loadChangedTraceFiles(FoDAttributionContext context, FoDAttributionIssue issue) {
        var index = context.getChangeIndex(FoDAttributionFileChangeIndex.class);
        var lookup = context.getTraceLookup();
        if ( index == null || lookup == null || !index.hasChanges() || !issue.hasDataflowSource()
                || index.isLocationChanged(issue.getLocation()) ) {
            return List.of();
        }
        var traces = lookup.lookup(issue);
        if ( traces == null ) { return List.of(); }
        var result = new ArrayList<TraceFileMatch>();
        for ( var traceFile : getTraceFiles(traces, FoDAttributionFileChangeIndex.normalize(issue.getLocation())) ) {
            var matches = index.lookup(traceFile.location());
            if ( matches.stream().anyMatch(m -> m.kind() != Kind.UNCHANGED) ) {
                result.add(new TraceFileMatch(traceFile.role(), traceFile.location(), matches));
            }
        }
        return result;
    }

    // Unique trace files other than the sink file; source role takes precedence if a file contains both source and interim nodes
    private static Collection<TraceFile> getTraceFiles(List<FoDIssueTrace> traces, String normalizedSink) {
        var sources = traces.stream().map(FoDIssueTrace::getSource).filter(Objects::nonNull)
                .map(n -> new TraceFile(n.location(), Role.SOURCE));
        var interims = traces.stream().flatMap(t -> t.getInterimNodes().stream())
                .map(n -> new TraceFile(n.location(), Role.INTERIM));
        var byNormalized = new LinkedHashMap<String, TraceFile>();
        Stream.concat(sources, interims).forEach(f -> {
            var normalized = FoDAttributionFileChangeIndex.normalize(f.location());
            if ( !normalized.isEmpty() && !normalized.equals(normalizedSink) ) { byNormalized.putIfAbsent(normalized, f); }
        });
        return byNormalized.values();
    }

    private record TraceFile(String location, Role role) {}

    private static FoDAttributionCause getCause(Role role, Kind kind) {
        if ( role == Role.INTERIM ) { return FoDAttributionCause.DATAFLOW_FILE_CHANGED; }
        return switch ( kind ) {
            case ADDED -> FoDAttributionCause.SOURCE_FILE_ADDED;
            case REMOVED -> FoDAttributionCause.SOURCE_FILE_REMOVED;
            case CHANGED -> FoDAttributionCause.SOURCE_FILE_CHANGED;
            case MOVED -> FoDAttributionCause.SOURCE_FILE_MOVED;
            case UNCHANGED -> throw new FcliBugException("Unchanged files don't have a cause");
        };
    }

    private static String describe(TraceFileMatch traceFile, boolean ambiguous) {
        var role = traceFile.role() == Role.SOURCE ? "dataflow source" : "dataflow step";
        var matches = traceFile.matches();
        if ( ambiguous ) {
            return String.format("%d files with different changes match %s location %s", matches.size(), role, traceFile.location());
        }
        var description = FoDAttributionFileChangeMatcher.describe(matches.get(0));
        return matches.size() == 1
                ? String.format("%s: %s", role, description)
                : String.format("%s: %d files matching location %s, for example: %s", role, matches.size(), traceFile.location(), description);
    }

    private static FoDIssueEvidence toEvidence(Map.Entry<FoDAttributionCause, List<String>> entry) {
        var descriptions = entry.getValue();
        var evidence = descriptions.size() == 1
                ? descriptions.get(0)
                : String.format("%d dataflow files, for example %s", descriptions.size(), descriptions.get(0));
        return new FoDIssueEvidence(entry.getKey(), evidence);
    }
}
