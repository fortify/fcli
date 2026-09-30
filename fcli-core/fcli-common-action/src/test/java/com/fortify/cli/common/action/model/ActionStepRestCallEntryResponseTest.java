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
package com.fortify.cli.common.action.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.action.model.Action.ActionMetadata;
import com.fortify.cli.common.action.model.ActionStepRestCallEntry.ActionStepRestCallResponseType;
import com.fortify.cli.common.spel.wrapper.TemplateExpressionKeyDeserializer;

class ActionStepRestCallEntryResponseTest {
    private static final ObjectMapper YAML_MAPPER = TemplateExpressionKeyDeserializer.registerOn(new ObjectMapper(new YAMLFactory()));

    @Test
    void responseTypeDefaultsToJson() throws Exception {
        var entry = loadRestCallEntry("");
        assertEquals(ActionStepRestCallResponseType.json, entry.getResponseType());
    }

    @Test
    void fileResponseTypeWithFileIsAccepted() throws Exception {
        var entry = loadRestCallEntry("""
                        response.type: file
                        response.file: out/${cli.id}.fpr
                """);
        assertEquals(ActionStepRestCallResponseType.file, entry.getResponseType());
        assertNotNull(entry.getResponseFile());
    }

    @Test
    void textResponseTypeIsAccepted() throws Exception {
        assertEquals(ActionStepRestCallResponseType.text, loadRestCallEntry("        response.type: text\n").getResponseType());
    }

    @Test
    void fileResponseTypeRequiresFile() {
        assertInvalid("        response.type: file\n", "response.file is required when response.type is file");
    }

    @Test
    void fileOnlyAllowedWithFileResponseType() {
        assertInvalid("""
                        response.type: text
                        response.file: x.txt
                """, "response.file is only allowed when response.type is file");
        assertInvalid("        response.file: x.txt\n", "response.file is only allowed when response.type is file");
    }

    @Test
    void fileResponseTypeRejectsPagedRequests() {
        assertInvalid("""
                        response.type: file
                        response.file: x.bin
                        type: paged
                """, "response.type file cannot be used with paged requests");
    }

    @Test
    void textResponseTypeRejectsLogProgress() {
        assertInvalid("""
                        response.type: text
                        log.progress:
                          page.post-load: loaded
                """, "response.type text cannot be used with paged requests");
    }

    @Test
    void fileResponseTypeRejectsRecordsForEach() {
        assertInvalid("""
                        response.type: file
                        response.file: x.bin
                        records.for-each:
                          record.var-name: r
                          do:
                            - log.debug: ${r}
                """, "records.for-each requires a JSON array response; not supported with response.type file");
    }

    private void assertInvalid(String properties, String expectedMessage) {
        var e = assertThrows(Exception.class, () -> loadRestCallEntry(properties));
        var message = getMessages(e);
        assertTrue(message.contains(expectedMessage), message);
    }

    private static String getMessages(Throwable t) {
        var sb = new StringBuilder();
        for ( var current = t; current!=null; current = current.getCause() ) {
            sb.append(current.getMessage()).append('\n');
        }
        return sb.toString();
    }

    private ActionStepRestCallEntry loadRestCallEntry(String extraProperties) throws Exception {
        var yaml = """
                usage:
                  header: Test
                  description: Test
                steps:
                  - rest.call:
                      dl:
                        target: fod
                        uri: /api/v3/test
                """ + extraProperties;
        var action = YAML_MAPPER.readValue(yaml, Action.class);
        action.postLoad(ActionMetadata.create(true));
        return action.getSteps().get(0).getRestCalls().get("dl");
    }
}
