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

import java.util.List;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionFileChangeIndex.FileMatch;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionFileChangeIndex.Kind;

/** Matches SAST issue locations against scan manifest file changes */
public final class FoDAttributionFileChangeMatcher implements IFoDIssueEvidenceMatcher {
    @Override
    public List<FoDIssueEvidence> match(FoDAttributionContext context, FoDAttributionIssue issue) {
        var index = context.getChangeIndex(FoDAttributionFileChangeIndex.class);
        if ( index == null ) { return List.of(); }
        var location = issue.getLocation();
        var matches = index.lookup(location);
        var kinds = matches.stream().map(FileMatch::kind).distinct().toList();
        if ( kinds.isEmpty() || kinds.equals(List.of(Kind.UNCHANGED)) ) { return List.of(); }
        matches.forEach(m -> reference(context, m));
        if ( kinds.size() > 1 ) {
            return List.of(new FoDIssueEvidence(FoDAttributionCause.FILE_MATCH_AMBIGUOUS, String.format(
                    "%d files with different changes match issue location %s", matches.size(), location)));
        }
        var evidence = matches.size() == 1
                ? describe(matches.get(0))
                : String.format("%d files matching issue location %s, for example: %s", matches.size(), location, describe(matches.get(0)));
        return List.of(new FoDIssueEvidence(getCause(kinds.get(0)), evidence));
    }

    static void reference(FoDAttributionContext context, FileMatch match) {
        if ( match.kind() == Kind.UNCHANGED ) { return; }
        context.referenceChange(match.path());
        context.referenceChange(match.movedFrom());
        context.referenceChange(match.movedTo());
    }

    private static FoDAttributionCause getCause(Kind kind) {
        return switch ( kind ) {
            case ADDED -> FoDAttributionCause.FILE_ADDED;
            case REMOVED -> FoDAttributionCause.FILE_REMOVED;
            case CHANGED -> FoDAttributionCause.FILE_CHANGED;
            case MOVED -> FoDAttributionCause.FILE_MOVED;
            case UNCHANGED -> throw new FcliBugException("Unchanged files don't have a cause");
        };
    }

    static String describe(FileMatch match) {
        return switch ( match.kind() ) {
            case ADDED -> String.format("file %s was added", match.path());
            case REMOVED -> String.format("file %s was removed", match.path());
            case CHANGED -> String.format("file %s was changed", match.path());
            case MOVED -> String.format("file was moved from %s to %s", match.movedFrom(), match.movedTo());
            case UNCHANGED -> String.format("file %s was not changed", match.path());
        };
    }
}
