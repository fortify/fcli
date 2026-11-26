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
package com.fortify.cli.aviator.grpc;

import java.util.Map;
import java.util.Objects;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.common.exception.AbstractFcliException;

/**
 * Terminal SAST stream state and a snapshot of received responses, keyed by issue ID.
 * Completion describes the stream, not whether every issue was audited successfully.
 * Local implementation failures, interruption and quota filtering remain exceptional.
 */
public sealed interface SastStreamOutcome {
    Map<String, AuditResponse> responses();

    /** The stream completed normally; individual responses may still report failed or skipped audits. */
    record Completed(Map<String, AuditResponse> responses) implements SastStreamOutcome {
        public Completed {
            responses = Map.copyOf(responses);
        }
    }

    /** The stream terminated unsuccessfully, retaining the domain failure and its original cause chain. */
    record Failed(Map<String, AuditResponse> responses, AbstractFcliException failure) implements SastStreamOutcome {
        public Failed {
            responses = Map.copyOf(responses);
            Objects.requireNonNull(failure, "failure");
        }
    }
}
