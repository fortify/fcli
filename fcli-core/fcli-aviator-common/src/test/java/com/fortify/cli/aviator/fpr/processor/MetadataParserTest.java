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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.model.FVDLMetadata;

class MetadataParserTest {
    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    private final MetadataParser metadataParser = new MetadataParser();

    @Test
    void testParseEngineDataExtractsRuleMetadataWithoutChangingDefaultAnalysisType() throws Exception {
        String xml = """
                <EngineData>
                  <EngineVersion>23.2.0.0123</EngineVersion>
                  <Properties type=\"WEBINSPECT\">
                    <Property>
                      <name>com.fortify.example</name>
                      <value>value</value>
                    </Property>
                  </Properties>
                  <RuleInfo>
                    <Rule id=\"RULE-1\">
                      <MetaInfo>
                        <Group name=\"Category\">Cross-Site Scripting</Group>
                      </MetaInfo>
                    </Rule>
                  </RuleInfo>
                </EngineData>
                """;

        FVDLMetadata fvdlMetadata = parseEngineData(xml);

        assertEquals("23.2.0.0123", fvdlMetadata.getEngineVersion());
        assertEquals("SCA", fvdlMetadata.getAnalysisType());
        assertEquals("Cross-Site Scripting", fvdlMetadata.getRuleMetadata().get("RULE-1").get("Category"));
    }

    @Test
    void testParseEngineDataIgnoresEngineDataPropertyGroupsForAnalysisType() throws Exception {
        String xml = """
                <EngineData>
                  <Properties type="System">
                    <Property>
                      <name>java.version</name>
                      <value>17</value>
                    </Property>
                  </Properties>
                  <Properties type="SCA">
                    <Property>
                      <name>com.fortify.example</name>
                      <value>value</value>
                    </Property>
                  </Properties>
                  <Properties type="Fortify">
                    <Property>
                      <name>com.fortify.locale</name>
                      <value>en</value>
                    </Property>
                  </Properties>
                </EngineData>
                """;

        FVDLMetadata fvdlMetadata = parseEngineData(xml);

        assertEquals("SCA", fvdlMetadata.getAnalysisType());
    }

    private FVDLMetadata parseEngineData(String xml) throws Exception {
        FVDLMetadata fvdlMetadata = new FVDLMetadata();
        var reader = xmlInputFactory.createXMLStreamReader(new StringReader(xml));
        reader.nextTag();
        metadataParser.parseEngineData(reader, fvdlMetadata);
        reader.close();
        return fvdlMetadata;
    }
}