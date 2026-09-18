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
package com.fortify.cli.aviator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.config.LanguagesCommentConfig;

class FileUtilTest {

    @BeforeAll
    static void initializeCommentConfig() {
        LanguagesCommentConfig commentConfig = new LanguagesCommentConfig();
        commentConfig.setLineCommentSymbols(Map.of(
                "HTML", "<!--",
                "JAVA", "//",
                "PLSQL", "--",
                "SQL", "--"
        ));
        LanguageCommentMapperUtil.initializeConfig(commentConfig);
    }

    /**
     * Phase 2 "make the hash check work": both the audit side and the apply side must hash the
     * same canonical form regardless of line-ending style or a trailing newline on disk.
     */
    @Test
    void canonicalizeForHashNormalizesLineEndingsAndStripsTrailingNewline() {
        String result = FileUtil.canonicalizeForHash("a\r\nb\r\n");

        assertEquals("a\nb", result);
    }

    @Test
    void canonicalizeForHashLeavesContentWithoutTrailingNewlineUnchanged() {
        String result = FileUtil.canonicalizeForHash("a\nb");

        assertEquals("a\nb", result);
    }

    /**
     * Apply-side protection: synthetic "// L<N>" markers left in NewCode (e.g. because the
     * write side generated this remediations.xml before/without the write-side strip) must never
     * reach the source file.
     */
    @Test
    void stripSyntheticLineMarkersRemovesJavaLineNumberMarkers() {
        String result = FileUtil.stripSyntheticLineMarkers("line1 // L1\nline2 // L22\nline3", "Example.java");

        assertEquals("line1\nline2\nline3", result);
    }

    @Test
    void stripSyntheticLineMarkersLeavesMarkerFreeContentUnchanged() {
        String result = FileUtil.stripSyntheticLineMarkers("line1\nline2", "Example.java");

        assertEquals("line1\nline2", result);
    }
}
