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

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for FilePreview record validation.
 */
class FilePreviewTest {

    @Test
    void validFilePreviewCreatedCorrectly() {
        FilePreview preview = new FilePreview("Example.java", "/path/to/Example.java", "UTF-8", List.of());
        
        assertNotNull(preview);
        assertEquals("Example.java", preview.filename());
        assertEquals("/path/to/Example.java", preview.path());
        assertEquals("UTF-8", preview.encoding());
        assertEquals(0, preview.totalChanges());
    }

    @Test
    void nullFilenameThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FilePreview(null, "/path", "UTF-8", List.of()));
    }

    @Test
    void blankFilenameThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FilePreview("  ", "/path", "UTF-8", List.of()));
    }

    @Test
    void nullChangesListIsConvertedToEmptyList() {
        FilePreview preview = new FilePreview("Test.java", "/path", "UTF-8", null);
        assertNotNull(preview.changes());
        assertEquals(0, preview.changes().size());
    }

    @Test
    void changesListIsUnmodifiable() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        FileChange change = new FileChange(1, 10, 12, "old", "new", context, false);
        FilePreview preview = new FilePreview("Test.java", "/path", "UTF-8", List.of(change));
        
        assertThrows(UnsupportedOperationException.class, 
            () -> preview.changes().add(new FileChange(2, 20, 22, "old2", "new2", context, false)));
    }

    @Test
    void totalChangesReturnsCorrectCount() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        List<FileChange> changes = List.of(
            new FileChange(1, 10, 12, "old1", "new1", context, false),
            new FileChange(2, 20, 22, "old2", "new2", context, false),
            new FileChange(3, 30, 32, "old3", "new3", context, false)
        );
        FilePreview preview = new FilePreview("Test.java", "/path", "UTF-8", changes);
        
        assertEquals(3, preview.totalChanges());
    }
}
