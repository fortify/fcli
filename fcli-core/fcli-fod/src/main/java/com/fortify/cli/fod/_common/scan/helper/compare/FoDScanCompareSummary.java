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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionResult;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionTransition;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDScanCondition;

import lombok.RequiredArgsConstructor;

/**
 * Short, human-readable summary of a scan comparison: which causes drove new, reopened and fixed issues,
 * or which scan changes were detected if there are no issue changes, or that nothing changed at all.
 */
@RequiredArgsConstructor
public final class FoDScanCompareSummary {
    private static final int MAX_CAUSES = 3;
    private final FoDScanCompareIssues issues;
    private final List<String> changedSettings;
    private final IFoDScanCompareChanges changes;
    /** Null if attribution is disabled */
    private final FoDAttributionResult attributionResult;

    public String getText() {
        var result = new StringBuilder(getMainSentence()).append('.');
        var skipReason = changes.getSkipReason();
        if ( skipReason != null ) { result.append(' ').append(skipReason).append(", so scan changes may be incomplete."); }
        return result.toString();
    }

    private String getMainSentence() {
        var issueParts = new ArrayList<String>();
        for ( var transition : FoDAttributionTransition.values() ) {
            int count = issues.getIssues(transition).size();
            if ( count > 0 ) { issueParts.add(getIssueDescription(transition, count)); }
        }
        if ( !issueParts.isEmpty() ) { return String.join("; ", issueParts); }
        var scanChanges = getScanChangeDescriptions();
        return scanChanges.isEmpty()
                ? "No issue or scan changes detected"
                : "No new, reopened or fixed issues, despite " + joinWithAnd(scanChanges);
    }

    // For example: '50 fixed issues driven by URL_REMOVED (39), SCAN_POLICY_CHANGE (9) and 2 other causes, 1 indeterminate'
    private String getIssueDescription(FoDAttributionTransition transition, int count) {
        var result = String.format("%s %s", count, pluralize(transition.getJsonName() + " issue", count));
        if ( attributionResult == null ) { return result; }
        var causes = attributionResult.getDriverCauses(transition).entrySet().stream()
                .map(e -> e.getValue() == count ? e.getKey().name() : String.format("%s (%s)", e.getKey().name(), e.getValue()))
                .toList();
        if ( causes.isEmpty() ) { return result + " (indeterminate)"; }
        var shownCauses = new ArrayList<>(causes.subList(0, Math.min(MAX_CAUSES, causes.size())));
        if ( causes.size() > MAX_CAUSES ) {
            int otherCount = causes.size() - MAX_CAUSES;
            shownCauses.add(String.format("%s other %s", otherCount, pluralize("cause", otherCount)));
        }
        result += " driven by " + joinWithAnd(shownCauses);
        var indeterminate = attributionResult.getIndeterminateCount(transition);
        return indeterminate == 0 ? result : String.format("%s, %s indeterminate", result, indeterminate);
    }

    private List<String> getScanChangeDescriptions() {
        var result = new ArrayList<String>();
        if ( !changedSettings.isEmpty() ) {
            result.add(String.format("changed %s (%s)", pluralize("setting", changedSettings.size()), String.join(", ", changedSettings)));
        }
        if ( attributionResult != null && !attributionResult.getConditions().isEmpty() ) {
            result.add(attributionResult.getConditions().stream().map(FoDScanCondition::cause).map(Enum::name).distinct()
                    .collect(Collectors.joining(", ", "scan conditions (", ")")));
        }
        var countsByItemName = new LinkedHashMap<String, Integer>();
        changes.getChangeSets().forEach(s -> countsByItemName.merge(s.itemName(), s.items().size(), Integer::sum));
        countsByItemName.forEach((itemName, count) -> {
            if ( count > 0 ) { result.add(String.format("%s %s", count, pluralize(itemName + " change", count))); }
        });
        return result;
    }

    private static String joinWithAnd(List<String> parts) {
        if ( parts.size() == 1 ) { return parts.get(0); }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }

    private static String pluralize(String singular, int count) {
        return count == 1 ? singular : singular + "s";
    }
}
