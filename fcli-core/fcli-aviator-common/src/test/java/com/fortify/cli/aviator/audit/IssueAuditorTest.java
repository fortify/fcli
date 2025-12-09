/*
 * Copyright 2021-2025 Open Text.
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.model.FilterSelection;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.Filter;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.util.FprHandle;


class IssueAuditorTest {

    private Path tempFprFile;
    private FprHandle fprHandle;

    @BeforeEach
    void setup() throws IOException {
        // 1. Create a temporary dummy FPR file (ZIP format)
        tempFprFile = Files.createTempFile("test_aviator", ".fpr");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {

            // A. Add a minimal audit.fvdl
            ZipEntry entry = new ZipEntry("audit.fvdl");
            zos.putNextEntry(entry);
            String minimalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><FVDL><UUID>test-uuid</UUID><Build><BuildID>test-build</BuildID></Build></FVDL>";
            zos.write(minimalXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // B. Add src-archive/index.xml to suppress the warning
            ZipEntry indexEntry = new ZipEntry("src-archive/index.xml");
            zos.putNextEntry(indexEntry);
            String indexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><index></index>";
            zos.write(indexXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        fprHandle = new FprHandle(tempFprFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fprHandle != null) {
            fprHandle.close();
        }
        if (tempFprFile != null) {
            Files.deleteIfExists(tempFprFile);
        }
    }

    @Test
    void testFilterVulnerabilities_LegacySyntaxWithSpaces() throws Exception {

        // Manual Logger Stub (No-op)
        IAviatorLogger dummyLogger = new IAviatorLogger() {
            @Override public void progress(String format, Object... args) {}
            @Override public void info(String format, Object... args) {}
            @Override public void warn(String format, Object... args) {}
            @Override public void error(String format, Object... args) {}
        };

        FPRInfo fprInfo = new FPRInfo(fprHandle);

        FilterTemplate dummyTemplate = new FilterTemplate();
        dummyTemplate.setTagDefinitions(new ArrayList<>());
        // Note: FPRInfo.setFilterTemplate() exists in the provided code
        fprInfo.setFilterTemplate(dummyTemplate);

        // --- 2. SETUP FILTER SET WITH LEGACY SYNTAX ---
        // The "Buggy" Filter String (Legacy syntax: Space in range AND between terms)
        String legacyQuery = "impact:![2.5, 5.0] [Analysis Type]:!SONATYPE";

        Filter filter = new Filter();
        filter.setAction("hide");
        filter.setQuery(legacyQuery);

        FilterSet filterSet = new FilterSet();
        filterSet.setTitle("Regression Test Set");
        filterSet.setFilters(Collections.singletonList(filter));

        FilterSelection selection = new FilterSelection(filterSet, null);

        // Target Issue: High Impact (4.0), Null Analysis Type.
        // Result: Should NOT be hidden.
        Vulnerability targetVuln = new Vulnerability();
        targetVuln.setInstanceID("TARGET_ISSUE");
        targetVuln.setImpact(4.0);

        // Noise Issue: Low Impact (1.0).
        // Result: Should be hidden.
        Vulnerability hiddenVuln = new Vulnerability();
        hiddenVuln.setInstanceID("HIDDEN_ISSUE");
        hiddenVuln.setImpact(1.0);

        List<Vulnerability> inputList = Arrays.asList(targetVuln, hiddenVuln);

        IssueAuditor auditor = new IssueAuditor(
            inputList, null, new HashMap<>(), fprInfo,
            "TestApp", "1.0", selection, dummyLogger
        );

        Method filterMethod = IssueAuditor.class.getDeclaredMethod("filterVulnerabilities", List.class, FilterSet.class);
        filterMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Vulnerability> results = (List<Vulnerability>) filterMethod.invoke(auditor, inputList, filterSet);

        List<String> remainingIds = results.stream()
            .map(Vulnerability::getInstanceID)
            .collect(Collectors.toList());

        assertEquals(1, remainingIds.size(), "Should verify that exactly one issue remains");
        assertTrue(remainingIds.contains("TARGET_ISSUE"),
            "Regression Failed: IssueAuditor hid the target issue. It likely used the Modern parser on a Legacy query string.");
    }
}
