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
package com.fortify.cli.aviator.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;

class IssueObjBuilderTest {

    @Test
    void shouldUseFprSourceTypeForSqlFiles() {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.registerSourceFileType("db/t1.sql", "PLSQL");
        SourceLanguageResolver resolver = new SourceLanguageResolver(metadata);

        Vulnerability vulnerability = Vulnerability.builder()
                .instanceID("ISSUE-1")
                .category("SQL Injection")
                .filetype("SQL")
                .lastStackTraceElement(stackTraceElement("db/t1.sql"))
                .stackTrace(List.of(List.of(stackTraceElement("db/t1.sql"))))
                .build();

        UserPrompt userPrompt = IssueObjBuilder.buildIssueObj(vulnerability, resolver);

        assertEquals("PLSQL", userPrompt.getLanguage());
        assertEquals("sql", userPrompt.getFileExtension());
        assertIterableEquals(List.of("PLSQL"), List.copyOf(userPrompt.getProgrammingLanguages()));
    }

    @Test
    void shouldUseExactFprMatchFromStackTraceWhenLastStackFileDoesNotMatch() {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.registerSourceFileType("db/package_body.pls", "PLSQL");
        SourceLanguageResolver resolver = new SourceLanguageResolver(metadata);

        Vulnerability vulnerability = Vulnerability.builder()
                .instanceID("ISSUE-2")
                .category("Data Flow")
                .filetype("SQL")
                .lastStackTraceElement(stackTraceElement("db/unknown.sql"))
                .stackTrace(List.of(List.of(stackTraceElement("db/package_body.pls"))))
                .build();

        UserPrompt userPrompt = IssueObjBuilder.buildIssueObj(vulnerability, resolver);

        assertEquals("PLSQL", userPrompt.getLanguage());
        assertEquals("pls", userPrompt.getFileExtension());
        assertIterableEquals(List.of("PLSQL"), List.copyOf(userPrompt.getProgrammingLanguages()));
    }

    private static StackTraceElement stackTraceElement(String fileName) {
        return new StackTraceElement(fileName, 1, null, null, null, null, null);
    }
}