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
package com.fortify.cli.aviator.fpr.remediation.model;

import java.util.Map;
import java.util.Set;

public record RemediationMetric(int totalRemediations, int appliedRemediations, int identicalRemediations,
                                 int supersededRemediations, int possiblyRemediatedRemediations, int skippedRemediations,
                                 Set<String> modifiedFiles, Map<String, Integer> skippedByReason) {
    public RemediationMetric(int totalRemediations, int appliedRemediations, int identicalRemediations,
                             int supersededRemediations, int skippedRemediations, Set<String> modifiedFiles) {
        this(totalRemediations, appliedRemediations, identicalRemediations, supersededRemediations,
             0, skippedRemediations, modifiedFiles, Map.of());
    }
}
