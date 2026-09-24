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
package com.fortify.cli.aviator.fpr.remediation.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.exception.AviatorBugException;

class PreviewDtoTest {
    @Test
    void contextMetadataValidatesCountsAndNormalizesNullContent() {
        assertThrows(AviatorBugException.class, () -> new ContextMetadata(-1, 0, "context"));
        assertThrows(AviatorBugException.class, () -> new ContextMetadata(0, -1, "context"));
        assertEquals("", new ContextMetadata(0, 0, null).content());
    }

    @Test
    void previewFileChangeValidatesIdentityRangeAndContext() {
        ContextMetadata context = new ContextMetadata(1, 1, "context");

        assertThrows(AviatorBugException.class,
            () -> new PreviewFileChange(0, 10, 12, "old", "new", context));
        assertThrows(AviatorBugException.class,
            () -> new PreviewFileChange(1, 12, 10, "old", "new", context));
        assertThrows(AviatorBugException.class,
            () -> new PreviewFileChange(1, 10, 12, "old", "new", null));
    }

    @Test
    void previewFileChangeBuilderPreservesValues() {
        ContextMetadata context = new ContextMetadata(2, 2, "context line");
        PreviewFileChange change = PreviewFileChange.builder()
                .changeIndex(1)
                .lineFrom(10)
                .lineTo(12)
                .originalCode("old code")
                .newCode("new code")
                .context(context)
                .build();

        assertEquals(1, change.changeIndex());
        assertEquals(context, change.context());
    }

    @Test
    void filePreviewNormalizesAndProtectsChanges() {
        assertThrows(AviatorBugException.class, () -> new FilePreview(" ", "UTF-8", List.of()));
        assertEquals(List.of(), new FilePreview("Example.java", "UTF-8", null).changes());

        PreviewFileChange change = new PreviewFileChange(1, 1, 1, "old", "new",
                new ContextMetadata(0, 0, "old"));
        FilePreview preview = new FilePreview("Example.java", "UTF-8", List.of(change));
        assertEquals(1, preview.totalChanges());
        assertThrows(UnsupportedOperationException.class, () -> preview.changes().add(change));
    }

    @Test
    void previewDetailFactoriesAndFilesAreStable() {
        Map<String, FilePreview> files = new LinkedHashMap<>();
        files.put("Example.java", new FilePreview("Example.java", "UTF-8", List.of()));

        PreviewDetail available = PreviewDetail.available("ISSUE-1", "description", files);
        PreviewDetail skipped = PreviewDetail.skipped("ISSUE-2", null);

        assertTrue(available.isAvailable());
        assertTrue(skipped.isSkipped());
        assertEquals(Map.of(), skipped.files());
        assertThrows(UnsupportedOperationException.class,
            () -> available.files().put("Other.java", new FilePreview("Other.java", "UTF-8", List.of())));
        assertThrows(AviatorBugException.class,
            () -> new PreviewDetail("", "available", null, Map.of()));
    }
}