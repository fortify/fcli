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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;

class DastAuditStreamConfigTest {
    @Test
    void rejectsBlankToken() {
        assertThrows(AviatorSimpleException.class,
            () -> new DastAuditStreamConfig(" ", "app", "ssc", "1", null));
    }

    @Test
    void rejectsBlankApplicationName() {
        assertThrows(AviatorSimpleException.class,
            () -> new DastAuditStreamConfig("token", " ", "ssc", "1", null));
    }
}