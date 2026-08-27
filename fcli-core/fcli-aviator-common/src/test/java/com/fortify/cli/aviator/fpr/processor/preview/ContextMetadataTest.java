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
package com.fortify.cli.aviator.fpr.processor.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.exception.AviatorBugException;

/**
 * Tests for ContextMetadata record validation.
 */
class ContextMetadataTest {

    @Test
    void validContextMetadataCreatedCorrectly() {
        ContextMetadata metadata = new ContextMetadata(2, 3, "context content");
        
        assertNotNull(metadata);
        assertEquals(2, metadata.linesBefore());
        assertEquals(3, metadata.linesAfter());
        assertEquals("context content", metadata.content());
    }

    @Test
    void zeroLinesBeforeAndAfterIsValid() {
        ContextMetadata metadata = new ContextMetadata(0, 0, "content");
        
        assertNotNull(metadata);
        assertEquals(0, metadata.linesBefore());
        assertEquals(0, metadata.linesAfter());
    }

    @Test
    void negativeLineBeforeThrowsException() {
        assertThrows(AviatorBugException.class, 
            () -> new ContextMetadata(-1, 2, "content"));
    }

    @Test
    void negativeLinesAfterThrowsException() {
        assertThrows(AviatorBugException.class, 
            () -> new ContextMetadata(2, -1, "content"));
    }

    @Test
    void nullContentConvertedToEmptyString() {
        ContextMetadata metadata = new ContextMetadata(1, 1, null);
        
        assertNotNull(metadata.content());
        assertEquals("", metadata.content());
    }

    @Test
    void emptyContentIsValid() {
        ContextMetadata metadata = new ContextMetadata(0, 0, "");
        
        assertNotNull(metadata);
        assertEquals("", metadata.content());
    }
}
