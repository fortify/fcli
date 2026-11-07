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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.fpr.filter.FilterTemplate;
import com.fortify.cli.common.fpr.filter.TagDefinition;
import com.fortify.cli.common.fpr.processor.FilterTemplateParser;
import com.fortify.cli.common.fpr.util.FprHandle;

class AviatorFilterTemplateCustomizerTest {
    @TempDir
    Path tempDir;

    @Test
    void addsAviatorFodAndAuditorStatusTagDefinitions() throws Exception {
        Path fprPath = tempDir.resolve("test.fpr");
        writeFpr(fprPath, "<FilterTemplate xmlns=\"urn:test\" version=\"1\" id=\"template\"><Name>T</Name></FilterTemplate>");

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

    @Test
    void addsMissingStatusValueToExistingDefinitionAndDoesNotDuplicateIt() throws Exception {
        Path fprPath = tempDir.resolve("legacy.fpr");
        String filterTemplate = """
                <FilterTemplate xmlns="urn:test" version="1" id="template">
                  <Name>T</Name>
                  <TagDefinition id="%s" type="user" extensible="false" hidden="false"
                      objectVersion="0" valueType="LIST">
                    <name>Aviator status</name>
                    <Description/>
                    <value id="0" hidden="false">PROCESSED_BY_AVIATOR</value>
                  </TagDefinition>
                </FilterTemplate>
                """.formatted(Constants.AVIATOR_STATUS_TAG_ID);
        writeFpr(fprPath, filterTemplate);

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            AviatorFilterTemplateCustomizer customizer = new AviatorFilterTemplateCustomizer();
            FilterTemplate template = new FilterTemplateParser(fprHandle, customizer).parseFilterTemplate().orElseThrow();

            assertEquals(List.of(
                    Constants.PROCESSED_BY_AVIATOR,
                    Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION),
                    statusTagDefinition(template).getTagValuesAsString());
            assertEquals(2, statusTagElement(customizer).getElementsByTagNameNS("urn:test", "value").getLength());

            customizer.customize(template, customizer.getFilterTemplateDocument());

            assertEquals(List.of(
                    Constants.PROCESSED_BY_AVIATOR,
                    Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION),
                    statusTagDefinition(template).getTagValuesAsString());
            assertEquals(2, statusTagElement(customizer).getElementsByTagNameNS("urn:test", "value").getLength());
        }
    }

    @Test
    void assignsUnusedIdsWhenExistingStatusValueIdsAreNotSequential() throws Exception {
        Path fprPath = tempDir.resolve("non-sequential-ids.fpr");
        String filterTemplate = """
                <FilterTemplate xmlns="urn:test" version="1" id="template">
                  <Name>T</Name>
                  <TagDefinition id="%s" type="user" extensible="false" hidden="false"
                      objectVersion="0" valueType="LIST">
                    <name>Aviator status</name>
                    <Description/>
                    <value id="0" hidden="false">PROCESSED_BY_AVIATOR</value>
                    <value id="2" hidden="false">LEGACY_STATUS</value>
                  </TagDefinition>
                </FilterTemplate>
                """.formatted(Constants.AVIATOR_STATUS_TAG_ID);
        writeFpr(fprPath, filterTemplate);

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            AviatorFilterTemplateCustomizer customizer = new AviatorFilterTemplateCustomizer();
            FilterTemplate template = new FilterTemplateParser(fprHandle, customizer).parseFilterTemplate().orElseThrow();

            List<String> modelValueIds = statusTagDefinition(template).getValues().stream()
                    .map(value -> value.getId())
                    .toList();
            assertEquals(3, modelValueIds.size());
            assertEquals(3L, modelValueIds.stream().distinct().count());
            assertEquals("1", statusTagDefinition(template).getValues().stream()
                    .filter(value -> Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION.equals(value.getValue()))
                    .findFirst().orElseThrow().getId());

            NodeList xmlValues = statusTagElement(customizer).getElementsByTagNameNS("urn:test", "value");
            assertEquals("1", ((Element) xmlValues.item(2)).getAttribute("id"));
        }
    }

    private void writeFpr(Path fprPath, String filterTemplate) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zip.putNextEntry(new ZipEntry("filtertemplate.xml"));
            zip.write(filterTemplate.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private TagDefinition statusTagDefinition(FilterTemplate template) {
        return template.getTagDefinitions().stream()
                .filter(tag -> Constants.AVIATOR_STATUS_TAG_ID.equalsIgnoreCase(tag.getId()))
                .findFirst()
                .orElseThrow();
    }

    private Element statusTagElement(AviatorFilterTemplateCustomizer customizer) {
        NodeList tags = customizer.getFilterTemplateDocument().getElementsByTagNameNS("urn:test", "TagDefinition");
        for (int i = 0; i < tags.getLength(); i++) {
            Element tag = (Element) tags.item(i);
            if (Constants.AVIATOR_STATUS_TAG_ID.equalsIgnoreCase(tag.getAttribute("id"))) {
                return tag;
            }
        }
        throw new AssertionError("Aviator status tag definition was not found");
    }
}
