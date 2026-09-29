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

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class AppliedChangeTest {

    /**
     * {@code contentCovers} must return {@code false}, not {@code true}, when either side's
     * comparison code is unavailable ({@code null}) or blank - unavailable content is not proof
     * of coverage. These branches are unreachable end-to-end through
     * {@code RemediationProcessor.processRemediationXML}
     * (see {@link RemediationProcessorKnownIssuesTest#malformedRemediationIsSkippedAndValidRemediationsStillApply}:
     * a missing comparison code causes the remediation to be skipped before the classifier is
     * ever reached for it), so this is asserted directly against {@link AppliedChange}.
     */
    @Test
    void unavailableComparisonCodeIsNotTreatedAsProvenCoverage() {
        assertFalse(
            AppliedChange.builder()
                .originalLineFrom(1)
                .originalLineTo(3)
                .deltaLines(0)
                .comparisonCode("W1W2W3")
                .lineNormalizedContent(new String[] {"W1", "W2", "W3"})
                .build()
                .contentCovers(null, 2, 2),
            "a candidate whose content could not be computed has not been proven covered");

        assertFalse(
            AppliedChange.builder()
                .originalLineFrom(1)
                .originalLineTo(3)
                .deltaLines(0)
                .comparisonCode(null)
                .lineNormalizedContent(null)
                .build()
                .contentCovers("M2", 2, 2),
            "an applied change whose content is unknown cannot prove it covers anything");
    }
}
