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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Handles issues with the same category that were both fixed and newly found or reopened at the same, unchanged
 * location. If any files on the issue dataflow were changed, these changes take precedence and are reported by
 * {@link FoDAttributionDataflowMatcher}. Otherwise, if correlation error detection is enabled and a fixed issue has
 * the same sink and dataflow source (file and line) as a new or reopened issue, the status change is attributed to
 * an issue instance ID correlation error, as differences in interim dataflow steps shouldn't result in new issue
 * instances. Otherwise, the issue is rendered indeterminate, as the status change may be caused by changes elsewhere
 * in the data flow, or by issue correlation; neither can be determined from the available data.
 */
public final class FoDAttributionPairedLocationMatcher implements IFoDIssueEvidenceMatcher {
    private final boolean detectCorrelationErrors;

    /** Create a matcher that doesn't detect issue instance ID correlation errors */
    public FoDAttributionPairedLocationMatcher() {
        this(false);
    }

    /**
     * @param detectCorrelationErrors Whether to detect issue instance ID correlation errors based on sink line numbers
     *        and, for dataflow issues, dataflow sources from issue traces; only applicable to SAST issues
     */
    public FoDAttributionPairedLocationMatcher(boolean detectCorrelationErrors) {
        this.detectCorrelationErrors = detectCorrelationErrors;
    }

    @Override
    public List<FoDIssueEvidence> match(FoDAttributionContext context, FoDAttributionIssue issue) {
        var location = getLocationKey(issue.getLocation());
        if ( location.isEmpty() ) { return List.of(); }
        var category = StringUtils.defaultString(issue.getCategory());
        var pairs = context.getCached(PairIndex.class, () -> PairIndex.of(context.getIssues()));
        var counterparts = pairs.getCounterparts(issue, location, category);
        if ( counterparts == null ) { return List.of(); }
        var changeIndex = context.getChangeIndex();
        if ( changeIndex != null && changeIndex.isLocationChanged(issue.getLocation()) ) { return List.of(); }
        if ( !FoDAttributionDataflowMatcher.getChangedTraceFiles(context, issue).isEmpty() ) { return List.of(); }
        var correlationError = detectCorrelationErrors ? getCorrelationError(context, issue, counterparts) : null;
        return List.of(correlationError != null ? correlationError : new FoDIssueEvidence(
                FoDAttributionCause.PAIRED_AT_UNCHANGED_LOCATION, String.format(
                "both fixed and new or reopened '%s' issues at location %s, which was not changed",
                category, issue.getLocation())));
    }

    private static FoDIssueEvidence getCorrelationError(FoDAttributionContext context, FoDAttributionIssue issue, Group counterparts) {
        var signatures = context.getCached(SignatureCache.class, () -> new SignatureCache(context));
        var signature = signatures.get(issue);
        var correlated = signature == null ? null : counterparts.findBySignature(signature.key(), signatures::get);
        return correlated == null ? null : new FoDIssueEvidence(FoDAttributionCause.INSTANCE_ID_CORRELATION_ERROR, String.format(
                "%s issue %s has the same category, sink and dataflow source (%s); issue instance ID correlation may have failed",
                correlated.transition().getJsonName(), correlated.getText("vulnId"), signature.description()));
    }

    private static String getLocationKey(String location) {
        var result = StringUtils.trimToEmpty(location).replace('\\', '/').toLowerCase(Locale.ROOT);
        return StringUtils.strip(result, "/");
    }

