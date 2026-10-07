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
package com.fortify.cli.aviator._common.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;

public final class AviatorIssueIdFilterUtils {
    private AviatorIssueIdFilterUtils() {}

    public static Set<String> normalizeIssueIds(List<String> issueIds) {
        if (issueIds == null) {
            return null;
        }
        Set<String> normalizedIssueIds = issueIds.stream()
                .map(StringUtils::trimToNull)
                .filter(StringUtils::isNotBlank)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        if (normalizedIssueIds.isEmpty()) {
            throw new FcliSimpleException("--issue-ids must contain at least one non-blank issue ID");
        }
        return normalizedIssueIds;
    }
}