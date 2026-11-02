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
package com.fortify.cli.aviator.audit.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.fpr.filter.TagDefinition;
import com.fortify.cli.common.fpr.processor.FilterTemplateParser;
import com.fortify.cli.common.fpr.util.FprHandle;

class AviatorFilterTemplateCustomizerTest {
    @TempDir
    Path tempDir;

    @Test
    void addsAviatorFodAndAuditorStatusTagDefinitions() throws Exception {
        Path fprPath = tempDir.resolve("test.fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zip.putNextEntry(new ZipEntry("filtertemplate.xml"));
            zip.write("<FilterTemplate xmlns=\"urn:test\" version=\"1\" id=\"template\"><Name>T</Name></FilterTemplate>"
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            AviatorFilterTemplateCustomizer customizer = new AviatorFilterTemplateCustomizer();
            var template = new FilterTemplateParser(fprHandle, customizer).parseFilterTemplate().orElseThrow();

            List<String> ids = template.getTagDefinitions().stream().map(TagDefinition::getId).toList();
            assertEquals(4, ids.size());
            assertTrue(ids.contains(Constants.AVIATOR_PREDICTION_TAG_ID));
            assertTrue(ids.contains(Constants.AVIATOR_STATUS_TAG_ID));
            assertTrue(ids.contains(Constants.FOD_TAG_ID));
            assertTrue(ids.contains(Constants.AUDITOR_STATUS_TAG_ID));
            assertNotNull(customizer.getFilterTemplateDocument());
        }
    }
}
