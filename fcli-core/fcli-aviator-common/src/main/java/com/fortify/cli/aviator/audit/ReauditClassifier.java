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

import java.util.Optional;

import com.fortify.cli.aviator.util.StringUtil;

/** Decides what to do with an already-audited issue by comparing its current and stored evidence fingerprints. */
public final class ReauditClassifier {
    public enum Decision {
        /** Source evidence cannot be read; nothing is stored and nothing is queued. */
        UNAVAILABLE,
        /** No usable stored fingerprint; store the current one without re-auditing. */
        BASELINE,
        /** Evidence unchanged. */
        SKIP,
        /** Evidence changed since the stored fingerprint was recorded. */
        REAUDIT
    }

    private ReauditClassifier() {}

    public static Decision classify(Optional<String> currentFingerprint, String storedFingerprint) {
        if (currentFingerprint.isEmpty()) {
            return Decision.UNAVAILABLE;
        }
        if (StringUtil.isEmpty(storedFingerprint) || !EvidenceFingerprinter.isCurrentVersion(storedFingerprint)) {
            return Decision.BASELINE;
        }
        return storedFingerprint.equals(currentFingerprint.get()) ? Decision.SKIP : Decision.REAUDIT;
    }
}
