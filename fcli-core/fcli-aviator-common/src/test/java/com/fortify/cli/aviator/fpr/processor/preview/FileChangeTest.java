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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.exception.AviatorBugException;

/**
 * Tests for FileChange record validation.
 */
class FileChangeTest {

    @Test
    void validFileChangeCreatedCorrectly() {
        ContextMetadata context = new ContextMetadata(2, 2, "context line");
        FileChange change = FileChange.builder()
                .changeIndex(1)
                .lineFrom(10)
                .lineTo(12)
                .originalCode("old code")
                .newCode("new code")
                .context(context)
                .build();

        assertNotNull(change);
        assertEquals(1, change.changeIndex());
        assertEquals(10, change.lineFrom());
        assertEquals(12, change.lineTo());
        assertEquals("old code", change.originalCode());
        assertEquals("new code", change.newCode());
        assertEquals(context, change.context());
        assertFalse(change.fuzzyMatched());
    }

    @Test
    void changeIndexZeroThrowsException() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        assertThrows(AviatorBugException.class,
            () -> new FileChange(0, 10, 12, "old", "new", context, false));
    }

    @Test
    void changeIndexNegativeThrowsException() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        assertThrows(AviatorBugException.class,
            () -> new FileChange(-1, 10, 12, "old", "new", context, false));
    }

    @Test
    void lineFromZeroThrowsException() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        assertThrows(AviatorBugException.class,
            () -> new FileChange(1, 0, 12, "old", "new", context, false));
    }

    @Test
    void lineFromNegativeThrowsException() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        assertThrows(AviatorBugException.class,
            () -> new FileChange(1, -1, 12, "old", "new", context, false));
    }

    @Test
    void lineToLessThanLineFromThrowsException() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        assertThrows(AviatorBugException.class,
            () -> new FileChange(1, 12, 10, "old", "new", context, false));
    }

    @Test
    void lineToEqualToLineFromIsValid() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        FileChange change = new FileChange(1, 10, 10, "old", "new", context, false);
        
        assertNotNull(change);
        assertEquals(10, change.lineFrom());
        assertEquals(10, change.lineTo());
    }

    @Test
    void nullContextThrowsException() {
        assertThrows(AviatorBugException.class,
            () -> new FileChange(1, 10, 12, "old", "new", null, false));
    }
}
