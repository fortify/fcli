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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AviatorSignatureUtilsTest {
    private static final String METHOD = "com.fortify.aviator.application.ApplicationService/ReduceEntitlement";
    private static final String OPERATION_ID = "0190a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b";

    @Test
    void operationMessageBindsMethodAndOperationIdBeforeTheTimestamp() {
        String[] parts = AviatorSignatureUtils.createOperationMessage(METHOD, OPERATION_ID, "tenant", "101").split(";");

        assertEquals(5, parts.length);
        assertEquals("tenant", parts[0]);
        assertEquals("101", parts[1]);
        assertEquals(METHOD, parts[2]);
        assertEquals(OPERATION_ID, parts[3]);
        assertTrue(Instant.parse(parts[4]).isBefore(Instant.now().plusSeconds(5)));
    }

    @Test
    void messageWithoutOperationIdKeepsTheLegacyShape() {
        String message = AviatorSignatureUtils.createOperationMessage(METHOD, null, "tenant", "101");
        String[] parts = message.split(";");

        assertEquals(3, parts.length);
        assertFalse(message.contains(METHOD));
        Instant.parse(parts[2]);
    }

    @Test
    void differentMethodsOrOperationIdsNeverProduceTheSamePrefix() {
        String first = AviatorSignatureUtils.createOperationMessage(METHOD, OPERATION_ID, "tenant", "101");
        String otherMethod = AviatorSignatureUtils.createOperationMessage("x.Service/Other", OPERATION_ID, "tenant", "101");
        String otherId = AviatorSignatureUtils.createOperationMessage(METHOD, "0190a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5c", "tenant", "101");

        assertFalse(first.startsWith(otherMethod.substring(0, otherMethod.lastIndexOf(';'))));
        assertFalse(first.startsWith(otherId.substring(0, otherId.lastIndexOf(';'))));
    }
}
