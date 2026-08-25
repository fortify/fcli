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
        FilePreview preview = new FilePreview("/path/to/Example.java", "UTF-8", List.of());
        
        assertNotNull(preview);
        assertEquals("/path/to/Example.java", preview.path());
        assertEquals("UTF-8", preview.encoding());
        assertEquals(0, preview.totalChanges());
    }

    @Test
    void nullPathThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FilePreview(null, "UTF-8", List.of()));
    }

    @Test
    void blankPathThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FilePreview("  ", "UTF-8", List.of()));
    }

    @Test
    void nullChangesListIsConvertedToEmptyList() {
        FilePreview preview = new FilePreview("/path", "UTF-8", null);
        assertNotNull(preview.changes());
        assertEquals(0, preview.changes().size());
    }

    @Test
    void changesListIsUnmodifiable() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        FileChange change = FileChange.builder()
                .changeIndex(1).lineFrom(10).lineTo(12)
                .originalCode("old").newCode("new").context(context).build();
        FilePreview preview = new FilePreview("/path", "UTF-8", List.of(change));

        assertThrows(UnsupportedOperationException.class,
            () -> preview.changes().add(FileChange.builder()
                    .changeIndex(2).lineFrom(20).lineTo(22)
                    .originalCode("old2").newCode("new2").context(context).build()));
    }

    @Test
    void totalChangesReturnsCorrectCount() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");
        List<FileChange> changes = List.of(
            FileChange.builder().changeIndex(1).lineFrom(10).lineTo(12).originalCode("old1").newCode("new1").context(context).build(),
            FileChange.builder().changeIndex(2).lineFrom(20).lineTo(22).originalCode("old2").newCode("new2").context(context).build(),
            FileChange.builder().changeIndex(3).lineFrom(30).lineTo(32).originalCode("old3").newCode("new3").context(context).build()
        );
        FilePreview preview = new FilePreview("/path", "UTF-8", changes);

        assertEquals(3, preview.totalChanges());
    }
}
