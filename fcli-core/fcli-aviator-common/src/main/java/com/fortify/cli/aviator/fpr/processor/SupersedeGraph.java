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

import static com.fortify.cli.aviator.fpr.processor.RemediationResponses.hasRemediationChanges;
import static com.fortify.cli.aviator.fpr.processor.RemediationResponses.supersededIssueIds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.Change;

/**
 * Which issues a fix supersedes, as declared by the server. Resolution never depends on map iteration order: when
 * several fixes claim the same issue the widest one wins and ties go to the smaller id.
 */
final class SupersedeGraph {
    private SupersedeGraph() {
    }

    /**
     * Maps each superseded issue to the fix that directly supersedes it. Issues that are part of a supersede cycle, or
     * lead into one, are left out so every involved issue simply keeps its own fix.
     */
    static Map<String, String> resolveDirectSupersededBy(Map<String, AuditResponse> auditResponses,
            Predicate<String> hasWritableEntry) {
        // The responses come from a concurrent map, so nothing here may depend on iteration order: when several fixes
        // claim the same issue the widest one wins (fcli applies the widest fix first), ties are broken by id, and the
        // result is sorted by issue id.
        Map<String, List<String>> claimedBy = new TreeMap<>();
        for (Map.Entry<String, AuditResponse> entry : auditResponses.entrySet()) {
            String fixId = entry.getKey();
            if (!hasRemediationChanges(entry.getValue()) || !hasWritableEntry.test(fixId)) {
                continue;
            }
            for (String supersededId : supersededIssueIds(entry.getValue())) {
                AuditResponse supersededResponse = auditResponses.get(supersededId);
                if (!supersededId.equals(fixId) && hasRemediationChanges(supersededResponse)
                        && hasWritableEntry.test(supersededId)) {
                    claimedBy.computeIfAbsent(supersededId, k -> new ArrayList<>()).add(fixId);
                }
            }
        }
        Map<String, String> direct = new TreeMap<>();
        claimedBy.forEach((supersededId, fixIds) -> direct.put(supersededId, fixIds.stream()
                .min(Comparator.comparingInt((String id) -> -fixWidth(auditResponses.get(id))).thenComparing(Comparator.naturalOrder()))
                .orElseThrow()));
        Map<String, String> acyclic = new TreeMap<>();
        for (String supersededId : direct.keySet()) {
            Set<String> visited = new HashSet<>(Set.of(supersededId));
            String fixId = direct.get(supersededId);
            while (direct.containsKey(fixId) && visited.add(fixId)) {
                fixId = direct.get(fixId);
            }
            if (!direct.containsKey(fixId)) {
                acyclic.put(supersededId, direct.get(supersededId));
            }
        }
        return acyclic;
    }

    /** Widest change of a fix in lines (lineTo - lineFrom), the same measure fcli uses to apply the widest fix first. */
    private static int fixWidth(AuditResponse response) {
        int width = 0;
        for (Change change : response.getAuditResult().getAutoremediation().getChanges()) {
            try {
                width = Math.max(width, Integer.parseInt(change.getToLine().trim()) - Integer.parseInt(change.getFromLine().trim()));
            } catch (NumberFormatException | NullPointerException e) {
                // an unparsable range does not make the fix wider
            }
        }
        return width;
    }

    /** The first issue at or above {@code startId} in a supersede chain whose fix was written, or null if none was. */
    static String writtenFixFrom(String startId, Map<String, String> directSupersededBy, Set<String> writtenFixIds) {
        String id = startId;
        while (id != null && !writtenFixIds.contains(id)) {
            id = directSupersededBy.get(id);
        }
        return id;
    }

    static int supersedeDepth(String id, Map<String, String> directSupersededBy) {
        int depth = 0;
        for (String above = directSupersededBy.get(id); above != null; above = directSupersededBy.get(above)) {
            depth++;
        }
        return depth;
    }
}
