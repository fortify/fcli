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
package com.fortify.cli.aviator.fpr.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.config.LanguagesCommentConfig;
import com.fortify.cli.aviator.util.LanguageCommentMapperUtil;

class FileUtilsTest {

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

    @Test
    void shouldUseResolvedLanguageWhenAppendingLineNumbers() {
        FileUtils fileUtils = new FileUtils();

        String result = fileUtils.appendLineNumbers("line one\nline two", "db/t1.sql", 0, "JAVA");

        assertEquals("line one // L1" + System.lineSeparator() + "line two // L2", result);
    }

    @Test
    void shouldUseResolvedLanguageForBlockCommentSyntax() {
        FileUtils fileUtils = new FileUtils();

        String result = fileUtils.appendLineNumbers("<div/>", "db/t1.sql", 0, "HTML");

        assertEquals("<div/> <!-- L1 -->", result);
    }
}