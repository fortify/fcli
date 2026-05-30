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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;

@DisplayName("AviatorGrpcUtils")
class AviatorGrpcUtilsTest {
    @Test
    @DisplayName("wraps conversion failures in AviatorTechnicalException")
    void wrapsConversionFailuresInTechnicalException() {
        AviatorTechnicalException exception = assertThrows(AviatorTechnicalException.class, () -> AviatorGrpcUtils.grpcToJsonNode(null));

        assertTrue(exception.getMessage().contains("Failed to convert gRPC message to JSON"));
        assertTrue(exception.getCause() instanceof NullPointerException);
    }
}