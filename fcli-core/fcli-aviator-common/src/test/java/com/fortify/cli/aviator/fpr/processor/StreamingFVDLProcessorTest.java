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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.VulnerabilityFilterer;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
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
    void testParseDoesNotResolveExternalEntity() throws Exception {
      Path secret = Files.createTempFile("fvdl-xxe-sentinel", ".txt");
      try {
        Files.writeString(secret, "PRIVATE-SENTINEL");
        createTestFpr("""
          <!DOCTYPE FVDL [<!ENTITY xxe SYSTEM "%s">]>
          <FVDL><Run><EngineName>&xxe;</EngineName></Run><Vulnerabilities/></FVDL>
          """.formatted(secret.toUri()));

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
                IOException exception = assertThrows(IOException.class, () -> processor.parse(zipFile, "audit.fvdl"));
                assertInstanceOf(XMLStreamException.class, exception.getCause());
          assertTrue(!exception.getMessage().contains("PRIVATE-SENTINEL"));
        }
      } finally {
        Files.deleteIfExists(secret);
      }
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
    void testParseBuildSourceFilesTypeMetadata() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Build>
                    <SourceFiles>
                      <File type="PLSQL"><Name>db/t1.sql</Name></File>
                      <File type="JAVA"><Name>src/Test.java</Name></File>
                      <File type="PLSQL"><Name>pkg/spec.pks</Name></File>
                      <File type="TSQL"><Name>db/other.sql</Name></File>
                    </SourceFiles>
                  </Build>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        FVDLMetadata metadata = processor.getFvdlMetadata();
        assertEquals("PLSQL", metadata.findSourceFileTypeForFileName("db/t1.sql"));
        assertEquals("PLSQL", metadata.findSourceFileTypeForFileName("t1.sql"));
        assertEquals("JAVA", metadata.findSourceFileTypeForFileName("src/Test.java"));
        assertNull(metadata.findSourceFileTypeForExtension(".sql"));
        assertEquals("PLSQL", metadata.findSourceFileTypeForExtension(".pks"));
    }

    @Test
    void testParseBuildSourceFilesKeepsExactCaseMappingsAndTreatsFoldedCaseAsAmbiguous() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Build>
                    <SourceFiles>
                      <File type="PLSQL"><Name>db/Case.sql</Name></File>
                      <File type="TSQL"><Name>db/case.sql</Name></File>
                    </SourceFiles>
                  </Build>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        FVDLMetadata metadata = processor.getFvdlMetadata();
        assertEquals("PLSQL", metadata.findSourceFileTypeForFileName("db/Case.sql"));
        assertEquals("TSQL", metadata.findSourceFileTypeForFileName("db/case.sql"));
        assertNull(metadata.findSourceFileTypeForFileName("db/CASE.sql"));
        assertNull(metadata.findSourceFileTypeForFileName("CASE.sql"));
        assertNull(metadata.findSourceFileTypeForExtension(".sql"));
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

    @Test
    void testParseSharesStackTraceElementsPerNodeAcrossVulnerabilities() throws Exception {
        String vulnerabilityTemplate = """
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <Kingdom>Dataflow</Kingdom>
                        <Type>SQL Injection</Type>
                        <AnalyzerName>Dataflow</AnalyzerName>
                        <DefaultSeverity>3.0</DefaultSeverity>
                      </ClassInfo>
                      <InstanceInfo>
                        <InstanceID>%s</InstanceID>
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
                        </Unified>
                      </AnalysisInfo>
                    </Vulnerability>
                """;
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Vulnerabilities>
                %s%s
                  </Vulnerabilities>
                  <UnifiedNodePool>
                    <Node id="0">
                      <SourceLocation path="src/Source.java" line="10" lineEnd="10" colStart="0" colEnd="0"/>
                      <Action type="Assign">input</Action>
                    </Node>
                    <Node id="1">
                      <SourceLocation path="src/Sink.java" line="20" lineEnd="20" colStart="0" colEnd="0"/>
                      <Action type="InCall">execute(0)</Action>
                      <Reason>
                        <Trace>
                          <Primary>
                            <Entry><NodeRef id="0"/></Entry>
                          </Primary>
                        </Trace>
                      </Reason>
                    </Node>
                  </UnifiedNodePool>
                </FVDL>
                """.formatted(vulnerabilityTemplate.formatted("instance-1"), vulnerabilityTemplate.formatted("instance-2"));

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        assertEquals(2, processor.getVulnerabilities().size());
        List<StackTraceElement> first = processor.getVulnerabilities().get(0).getStackTrace().get(0);
        List<StackTraceElement> second = processor.getVulnerabilities().get(1).getStackTrace().get(0);
        assertNotSame(first, second);
        assertSame(first.get(0), second.get(0));
        assertSame(first.get(1), second.get(1));

        StackTraceElement sink = first.get(1);
        assertEquals("src/Sink.java", sink.getFilename());
        assertEquals(1, sink.getInnerStackTrace().size());
        StackTraceElement reasonElement = sink.getInnerStackTrace().get(0);
        assertEquals("src/Source.java", reasonElement.getFilename());
        assertTrue(reasonElement.getInnerStackTrace().isEmpty());
    }

    @Test
    void testParseConvertsVulnerabilitiesInDocumentOrderUsingMetadataDeclaredAfterThem() throws Exception {
        String vulnerabilityTemplate = """
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <Type>SQL Injection</Type>
                        <AnalyzerName>Dataflow</AnalyzerName>
                        <DefaultSeverity>3.0</DefaultSeverity>
                      </ClassInfo>
                      <InstanceInfo>
                        <InstanceID>%s</InstanceID>
                        <InstanceSeverity>3.0</InstanceSeverity>
                        <Confidence>4.0</Confidence>
                      </InstanceInfo>
                      <AnalysisInfo>
                        <Unified>
                          <ReplacementDefinitions>
                            <Def key="Source" value="%s"/>
                          </ReplacementDefinitions>
                        </Unified>
                      </AnalysisInfo>
                    </Vulnerability>
                """;
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Vulnerabilities>
                %s%s%s
                  </Vulnerabilities>
                  <Description classID="RULE-1">
                    <Abstract>Input from &lt;Replace key="Source"/&gt; reaches a query.</Abstract>
                    <Explanation>Explanation text.</Explanation>
                  </Description>
                  <EngineData>
                    <RuleInfo>
                      <Rule id="RULE-1">
                        <MetaInfo><Group name="CWE">89</Group></MetaInfo>
                      </Rule>
                    </RuleInfo>
                  </EngineData>
                </FVDL>
                """.formatted(
                    vulnerabilityTemplate.formatted("instance-a", "getParameter"),
                    vulnerabilityTemplate.formatted("instance-b", "getHeader"),
                    vulnerabilityTemplate.formatted("instance-c", "getCookies"));

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        List<Vulnerability> vulnerabilities = processor.getVulnerabilities();
        assertEquals(List.of("instance-a", "instance-b", "instance-c"),
            vulnerabilities.stream().map(Vulnerability::getInstanceID).toList());
        assertEquals("Input from getParameter reaches a query.", vulnerabilities.get(0).getShortDescription());
        assertEquals("Input from getHeader reaches a query.", vulnerabilities.get(1).getShortDescription());
        assertEquals("Input from getCookies reaches a query.", vulnerabilities.get(2).getShortDescription());
        assertEquals("Explanation text.", vulnerabilities.get(2).getExplanation());
        vulnerabilities.forEach(v -> assertEquals("89", v.getKnowledge().get("CWE")));
        assertTrue(processor.getFvdlMetadata().getDescriptionCache().isEmpty());
        assertTrue(processor.getFvdlMetadata().getNodePool().isEmpty());
    }

    @Test
    void testParseHandlesSelfReferencingReasonAndMissingTraceRef() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Vulnerabilities>
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <Type>SQL Injection</Type>
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
                            </Primary>
                          </Trace>
                        </Unified>
                      </AnalysisInfo>
                    </Vulnerability>
                  </Vulnerabilities>
                  <UnifiedNodePool>
                    <Node id="0">
                      <SourceLocation path="src/Loop.java" line="5" lineEnd="5" colStart="0" colEnd="0"/>
                      <Action type="Call">loop(0)</Action>
                      <Reason>
                        <TraceRef id="self-trace"/>
                        <TraceRef id="missing-trace"/>
                      </Reason>
                    </Node>
                  </UnifiedNodePool>
                  <UnifiedTracePool>
                    <Trace id="self-trace">
                      <Primary>
                        <Entry><NodeRef id="0"/></Entry>
                      </Primary>
                    </Trace>
                  </UnifiedTracePool>
                </FVDL>
                """;

        createTestFpr(xml);

        StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
        try (ZipFile zipFile = new ZipFile(tempFprFile.toFile())) {
            processor.parse(zipFile, "audit.fvdl");
        }

        StackTraceElement element = processor.getVulnerabilities().get(0).getStackTrace().get(0).get(0);
        assertEquals("src/Loop.java", element.getFilename());
        assertEquals(1, element.getInnerStackTrace().size());
        StackTraceElement reasonElement = element.getInnerStackTrace().get(0);
        assertNotSame(element, reasonElement);
        assertEquals("src/Loop.java", reasonElement.getFilename());
        assertTrue(reasonElement.getInnerStackTrace().isEmpty());
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