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
package com.fortify.cli.aviator._common.cli.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import picocli.CommandLine.TypeConversionException;

/**
 * Only the CLI adapter layer: domain factory exceptions become {@link TypeConversionException}.
 * Happy-path token semantics are covered by {@code SourceDecodersTest}.
 */
class SourceDecoderConverterTest {

    private final SourceDecoderConverter converter = new SourceDecoderConverter();

    @Test
    void convert_delegatesHappyPathToFactory() {
        assertEquals("FPR", converter.convert("FPR").describe());
        assertEquals("UTF-8", converter.convert("UTF-8").describe());
    }

    @Test
    void convert_mapsFactoryFailuresToTypeConversionException() {
        assertThrows(TypeConversionException.class, () -> converter.convert("  "));
        assertThrows(TypeConversionException.class, () -> converter.convert("NOT-A-REAL-CHARSET"));
    }
}
