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
package com.fortify.cli.aviator.fpr.remediation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;

public record RemediationProcessingOptions(Set<String> issueIdFilter, RemediationExecutionMode executionMode,
        ISourceDecoder sourceDecoder) {
    public RemediationProcessingOptions {
        issueIdFilter = issueIdFilter == null
            ? Set.of()
            : Collections.unmodifiableSet(new LinkedHashSet<>(issueIdFilter));
        executionMode = Objects.requireNonNull(executionMode, "executionMode");
        sourceDecoder = Objects.requireNonNull(sourceDecoder, "sourceDecoder");
    }

    public boolean isFiltered() {
        return !issueIdFilter.isEmpty();
    }

    public boolean isPreview() {
        return executionMode == RemediationExecutionMode.PREVIEW;
    }
}