    /** Sink and dataflow source locations; compared by key (normalized locations), but described by original locations */
    private record Signature(String key, String description) {
        /**
         * @return Signature for the given issue, or null if not available; for dataflow issues, this requires issue
         *         traces, as otherwise dataflow sources can't be compared
         */
        static Signature of(FoDAttributionContext context, FoDAttributionIssue issue) {
            var lineNumber = issue.getText("lineNumber");
            if ( StringUtils.isBlank(lineNumber) ) { return null; }
            var sink = new Location(issue.getLocation(), lineNumber);
            if ( !issue.hasDataflowSource() ) { return of(sink, List.of()); }
            var lookup = context.getTraceLookup();
            var traces = lookup == null ? null : lookup.lookup(issue);
            if ( traces == null || traces.isEmpty() ) { return null; }
            var sources = new ArrayList<Location>();
            for ( var trace : traces ) {
                var source = trace.getSource();
                if ( source == null || source.lineNumber() == null ) { return null; }
                sources.add(new Location(source.location(), source.lineNumber().toString()));
            }
            return of(sink, sources);
        }

        private static Signature of(Location sink, List<Location> sources) {
            var key = sink.key() + "|" + sources.stream().map(Location::key).distinct().sorted().collect(Collectors.joining(","));
            var sourceDescription = sources.stream().map(Location::toString).distinct().collect(Collectors.joining(", "));
            return new Signature(key, sources.isEmpty() ? "sink " + sink : String.format("sink %s, source %s", sink, sourceDescription));
        }
    }

    private record Location(String location, String lineNumber) {
        String key() {
            return FoDAttributionFileChangeIndex.normalize(location) + ":" + lineNumber;
        }

        @Override
        public String toString() {
            return location + ":" + lineNumber;
        }
    }

    /** Signatures per issue (by identity), including unavailable signatures, computed at most once per issue */
    private static final class SignatureCache {
        private final FoDAttributionContext context;
        private final Map<FoDAttributionIssue, Optional<Signature>> signatures = new IdentityHashMap<>();

        SignatureCache(FoDAttributionContext context) {
            this.context = context;
        }

        Signature get(FoDAttributionIssue issue) {
            return signatures.computeIfAbsent(issue, i -> Optional.ofNullable(Signature.of(context, i))).orElse(null);
        }
    }

    /** Issues at the same location, with the same category and transition group (fixed, or new and reopened) */
    private static final class Group {
        private final List<FoDAttributionIssue> issues = new ArrayList<>();
        private Map<String, FoDAttributionIssue> bySignatureKey;

        /** @return First issue with the given signature key; signatures are indexed once per group, when first needed */
        FoDAttributionIssue findBySignature(String signatureKey, Function<FoDAttributionIssue, Signature> signatures) {
            if ( bySignatureKey == null ) {
                bySignatureKey = new HashMap<>();
                for ( var issue : issues ) {
                    var signature = signatures.apply(issue);
                    if ( signature != null ) { bySignatureKey.putIfAbsent(signature.key(), issue); }
                }
            }
            return bySignatureKey.get(signatureKey);
        }
    }

    private static final class PairIndex {
        // Location -> category -> [fixed issues, new or reopened issues]
        private final Map<String, Map<String, List<Group>>> groups = new HashMap<>();

        static PairIndex of(List<FoDAttributionIssue> issues) {
            var result = new PairIndex();
            for ( var issue : issues ) {
                result.groups.computeIfAbsent(getLocationKey(issue.getLocation()), k -> new HashMap<>())
                        .computeIfAbsent(StringUtils.defaultString(issue.getCategory()), k -> List.of(new Group(), new Group()))
                        .get(getGroupIndex(issue)).issues.add(issue);
            }
            return result;
        }

        /** @return Issues at the given location and with the given category, but with the opposite transition, or null if none */
        Group getCounterparts(FoDAttributionIssue issue, String location, String category) {
            var groupPair = groups.getOrDefault(location, Map.of()).get(category);
            var result = groupPair == null ? null : groupPair.get(1 - getGroupIndex(issue));
            return result == null || result.issues.isEmpty() ? null : result;
        }

        private static int getGroupIndex(FoDAttributionIssue issue) {
            return issue.transition() == FoDAttributionTransition.FIXED ? 0 : 1;
        }
    }
}
