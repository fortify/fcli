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

import com.fortify.cli.common.fpr.Vulnerability;
import com.fortify.cli.common.fpr.model.FVDLMetadata;
import com.fortify.cli.common.fpr.model.StackTraceElement;

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

    @Test
    void shouldMapFvdlCppToCForPrimaryCSource() {
        SourceLanguageResolver resolver = resolverWithFileType("src/main.c", "CPP");
        Vulnerability vulnerability = vulnerabilityWithFile("src/main.c");

        assertEquals("C", resolver.resolvePrimaryLanguage(vulnerability));
        assertEquals("c", resolver.resolvePrimaryFileExtension(vulnerability));
        assertEquals(Set.of("CPP"), resolver.resolveProgrammingLanguages(vulnerability));
    }

    @Test
    void shouldMapFvdlCppToCppForPrimaryCppSource() {
        SourceLanguageResolver resolver = resolverWithFileType("src/main.cpp", "CPP");
        Vulnerability vulnerability = vulnerabilityWithFile("src/main.cpp");

        assertEquals("C++", resolver.resolvePrimaryLanguage(vulnerability));
        assertEquals("cpp", resolver.resolvePrimaryFileExtension(vulnerability));
    }

    @Test
    void shouldKeepExistingCppLanguageAndNonCOrCppExtensionsUnchanged() {
        assertEquals("C++", resolverWithFileType("src/main.c", "C++")
                .resolvePrimaryLanguage(vulnerabilityWithFile("src/main.c")));
        assertEquals("CPP", resolverWithFileType("src/main.h", "CPP")
                .resolvePrimaryLanguage(vulnerabilityWithFile("src/main.h")));
        assertEquals("CPP", resolverWithFileType("src/main.hpp", "CPP")
                .resolvePrimaryLanguage(vulnerabilityWithFile("src/main.hpp")));
        assertEquals("CPP", resolverWithFileType("src/main.cc", "CPP")
                .resolvePrimaryLanguage(vulnerabilityWithFile("src/main.cc")));
    }

    @Test
    void shouldKeepOtherLanguagesUnchangedForCExtension() {
        assertEquals("JAVA", resolverWithFileType("src/main.c", "JAVA")
                .resolvePrimaryLanguage(vulnerabilityWithFile("src/main.c")));
    }

    @Test
    void shouldKeepCppUnchangedForMissingOrUnmatchedMetadata() {
        Vulnerability noFile = Vulnerability.builder().filetype("CPP").build();
        assertEquals("CPP", new SourceLanguageResolver(new FVDLMetadata()).resolvePrimaryLanguage(noFile));
        assertEquals("CPP", new SourceLanguageResolver(new FVDLMetadata()).resolvePrimaryLanguage(
                Vulnerability.builder().filetype("CPP").lastStackTraceElement(stackTraceElement("src/missing.c")).build()));

        FVDLMetadata conflicting = new FVDLMetadata();
        conflicting.registerSourceFileType("src/a.c", "CPP");
        conflicting.registerSourceFileType("src/b.c", "JAVA");
        assertEquals("CPP", new SourceLanguageResolver(conflicting).resolvePrimaryLanguage(
                Vulnerability.builder().filetype("CPP").lastStackTraceElement(stackTraceElement("src/other.h")).build()));
        assertEquals("CPP", new SourceLanguageResolver(conflicting).resolvePrimaryLanguage(
                Vulnerability.builder().filetype("CPP").lastStackTraceElement(stackTraceElement("src/other.c")).build()));
    }

    private static SourceLanguageResolver resolverWithFileType(String path, String fileType) {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.registerSourceFileType(path, fileType);
        return new SourceLanguageResolver(metadata);
    }

    private static Vulnerability vulnerabilityWithFile(String path) {
        return Vulnerability.builder().lastStackTraceElement(stackTraceElement(path)).build();
    }

    private static StackTraceElement stackTraceElement(String fileName) {
        return new StackTraceElement(fileName, 1, null, null, null, null, null);
    }
}