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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;

class SourceLanguageResolverTest {

    @Test
    void shouldPreferExactFprFileTypeOverWeakerFallbacks() {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.registerSourceFileType("db/package_body.pls", "PLSQL");
        metadata.registerSourceFileType("db/other.sql", "TSQL");

        SourceLanguageResolver resolver = new SourceLanguageResolver(metadata);
        Vulnerability vulnerability = Vulnerability.builder()
                .filetype("SQL")
                .lastStackTraceElement(stackTraceElement("db/missing.sql"))
                .stackTrace(List.of(List.of(stackTraceElement("db/package_body.pls"))))
                .build();

        assertEquals("PLSQL", resolver.resolvePrimaryLanguage(vulnerability));
        assertEquals("pls", resolver.resolvePrimaryFileExtension(vulnerability));
        assertEquals(Set.of("PLSQL"), resolver.resolveProgrammingLanguages(vulnerability));
    }

    @Test
    void shouldUseUnambiguousFprExtensionBeforeVulnerabilityFileType() {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.registerSourceFileType("db/first.sql", "PLSQL");
        metadata.registerSourceFileType("db/second.sql", "PLSQL");

        SourceLanguageResolver resolver = new SourceLanguageResolver(metadata);
        Vulnerability vulnerability = Vulnerability.builder()
                .filetype("SQL")
                .lastStackTraceElement(stackTraceElement("scratch/third.sql"))
                .build();

        assertEquals("PLSQL", resolver.resolvePrimaryLanguage(vulnerability));
        assertEquals("sql", resolver.resolvePrimaryFileExtension(vulnerability));
        assertEquals(Set.of("PLSQL"), resolver.resolveProgrammingLanguages(vulnerability));
    }

    @Test
    void shouldFallBackToYamlExtensionMappingWhenFprAndFileTypeMissing() {
        SourceLanguageResolver resolver = new SourceLanguageResolver(new FVDLMetadata());
        Vulnerability vulnerability = Vulnerability.builder()
                .lastStackTraceElement(stackTraceElement("src/Test.java"))
                .build();

        assertEquals("JAVA", resolver.resolvePrimaryLanguage(vulnerability));
        assertTrue(resolver.resolveProgrammingLanguages(vulnerability).contains("JAVA"));
        assertEquals("java", resolver.resolvePrimaryFileExtension(vulnerability));
    }

    @Test
    void shouldPreserveExactBasenameCaseBeforeFoldedCaseFallback() {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.registerSourceFileType("db/Case.sql", "PLSQL");
        metadata.registerSourceFileType("db/case.sql", "TSQL");

        SourceLanguageResolver resolver = new SourceLanguageResolver(metadata);

        assertEquals("PLSQL", resolver.resolveLanguage("Case.sql", ""));
        assertEquals("TSQL", resolver.resolveLanguage("case.sql", ""));
        assertEquals("PLSQL", resolver.resolveLanguage("CASE.sql", "PLSQL"));
    }

    private static StackTraceElement stackTraceElement(String fileName) {
        return new StackTraceElement(fileName, 1, null, null, null, null, null);
    }
}