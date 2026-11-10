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
package com.fortify.cli.aviator.fpr.processor;

import static com.fortify.cli.aviator.fpr.processor.RemediationResponses.coveredByPriorFixIds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;

/**
 * The notes the server appends to audit comments ("already addressed by the fix for issue X", "also contains the fix
 * for issue Y") are written before it knows which fix finally wins. This rewrites them so they name the fixes that are
 * written to remediations.xml.
 */
final class PriorFixNotes {
    private PriorFixNotes() {
    }

    private static final Logger NOTE_LOGGER = LoggerFactory.getLogger(PriorFixNotes.class);

    private static final Pattern COVERED_NOTE = Pattern.compile(
            "(already addressed by )(?:the fix for issue |the fixes for issues )([\\w-]+(?:, [\\w-]+)*)(\\.)");
    private static final Pattern SUPERSEDE_NOTE = Pattern.compile(
            "(also contains )(?:the fix for issue |the fixes for issues )([\\w-]+(?:, [\\w-]+)*)(, which falls within the lines it changes\\.)");

    private static final Pattern SUPERSEDE_NOTE_SENTENCE = Pattern.compile(
            "\\s*Note: The automated remediation for this finding also contains (?:the fix for issue |the fixes for issues )"
            + "[\\w-]+(?:, [\\w-]+)*, which falls within the lines it changes\\.");

    /**
     * Returns the responses to write, with the notes in their audit comments rewritten so they name fixes that are written to
     * remediations.xml: a covering fix that was superseded is replaced by the nearest written fix above it (the same one the
     * writer references), a superseding fix lists every issue it absorbed, including through chains, and the supersede
     * sentence is dropped from a fix that was not written. Notes that name an issue without a written fix above it are left
     * as they are. The responses passed in are not modified: a changed response is a copy.
     * <p>
     * The notes are English text written by the server, so the rewrite depends on its wording (pinned by tests). The ids
     * themselves are always available structurally; when they say a note needs rewriting but no note is found in the
     * comment, a warning is logged and the comment is left unchanged.
     *
     * @param writtenFixIds the fixes that are written, or null when nothing was superseded
     */
    static Map<String, AuditResponse> withResolvedPriorFixNotes(Map<String, AuditResponse> auditResponses,
            Set<String> writtenFixIds) {
        if (writtenFixIds == null) {
            return auditResponses;
        }
        Map<String, String> direct = SupersedeGraph.resolveDirectSupersededBy(auditResponses, id -> true);
        if (direct.isEmpty()) {
            return auditResponses;
        }
        Map<String, AuditResponse> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, AuditResponse> entry : auditResponses.entrySet()) {
            AuditResponse response = entry.getValue();
            AuditResult result = response == null ? null : response.getAuditResult();
            String comment = result == null ? null : result.getComment();
            String rewritten = comment == null ? null : rewriteNotes(entry.getKey(), response, comment, direct, writtenFixIds);
            if (rewritten != null && !rewritten.equals(comment)) {
                resolved.put(entry.getKey(),
                        response.toBuilder().auditResult(result.toBuilder().comment(rewritten).build()).build());
            } else {
                resolved.put(entry.getKey(), response);
            }
        }
        return resolved;
    }

    private static String rewriteNotes(String issueId, AuditResponse response, String comment, Map<String, String> direct,
            Set<String> writtenFixIds) {
        boolean coveredNeedsRewrite = coveredByPriorFixIds(response).stream().anyMatch(id -> {
            String written = SupersedeGraph.writtenFixFrom(id, direct, writtenFixIds);
            return written != null && !written.equals(id);
        });
        if (coveredNeedsRewrite && !COVERED_NOTE.matcher(comment).find()) {
            NOTE_LOGGER.warn("Issue {} is covered by a fix that was superseded, but its audit comment has no recognizable "
                    + "covered-by-prior-fix note (the server wording may have changed); the comment is left unchanged", issueId);
        }
        String rewritten = COVERED_NOTE.matcher(comment).replaceAll(match -> {
            List<String> fixIds = Arrays.stream(match.group(2).split(", "))
                    .map(id -> {
                        String written = SupersedeGraph.writtenFixFrom(id, direct, writtenFixIds);
                        return written != null ? written : id;
                    }).distinct().toList();
            return Matcher.quoteReplacement(match.group(1) + noteSubject(fixIds) + match.group(3));
        });
        if (!writtenFixIds.contains(issueId)) {
            return SUPERSEDE_NOTE_SENTENCE.matcher(rewritten).replaceAll("");
        }
        List<String> absorbed = direct.keySet().stream()
                .filter(supersededId -> issueId.equals(SupersedeGraph.writtenFixFrom(direct.get(supersededId), direct, writtenFixIds)))
                .toList();
        if (absorbed.isEmpty()) {
            return rewritten;
        }
        if (!SUPERSEDE_NOTE.matcher(rewritten).find()) {
            NOTE_LOGGER.warn("Issue {} supersedes {} but its audit comment has no recognizable supersede note "
                    + "(the server wording may have changed); the comment is left unchanged", issueId, absorbed);
            return rewritten;
        }
        return SUPERSEDE_NOTE.matcher(rewritten).replaceAll(match -> {
            Set<String> ids = new LinkedHashSet<>(List.of(match.group(2).split(", ")));
            ids.addAll(absorbed);
            return Matcher.quoteReplacement(match.group(1) + noteSubject(new ArrayList<>(ids)) + match.group(3));
        });
    }

    private static String noteSubject(List<String> ids) {
        return (ids.size() == 1 ? "the fix for issue " : "the fixes for issues ") + String.join(", ", ids);
    }
}
