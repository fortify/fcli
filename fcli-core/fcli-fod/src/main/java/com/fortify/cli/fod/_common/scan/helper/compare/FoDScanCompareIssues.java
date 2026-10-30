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
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionIssue;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionTransition;

/** All new, reopened and fixed issues between two scans */
public record FoDScanCompareIssues(List<ObjectNode> newIssues, List<ObjectNode> reopenedIssues, List<ObjectNode> fixedIssues) {
    /**
     * @return Issues to be attributed, highest severity first, such that bounded per-issue lookups (like issue
     *         traces) are spent on the most relevant issues
     */
    public List<FoDAttributionIssue> toAttributionIssues() {
        var result = new ArrayList<FoDAttributionIssue>();
        newIssues.forEach(i -> result.add(new FoDAttributionIssue(i, FoDAttributionTransition.NEW)));
        reopenedIssues.forEach(i -> result.add(new FoDAttributionIssue(i, FoDAttributionTransition.REOPENED)));
        fixedIssues.forEach(i -> result.add(new FoDAttributionIssue(i, FoDAttributionTransition.FIXED)));
        result.sort(Comparator.comparing(FoDAttributionIssue::node, FoDScanCompareHelper.ISSUE_ORDER));
        return result;
    }

    /** Add issue lists, limited to the given maximum number of issues per list, together with counts and truncation flags */
    public void addTo(ObjectNode result, int maxIssues) {
        addList(result, "newIssue", newIssues, maxIssues);
        addList(result, "reopenedIssue", reopenedIssues, maxIssues);
        addList(result, "fixedIssue", fixedIssues, maxIssues);
    }

    private static void addList(ObjectNode result, String name, List<ObjectNode> issues, int maxIssues) {
        var list = new FoDScanCompareLimitedList<JsonNode>(maxIssues, FoDScanCompareHelper.ISSUE_ORDER);
        issues.forEach(list::add);
        list.addTo(result, name, Function.identity());
    }
}
