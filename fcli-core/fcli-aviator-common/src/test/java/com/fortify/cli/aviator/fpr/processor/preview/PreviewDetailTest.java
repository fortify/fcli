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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.exception.AviatorBugException;

/**
 * Tests for PreviewDetail record validation and factory methods.
 */
class PreviewDetailTest {

    @Test
    void availablePreviewDetailCreatedCorrectly() {
        Map<String, FilePreview> files = Map.of("Example.java", 
            new FilePreview("/path/to/Example.java", "UTF-8", java.util.List.of()));
        PreviewDetail detail = PreviewDetail.available("ISSUE-123", "Remediation rationale", files);
        
        assertNotNull(detail);
        assertEquals("ISSUE-123", detail.issueId());
        assertEquals("available", detail.status());
        assertEquals("Remediation rationale", detail.description());
        assertEquals(1, detail.files().size());
        assertEquals(null, detail.skipReason());
        assertTrue(detail.isAvailable());
    }

    @Test
    void skippedPreviewDetailCreatedCorrectly() {
        PreviewDetail detail = PreviewDetail.skipped("ISSUE-456", null, "Source file missing");
        
        assertNotNull(detail);
        assertEquals("ISSUE-456", detail.issueId());
        assertEquals("skipped", detail.status());
        assertEquals(0, detail.files().size());
        assertEquals("Source file missing", detail.skipReason());
        assertTrue(detail.isSkipped());
    }

    @Test
    void nullIssueIdThrowsException() {
        assertThrows(AviatorBugException.class, 
            () -> new PreviewDetail(null, "available", null, Map.of(), null));
    }

    @Test
    void blankIssueIdThrowsException() {
        assertThrows(AviatorBugException.class, 
            () -> new PreviewDetail("", "available", null, Map.of(), null));
    }

    @Test
    void nullStatusThrowsException() {
        assertThrows(AviatorBugException.class, 
            () -> new PreviewDetail("ISSUE-1", null, null, Map.of(), null));
    }

    @Test
    void blankStatusThrowsException() {
        assertThrows(AviatorBugException.class, 
            () -> new PreviewDetail("ISSUE-1", "   ", null, Map.of(), null));
    }

    @Test
    void nullFilesMapIsConvertedToEmptyMap() {
        PreviewDetail detail = new PreviewDetail("ISSUE-1", "available", null, null, null);
        assertNotNull(detail.files());
        assertEquals(0, detail.files().size());
    }

    @Test
    void filesMapIsUnmodifiable() {
        Map<String, FilePreview> files = new java.util.LinkedHashMap<>();
        files.put("Test.java", new FilePreview("/path", "UTF-8", java.util.List.of()));
        PreviewDetail detail = new PreviewDetail("ISSUE-1", "available", null, files, null);
        
        assertThrows(UnsupportedOperationException.class, 
            () -> detail.files().put("Another.java", new FilePreview("/path2", "UTF-8", java.util.List.of())));
    }
}
