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
package com.fortify.cli.common.fpr.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.fpr.Vulnerability;
import com.fortify.cli.common.fpr.model.FVDLMetadata;
import com.fortify.cli.common.fpr.model.ReplacementData;
import com.fortify.cli.common.fpr.model.StreamedDescription;

class DescriptionProcessorTest {
    private static final String CLASS_ID = "class-1";

    @Test
    void testPrivateFallbackSignalDoesNotCaptureStackTrace() throws Exception {
        // Verify the allocation optimization without exposing this implementation detail in production APIs.
        var type = Class.forName(DescriptionProcessor.class.getName() + "$MissingReplacementException");
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        RuntimeException signal = (RuntimeException) constructor.newInstance();
        assertEquals(0, signal.getStackTrace().length);
        signal.fillInStackTrace();
        assertEquals(0, signal.getStackTrace().length);
    }

    @Test
    void testMissingReplacementDiscardsPartialParagraphAndRendersAlternative() {
        FVDLMetadata metadata = metadataWith(
            "before<Paragraph>discard <Replace key=\"Source\"/> then <Replace key=\"Missing\"/>"
                + "<AltParagraph>fallback <Replace key=\"Source\"/>:<Replace key=\"Missing\"/></AltParagraph>"
                + "</Paragraph>after", "");
        DescriptionProcessor processor = new DescriptionProcessor();
        assertArrayEquals(new String[] {"beforefallback value:after", ""},
            processor.processForVuln(vulnerability(), CLASS_ID, replacements("Source", "value"), metadata));
        assertArrayEquals(new String[] {"beforefallback :after", ""},
            processor.processForVuln(vulnerability(), CLASS_ID, null, metadata));
    }

    @Test
    void testMissingReplacementWithoutAlternativeKeepsSurroundingText() {
        FVDLMetadata metadata = metadataWith(
            "before<Paragraph>discard <Replace key=\"Missing\"/></Paragraph>after",
            "outside:<Replace key=\"Missing\"/>!");
        assertArrayEquals(new String[] {"beforeafter", "outside:!"},
            new DescriptionProcessor().processForVuln(vulnerability(), CLASS_ID, new ReplacementData(), metadata));
    }

    @Test
    void testDefaultsAndPresentEmptyOrNullValuesDoNotSelectAlternative() {
        FVDLMetadata metadata = metadataWith(
            "<Paragraph>[<Replace key=\"Source\" default=\"default\"/>]"
                + "<AltParagraph>fallback</AltParagraph></Paragraph>",
            "<Paragraph>[<Replace key=\"Source\"/>]<AltParagraph>fallback</AltParagraph></Paragraph>");
        DescriptionProcessor processor = new DescriptionProcessor();
        assertArrayEquals(new String[] {"[default]", "fallback"},
            processor.processForVuln(vulnerability(), CLASS_ID, null, metadata));
        assertArrayEquals(new String[] {"[]", "[]"},
            processor.processForVuln(vulnerability(), CLASS_ID, replacements("Source", ""), metadata));
        assertArrayEquals(new String[] {"[default]", "[]"},
            processor.processForVuln(vulnerability(), CLASS_ID, replacements("Source", null), metadata));
    }

    @Test
    void testLocationReplacementAndConditionalMissingReplacementKeepFallbackSemantics() {
        FVDLMetadata metadata = metadataWith(
            "<Paragraph><Replace key=\"Source\" link=\"true\"/><AltParagraph>fallback</AltParagraph></Paragraph>",
            "<Paragraph>prefix<IfDef var=\"PrimaryLocation\"><Replace key=\"Missing\"/></IfDef>suffix"
                + "<AltParagraph>fallback</AltParagraph></Paragraph>");
        ReplacementData data = new ReplacementData();
        data.addLocationReplacement("Source", Map.of("path", "src/App.java", "line", "42"));
        DescriptionProcessor processor = new DescriptionProcessor();
        assertArrayEquals(new String[] {"src/App.java (at line 42)", "prefixsuffix"},
            processor.processForVuln(vulnerability(), CLASS_ID, data, metadata));
        Vulnerability withCondition = vulnerability();
        withCondition.getKnowledge().put("PrimaryLocation", "x");
        assertArrayEquals(new String[] {"src/App.java (at line 42)", "fallback"},
            processor.processForVuln(withCondition, CLASS_ID, data, metadata));
    }

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
