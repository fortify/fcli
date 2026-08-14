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
package com.fortify.cli.aviator.diagnose;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Shared helpers for diagnostic evidence JSON (no orchestration dependency).
 */
public final class AviatorDiagnosticEvidence {
    private AviatorDiagnosticEvidence() {}

    public static ObjectNode empty() {
        return JsonHelper.getObjectMapper().createObjectNode();
    }

    public static ObjectNode errorEvidence(Exception e) {
        var evidence = empty();
        if (e == null) {
            return evidence;
        }
        evidence.put("exceptionType", e.getClass().getName());
        evidence.put("exceptionMessage", e.getMessage());
        var cause = e.getCause();
        if (cause != null) {
            evidence.put("causeType", cause.getClass().getName());
            evidence.put("causeMessage", cause.getMessage());
            var nested = cause.getCause();
            if (nested != null) {
                evidence.put("rootCauseType", nested.getClass().getName());
                evidence.put("rootCauseMessage", nested.getMessage());
            }
        }
        return evidence;
    }

    public static void merge(ObjectNode target, ObjectNode source) {
        if (target == null || source == null) {
            return;
        }
        source.fields().forEachRemaining(entry -> target.set(entry.getKey(), entry.getValue()));
    }
}
