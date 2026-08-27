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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DastAuditStreamResultTest {
    @Test
    void defensivelyCopiesResults() {
        var source = new ArrayList<DastAuditResult>();
        DastAuditStreamResult result = DastAuditStreamResult.builder().results(source).build();

        source.add(DastAuditResult.Skipped.builder().issueId("DAST-1").build());

        assertEquals(List.of(), result.results());
        assertThrows(UnsupportedOperationException.class,
            () -> result.results().add(DastAuditResult.Skipped.builder().issueId("DAST-2").build()));
    }

    @Test
    void normalizesNullResultsToEmptyList() {
        DastAuditStreamResult result = DastAuditStreamResult.builder().build();

        assertEquals(List.of(), result.results());
    }
}
