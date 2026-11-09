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
package com.fortify.cli.aviator.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.ReauditClassifier.Decision;

class ReauditClassifierTest {
    private static final String CURRENT = "v1:aaaa";

    @Test
    void unavailableEvidenceIsNeverBaselinedOrReaudited() {
        assertEquals(Decision.UNAVAILABLE, ReauditClassifier.classify(Optional.empty(), null));
        assertEquals(Decision.UNAVAILABLE, ReauditClassifier.classify(Optional.empty(), CURRENT));
    }

    @Test
    void missingOrBlankStoredFingerprintIsBaselined() {
        assertEquals(Decision.BASELINE, ReauditClassifier.classify(Optional.of(CURRENT), null));
        assertEquals(Decision.BASELINE, ReauditClassifier.classify(Optional.of(CURRENT), ""));
        assertEquals(Decision.BASELINE, ReauditClassifier.classify(Optional.of(CURRENT), "   "));
    }

    @Test
    void storedFingerprintFromAnotherAlgorithmVersionIsRebaselinedNotReaudited() {
        assertEquals(Decision.BASELINE, ReauditClassifier.classify(Optional.of(CURRENT), "v0:aaaa"));
        assertEquals(Decision.BASELINE, ReauditClassifier.classify(Optional.of(CURRENT), "deadbeef"));
    }

    @Test
    void identicalFingerprintIsSkippedAndDifferentOneIsReaudited() {
        assertEquals(Decision.SKIP, ReauditClassifier.classify(Optional.of(CURRENT), CURRENT));
        assertEquals(Decision.REAUDIT, ReauditClassifier.classify(Optional.of(CURRENT), "v1:bbbb"));
    }
}
