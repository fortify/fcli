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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.VulnerabilityFilterer;
import com.fortify.cli.aviator.util.FprHandle;

class StreamingFVDLProcessorTest {
    private Path tempFprFile;
    private FprHandle fprHandle;

    @AfterEach
    void tearDown() throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
        }
        if (tempFprFile != null) {
            Files.deleteIfExists(tempFprFile);
        }
    }

    @Test
    void testParseUsesRunEngineNameAndIgnoresEngineDataPropertiesForAnalysisType() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <EngineData>
                    <Properties type="WEBINSPECT">
                      <Property>
                        <name>com.fortify.example</name>
                        <value>value</value>
                      </Property>
                    </Properties>
                  </EngineData>
                  <Run>
                    <EngineName>PTA</EngineName>
                  </Run>
                  <Vulnerabilities/>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        assertEquals("PTA", processor.getFvdlMetadata().getAnalysisType());
    }

    @Test
    void testParsePreservesRuleMetadataWithoutTracesAndKeepsDefaultAnalysisType() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <EngineData>
                    <RuleInfo>
                      <Rule id="RULE-1">
                        <MetaInfo>
                          <Group name="CWE">79</Group>
                          <Group name="Category">Cross-Site Scripting</Group>
                        </MetaInfo>
                      </Rule>
                    </RuleInfo>
                  </EngineData>
                  <Vulnerabilities>
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <Kingdom>Dataflow</Kingdom>
                        <Type>Cross-Site Scripting</Type>
                        <Subtype>Reflected</Subtype>
                        <AnalyzerName>Dataflow</AnalyzerName>
                        <DefaultSeverity>3.0</DefaultSeverity>
                      </ClassInfo>
                      <InstanceInfo>
                        <InstanceID>instance-1</InstanceID>
                        <InstanceSeverity>3.0</InstanceSeverity>
                        <Confidence>4.0</Confidence>
                      </InstanceInfo>
                    </Vulnerability>
                  </Vulnerabilities>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        assertEquals(1, processor.getVulnerabilities().size());

        Vulnerability vulnerability = processor.getVulnerabilities().get(0);
        assertEquals("SCA", vulnerability.getAnalysisType());
        assertEquals("79", vulnerability.getKnowledge().get("CWE"));
        assertEquals("Cross-Site Scripting", vulnerability.getKnowledge().get("Category"));
        assertNotNull(vulnerability.getKnowledge());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "cwe:79").size());
    }

    @Test
    void testParseExposesMinVirtualCallConfidenceForFiltering() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Vulnerabilities>
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <AnalyzerName>Dataflow</AnalyzerName>
                        <Type>Cross-Site Scripting</Type>
                        <Subtype>Reflected</Subtype>
                        <DefaultSeverity>3.0</DefaultSeverity>
                      </ClassInfo>
                      <InstanceInfo MinVirtualCallConfidence="0.58">
                        <InstanceID>instance-1</InstanceID>
                        <InstanceSeverity>3.0</InstanceSeverity>
                        <Confidence>4.0</Confidence>
                      </InstanceInfo>
                    </Vulnerability>
                  </Vulnerabilities>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        Vulnerability vulnerability = processor.getVulnerabilities().get(0);
        assertEquals(0.58, vulnerability.getMinVirtualCallConfidence());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "virtconf:0.58").size());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "maxVirtConf:0.60").size());
    }

      @Test
      void testCreatePackageNameMatchesModelSemantics() {
        String filename = "leffe/rules/runtime/hybrid/head/demo/dotnet/CommerceAD/App_Code/Components/CustomersDB.cs";
        assertEquals(
          "leffe.rules.runtime.hybrid.head.demo.dotnet.CommerceAD.App_Code.Components",
          StreamingFVDLProcessor.createPackageName(filename)
        );
      }

    @Test
    void testParseExposesContextBackedAttributesForFiltering() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Vulnerabilities>
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <Kingdom>Dataflow</Kingdom>
                        <Type>SQL Injection</Type>
                        <Subtype>Dynamic</Subtype>
                        <AnalyzerName>Dataflow</AnalyzerName>
                        <DefaultSeverity>3.0</DefaultSeverity>
                      </ClassInfo>
                      <InstanceInfo>
                        <InstanceID>instance-1</InstanceID>
                        <InstanceSeverity>3.0</InstanceSeverity>
                        <Confidence>4.0</Confidence>
                      </InstanceInfo>
                      <AnalysisInfo>
                        <Unified>
                          <Context>
                            <Function name="handleRequest" namespace="com.example.issue" enclosingClass="IssueController"/>
                          </Context>
                          <Trace>
                            <Primary>
                              <Entry><NodeRef id="0"/></Entry>
                              <Entry><NodeRef id="1"/></Entry>
                            </Primary>
                          </Trace>
                        </Unified>
                      </AnalysisInfo>
                    </Vulnerability>
                  </Vulnerabilities>
                  <ContextPool>
                    <Context id="1">
                      <Function name="readInput" namespace="com.example.source" enclosingClass="SourceController"/>
                    </Context>
                    <Context id="2">
                      <Function name="executeQuery" namespace="com.example.sink" enclosingClass="SqlSink"/>
                    </Context>
                  </ContextPool>
                  <UnifiedNodePool>
                    <Node id="0">
                      <SourceLocation path="src/main/java/com/example/source/SourceController.java" line="10" lineEnd="10" colStart="0" colEnd="0" contextId="1"/>
                      <Action type="Assign">Assignment to input</Action>
                    </Node>
                    <Node id="1">
                      <SourceLocation path="src/main/java/com/example/sink/SqlSink.java" line="42" lineEnd="42" colStart="0" colEnd="0" contextId="2"/>
                      <Action type="InCall">executeQuery(0)</Action>
                    </Node>
                  </UnifiedNodePool>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        Vulnerability vulnerability = processor.getVulnerabilities().get(0);
        assertEquals("com.example.issue", vulnerability.getPackageName());
        assertEquals("IssueController", vulnerability.getClassName());
        assertEquals("com.example.source.SourceController.readInput()", vulnerability.getSourceFunction());
        assertEquals("com.example.sink.SqlSink.executeQuery()", vulnerability.getSinkFunction());
        assertEquals("com.example.source.SourceController.readInput", vulnerability.getSourceContext());
        assertEquals("com.example.sink.SqlSink.executeQuery", vulnerability.getSinkContext());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "package:com.example.issue").size());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "class:IssueController").size());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "source:readInput").size());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "sink:executeQuery").size());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "[source context]:com.example.source.SourceController.readInput").size());
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "[sink context]:com.example.sink.SqlSink.executeQuery").size());
    }

    @Test
    void testParseUsesFirstTraceForSourceContextAndLastTraceForSinkContext() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Vulnerabilities>
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <Kingdom>Dataflow</Kingdom>
                        <Type>SQL Injection</Type>
                        <Subtype>Dynamic</Subtype>
                        <AnalyzerName>Dataflow</AnalyzerName>
                        <DefaultSeverity>3.0</DefaultSeverity>
                      </ClassInfo>
                      <InstanceInfo>
                        <InstanceID>instance-1</InstanceID>
                        <InstanceSeverity>3.0</InstanceSeverity>
                        <Confidence>4.0</Confidence>
                      </InstanceInfo>
                      <AnalysisInfo>
                        <Unified>
                          <Trace>
                            <Primary>
                              <Entry><NodeRef id="0"/></Entry>
                              <Entry><NodeRef id="1"/></Entry>
                            </Primary>
                          </Trace>
                          <Trace>
                            <Primary>
                              <Entry><NodeRef id="2"/></Entry>
                              <Entry><NodeRef id="3"/></Entry>
                            </Primary>
                          </Trace>
                        </Unified>
                      </AnalysisInfo>
                    </Vulnerability>
                  </Vulnerabilities>
                  <ContextPool>
                    <Context id="1">
                      <Function name="readInput" namespace="com.example.source" enclosingClass="SourceController"/>
                    </Context>
                    <Context id="2">
                      <Function name="sanitize" namespace="com.example.mid" enclosingClass="Sanitizer"/>
                    </Context>
                    <Context id="3">
                      <Function name="prepareQuery" namespace="com.example.mid" enclosingClass="QueryBuilder"/>
                    </Context>
                    <Context id="4">
                      <Function name="executeQuery" namespace="com.example.sink" enclosingClass="SqlSink"/>
                    </Context>
                  </ContextPool>
                  <UnifiedNodePool>
                    <Node id="0">
                      <SourceLocation path="src/main/java/com/example/source/SourceController.java" line="10" lineEnd="10" colStart="0" colEnd="0" contextId="1"/>
                      <Action type="Assign">Assignment to input</Action>
                    </Node>
                    <Node id="1">
                      <SourceLocation path="src/main/java/com/example/mid/Sanitizer.java" line="20" lineEnd="20" colStart="0" colEnd="0" contextId="2"/>
                      <Action type="Call">sanitize(0)</Action>
                    </Node>
                    <Node id="2">
                      <SourceLocation path="src/main/java/com/example/mid/QueryBuilder.java" line="30" lineEnd="30" colStart="0" colEnd="0" contextId="3"/>
                      <Action type="Call">prepareQuery(0)</Action>
                    </Node>
                    <Node id="3">
                      <SourceLocation path="src/main/java/com/example/sink/SqlSink.java" line="42" lineEnd="42" colStart="0" colEnd="0" contextId="4"/>
                      <Action type="InCall">executeQuery(0)</Action>
                    </Node>
                  </UnifiedNodePool>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        Vulnerability vulnerability = processor.getVulnerabilities().get(0);
        assertEquals("com.example.source.SourceController.readInput", vulnerability.getSourceContext());
        assertEquals("com.example.sink.SqlSink.executeQuery", vulnerability.getSinkContext());
        assertEquals("com.example.source.SourceController.readInput()", vulnerability.getSourceFunction());
        assertEquals("com.example.sink.SqlSink.executeQuery()", vulnerability.getSinkFunction());
    }

    @Test
    void testParseExposesRequestIdFromRequestHeadersForFiltering() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Vulnerabilities>
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <Kingdom>Dataflow</Kingdom>
                        <Type>SQL Injection</Type>
                        <Subtype>Dynamic</Subtype>
                        <AnalyzerName>Dataflow</AnalyzerName>
                        <DefaultSeverity>3.0</DefaultSeverity>
                      </ClassInfo>
                      <InstanceInfo>
                        <InstanceID>instance-1</InstanceID>
                        <InstanceSeverity>3.0</InstanceSeverity>
                        <Confidence>4.0</Confidence>
                      </InstanceInfo>
                      <AnalysisInfo>
                        <Unified>
                          <AuxiliaryData contentType="RequestHeaders">
                            <AuxField name="Host" value="example.com"/>
                            <AuxField name="X-Scan-Memo" value="Category=&quot;Crawl&quot;; SID=&quot;ABC123&quot;; SessionType=&quot;ExternalAddedToCrawl&quot;;"/>
                          </AuxiliaryData>
                        </Unified>
                      </AnalysisInfo>
                    </Vulnerability>
                  </Vulnerabilities>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        Vulnerability vulnerability = processor.getVulnerabilities().get(0);
        assertEquals("ABC123", vulnerability.getAttributeValue("requestid"));
        assertEquals(1, VulnerabilityFilterer.filter(processor.getVulnerabilities(), "[request id]:ABC123").size());
    }

    private void createTestFpr(String auditFvdlXml) throws Exception {
        tempFprFile = Files.createTempFile("streaming-fvdl", ".fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {
            zipOutputStream.putNextEntry(new ZipEntry("audit.fvdl"));
            zipOutputStream.write(auditFvdlXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zipOutputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index/>".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        fprHandle = new FprHandle(tempFprFile);
    }
}