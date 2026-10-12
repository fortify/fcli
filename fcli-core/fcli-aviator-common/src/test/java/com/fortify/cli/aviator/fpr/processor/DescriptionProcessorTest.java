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
package com.fortify.cli.aviator.fpr.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.fpr.model.ReplacementData;
import com.fortify.cli.aviator.fpr.model.StreamedDescription;

class DescriptionProcessorTest {
    private static final String CLASS_ID = "class-1";

    @Test
    void testCompiledTemplateRendersReplacementsPerVulnerability() {
        FVDLMetadata metadata = metadataWith(
            "<Content>Data flows from <Replace key=\"Source\"/> to <Replace key=\"Sink\" default=\"a sink\"/>.</Content>",
            "<Content><Paragraph>Source is <Replace key=\"Source\"/>.<AltParagraph>Source is unknown.</AltParagraph></Paragraph></Content>");
        DescriptionProcessor processor = new DescriptionProcessor();

        String[] first = processor.processForVuln(vulnerability(), CLASS_ID, replacements("Source", "request.getParameter"), metadata);
        String[] second = processor.processForVuln(vulnerability(), CLASS_ID, replacements("Source", "System.getenv"), metadata);
        String[] missing = processor.processForVuln(vulnerability(), CLASS_ID, new ReplacementData(), metadata);

        assertArrayEquals(new String[] {
            "Data flows from request.getParameter to a sink.", "Source is request.getParameter."}, first);
        assertArrayEquals(new String[] {
            "Data flows from System.getenv to a sink.", "Source is System.getenv."}, second);
        assertArrayEquals(new String[] {"Data flows from  to a sink.", "Source is unknown."}, missing);
    }

    @Test
    void testCompiledTemplateEvaluatesConditionsPerVulnerability() {
        FVDLMetadata metadata = metadataWith(
            "<IfDef var=\"PrimaryLocation\">Defined</IfDef><IfNotDef var=\"PrimaryLocation\">Not defined</IfNotDef>", "");
        DescriptionProcessor processor = new DescriptionProcessor();
        Vulnerability withVar = vulnerability();
        withVar.getKnowledge().put("PrimaryLocation", "x");

        assertArrayEquals(new String[] {"Defined", ""}, processor.processForVuln(withVar, CLASS_ID, null, metadata));
        assertArrayEquals(new String[] {"Not defined", ""}, processor.processForVuln(vulnerability(), CLASS_ID, null, metadata));
    }

    @Test
    void testClearCompiledTemplatesKeepsRenderingCorrect() {
        FVDLMetadata metadata = metadataWith("<Replace key=\"Source\"/>", "");
        DescriptionProcessor processor = new DescriptionProcessor();

        processor.processForVuln(vulnerability(), CLASS_ID, replacements("Source", "a"), metadata);
        processor.clearCompiledTemplates();

        assertArrayEquals(new String[] {"b", ""},
            processor.processForVuln(vulnerability(), CLASS_ID, replacements("Source", "b"), metadata));
    }

    private static FVDLMetadata metadataWith(String abstractText, String explanation) {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.getDescriptionCache().put(CLASS_ID, StreamedDescription.builder()
            .classID(CLASS_ID).abstractText(abstractText).explanation(explanation).build());
        return metadata;
    }

    private static ReplacementData replacements(String key, String value) {
        ReplacementData data = new ReplacementData();
        data.addReplacement(key, value, null, null, null, null);
        return data;
    }

    private static Vulnerability vulnerability() {
        return Vulnerability.builder().classID(CLASS_ID).instanceID("instance").build();
    }
}
