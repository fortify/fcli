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

import static com.fortify.cli.common.fpr.processor.XmlParserUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.fpr.Vulnerability;
import com.fortify.cli.common.fpr.filter.AnalyzerType;
import com.fortify.cli.common.fpr.model.*;
import com.fortify.cli.common.fpr.model.StackTraceElement;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.util.StringUtil;
import com.fortify.cli.common.fpr.utils.FileUtils;
import com.fortify.cli.common.fpr.utils.ISourceDecoder;
import com.fortify.cli.common.fpr.utils.SourceDecoders;
import com.fortify.cli.common.fpr.utils.XmlUtils;
import com.fortify.cli.common.util.SecureXmlParserFactory;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class StreamingFVDLProcessor {
    private static final Logger logger = LoggerFactory.getLogger(StreamingFVDLProcessor.class);
    private static final int MEMORY_SAMPLE_INTERVAL = 10_000;
    private final XMLInputFactory xmlInputFactory;
    @Getter
    private final FVDLMetadata fvdlMetadata;
    private final VulnFinalizer vulnFinalizer;
    private final FileUtils fileUtils;
    private final DescriptionProcessor descriptionProcessor;
    @Getter
    private final Map<String, String> sourceFileMap;
    @Getter
    private final List<Vulnerability> vulnerabilities;
    private final FprHandle fprHandle;
    private final StackTraceConverter stackTraceConverter;
    // Many vulnerabilities render the same description text; keep one copy of each.
    private final Map<String, String> renderedDescriptions = new HashMap<>();
    private final DescriptionCleanupCache descriptionCleanupCache = new DescriptionCleanupCache();
    // Vulnerabilities of the same rule usually have equal knowledge; share one read-only map.
    private final Map<Map<String, String>, Map<String, String>> sharedKnowledgeMaps = new HashMap<>();
    /*private final Path extractedPath;
    private final IndexXMLProcessor indexXMLProcessor;*/

    // Specialized parsers
    private final MemoryTracker memoryTracker;
    private final NodeParser nodeParser;
    private final TraceParser traceParser;
    private final DescriptionParser descriptionParser;
    private final MetadataParser metadataParser;

    public StreamingFVDLProcessor(FprHandle fprHandle) {
        this(fprHandle, SourceDecoders.defaults());
    }

    /**
     * @param sourceDecoder used for stack-trace line/fragment source reads; shares {@link #fvdlMetadata}
     *                      so FPR encoding candidates resolve after build metadata is parsed.
     */
    public StreamingFVDLProcessor(FprHandle fprHandle, ISourceDecoder sourceDecoder) {
        this.vulnFinalizer = new VulnFinalizer();
        this.fprHandle = fprHandle;
        this.sourceFileMap = fprHandle.getSourceFileMap();
        this.xmlInputFactory = SecureXmlParserFactory.newXmlInputFactory();
        this.fvdlMetadata = new FVDLMetadata();
        // Same metadata instance FileUtils will see once encodings are registered during parse.
        this.fileUtils = new FileUtils(
                Objects.requireNonNull(sourceDecoder, "sourceDecoder"), this.fvdlMetadata);
        this.stackTraceConverter = new StackTraceConverter(fileUtils, fprHandle, fvdlMetadata);
        this.vulnerabilities = new ArrayList<>();
        this.descriptionProcessor = new DescriptionProcessor();

        // Initialize specialized parsers
        this.memoryTracker = new MemoryTracker();
        this.traceParser = new TraceParser(fvdlMetadata.getNodePool());
        this.nodeParser = new NodeParser(traceParser);
        this.traceParser.setNodeParser(nodeParser); // Circular dependency for Reason parsing
        this.descriptionParser = new DescriptionParser();
        this.metadataParser = new MetadataParser();
    }


    /**
     * Parse an FPR file using two-pass parsing strategy.
     *
     * TWO-PASS PARSING STRATEGY:
    * Pass 1: Parse metadata and pools (NodePool, TracePool, ContextPool, Descriptions, EngineData)
     *         This populates the reference pools needed for vulnerability parsing.
     * Pass 2: Parse Vulnerabilities with fully populated pools and convert each one as soon as it is parsed,
     *         so raw parse results never accumulate in memory.
     *         NodeRef lookups now succeed because NodePool is populated.
     *
     * @param zipFile zipFile for FPR file
     * @param entryName entry name for FVDL file
     * @throws Exception If file access or parsing fails
     */
    public void parse(ZipFile zipFile, String entryName) throws Exception {
        logger.info("=== Two-Pass Streaming Parsing Started for FVDL file ===");
        logMemory("Before Parsing");

        ZipEntry fvdlEntry = zipFile.getEntry(entryName);
        if (fvdlEntry == null) {
            throw new IOException("audit.fvdl not found in FPR file");
        }

        try {
            // ========================================
            // PASS 1: Parse Metadata and Pools
            // ========================================
            logger.info(">>> PASS 1: Parsing metadata and pools (NodePool, TracePool, ContextPool, Descriptions, EngineData)");
            long pass1Start = System.currentTimeMillis();

            try (InputStream is1 = zipFile.getInputStream(fvdlEntry)) {
                parseMetadataAndPools(is1);
            }

            long pass1Time = System.currentTimeMillis() - pass1Start;
            logger.info("<<< PASS 1 Complete - Time: {}ms", pass1Time);
            logMemory("PASS 1");

            // ========================================
            // PASS 2: Parse and Convert Vulnerabilities
            // ========================================
            logger.info(">>> PASS 2: Parsing and converting vulnerabilities (with populated pools)");
            long pass2Start = System.currentTimeMillis();

            try (InputStream is2 = zipFile.getInputStream(fvdlEntry)) {
                parseVulnerabilitiesOnly(is2);
            }

            long pass2Time = System.currentTimeMillis() - pass2Start;
            logger.info("<<< PASS 2 Complete - Time: {}ms", pass2Time);
            logger.info("    - Vulnerabilities processed: {}", vulnerabilities.size());
            logMemory("PASS 2");

            releaseParsingState();
            logMemory("Releasing Parsing State");

            logger.info("=== Two-Pass Parsing Complete - Total Time: {}ms (Pass1: {}ms, Pass2: {}ms) ===",
                pass1Time + pass2Time, pass1Time, pass2Time);

        } catch (XMLStreamException e) {
            throw new IOException("Failed to parse FVDL: " + e.getMessage(), e);
        }

    }

    private void logMemory(String phase) {
        memoryTracker.logPhase(phase, fvdlMetadata, vulnerabilities.size());
    }

    /** Releases pools and caches that are only needed while converting vulnerabilities. */
    private void releaseParsingState() {
        logger.debug("Releasing parsing state: descriptions={}, rules={}, nodes={}, traces={}",
            fvdlMetadata.getDescriptionCache().size(), fvdlMetadata.getRuleMetadata().size(),
            fvdlMetadata.getNodePool().size(), fvdlMetadata.getTracePool().size());
        fvdlMetadata.getDescriptionCache().clear();
        descriptionProcessor.clearCompiledTemplates();
        fvdlMetadata.getRuleMetadata().clear();
        fvdlMetadata.getNodePool().clear();
        fvdlMetadata.getTracePool().clear();
        stackTraceConverter.clear();
        renderedDescriptions.clear();
        descriptionCleanupCache.clear();
        sharedKnowledgeMaps.clear();
    }


    /**
     * PASS 1: Parse metadata and pools only.
     * Skips Vulnerabilities section for later processing in Pass 2.
     *
     * Parsed sections:
     * - EngineData (rule metadata)
     * - UnifiedNodePool (node definitions)
     * - UnifiedTracePool (trace definitions)
    * - ContextPool (context definitions)
    * - Description (vulnerability descriptions)
    * - Build (source file typing and basic build metadata)
     *
     * Skipped sections:
     * - Vulnerabilities (will be parsed in Pass 2)
     */
    private void parseMetadataAndPools(InputStream is) throws XMLStreamException {
        logger.debug("Pass 1: Starting metadata and pools parsing");
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);

        try {
            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();

                    switch (localName) {
                        case "EngineData":
                            logger.debug("Pass 1: Parsing EngineData");
                            parseEngineData(reader);
                            break;
                        case "Run":
                            logger.debug("Pass 1: Parsing Run for analysis type");
                            parseRun(reader);
                            break;
                        case "RuntimeConfiguration":
                            logger.debug("Pass 1: Parsing RuntimeConfiguration for analysis type");
                            parseRuntimeConfiguration(reader);
                            break;
                        case "Build":
                            logger.debug("Pass 1: Parsing Build");
                            parseBuild(reader);
                            break;
                        case "UnifiedNodePool":
                            logger.debug("Pass 1: Parsing UnifiedNodePool");
                            parseNodePool(reader);
                            break;
                        case "UnifiedTracePool":
                            logger.debug("Pass 1: Parsing UnifiedTracePool");
                            parseTracePool(reader);
                            break;
                        case "ContextPool":
                            logger.debug("Pass 1: Parsing ContextPool");
                            parseContextPool(reader);
                            break;
                        case "Description":
                            logger.debug("Pass 1: Parsing Description");
                            parseDescriptions(reader);
                            break;
                        case "Vulnerabilities":
                            logger.debug("Pass 1: Skipping Vulnerabilities (will parse in Pass 2)");
                            skipSection(reader, "Vulnerabilities");
                            break;
                    }
                }
            }
        } finally {
            reader.close();
        }
        logger.debug("Pass 1: Metadata and pools parsing complete");
    }

    /**
     * PASS 2: Parse vulnerabilities only.
     * Skips all other sections that were already parsed in Pass 1.
     *
     * At this point, NodePool and TracePool are fully populated,
     * so all NodeRef lookups will succeed.
     *
     * Parsed sections:
     * - Vulnerabilities (with NodeRef resolution)
     */
    private void parseVulnerabilitiesOnly(InputStream is) throws XMLStreamException {
        logger.debug("Pass 2: Starting vulnerabilities parsing");
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);

        try {
            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();

                    switch (localName) {
                        case "Vulnerabilities":
                            logger.debug("Pass 2: Parsing Vulnerabilities");
                            parseVulnerabilities(reader);
                            break;
                        case "EngineData":
                        case "Build":
                        case "UnifiedNodePool":
                        case "UnifiedTracePool":
                        case "ContextPool":
                        case "Description":
                            logger.debug("Pass 2: Skipping {} (already parsed in Pass 1)", localName);
                            skipSection(reader, localName);
                            break;
                    }
                }
            }
        } finally {
            reader.close();
        }
        logger.debug("Pass 2: Vulnerabilities parsing complete");
    }

    /**
     * Parse EngineData section for rule metadata.
     * Delegates to MetadataParser.
     */
    private void parseEngineData(XMLStreamReader reader) throws XMLStreamException {
        metadataParser.parseEngineData(reader, fvdlMetadata);
    }

    private void parseRun(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT && "EngineName".equals(reader.getLocalName())) {
                updateAnalysisType(readElementText(reader));
            } else if (event == XMLStreamConstants.END_ELEMENT && "Run".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    public void parseBuildMetadata(ZipFile zipFile, String entryName) throws Exception {
        logger.debug("Parsing FVDL build metadata entry '{}'", entryName);
        ZipEntry fvdlEntry = zipFile.getEntry(entryName);
        if (fvdlEntry == null) {
            throw new IOException("audit.fvdl not found in FPR file");
        }

        try (InputStream inputStream = zipFile.getInputStream(fvdlEntry)) {
            parseBuildMetadata(inputStream);
        }
        logger.debug("Parsed FVDL build metadata entry '{}'", entryName);
    }

    public void parseBuildMetadata(InputStream inputStream) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);

        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT && "Build".equals(reader.getLocalName())) {
                    parseBuild(reader);
                    return;
                }
            }
            logger.debug("No Build section found while parsing FVDL build metadata");
        } finally {
            reader.close();
        }
    }

    private void parseRuntimeConfiguration(XMLStreamReader reader) throws XMLStreamException {
        updateAnalysisType("SECURITYSCOPE");
        skipSection(reader, "RuntimeConfiguration");
    }

    private void updateAnalysisType(String analysisType) {
        if (analysisType != null && !analysisType.isBlank()) {
            fvdlMetadata.setAnalysisType(analysisType.trim());
        }
    }

    private void parseBuild(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                switch (localName) {
                    case "BuildID":
                        fvdlMetadata.setBuildId(readElementText(reader));
                        continue;
                    case "Project":
                        fvdlMetadata.setProjectName(readElementText(reader));
                        continue;
                    case "Version":
                        fvdlMetadata.setProjectVersion(readElementText(reader));
                        continue;
                    case "SourceFiles":
                        parseSourceFiles(reader);
                        continue;
                    default:
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Build".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    private void parseSourceFiles(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT && "File".equals(reader.getLocalName())) {
                parseSourceFile(reader);
            } else if (event == XMLStreamConstants.END_ELEMENT && "SourceFiles".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    private void parseSourceFile(XMLStreamReader reader) throws XMLStreamException {
        String fileType = reader.getAttributeValue(null, "type");
        String fileEncoding = reader.getAttributeValue(null, "encoding");
        String fileName = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT && "Name".equals(reader.getLocalName())) {
                fileName = readElementText(reader);
            } else if (event == XMLStreamConstants.END_ELEMENT && "File".equals(reader.getLocalName())) {
                break;
            }
        }

        fvdlMetadata.registerSourceFileType(fileName, fileType);
        fvdlMetadata.registerSourceFileEncoding(fileName, fileEncoding);
    }

    /**
     * Parse UnifiedNodePool section.
     * Delegates node parsing to NodeParser.
     */
    private void parseNodePool(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Node".equals(reader.getLocalName())) {
                    Node node = nodeParser.parseNode(reader, null);

                    if (node != null) {
                        fvdlMetadata.getNodePool().put(node.getId(), node);
                        fvdlMetadata.setTotalNodes(fvdlMetadata.getTotalNodes() + 1);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("UnifiedNodePool".equals(reader.getLocalName())) {
                    break;
                }
            }
        }
        logger.info("Nodes processed: {} ", fvdlMetadata.getNodePool().size());
    }

    /**
     * Parse UnifiedTracePool section.
     * Delegates to TraceParser.
     */
    private void parseTracePool(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Trace".equals(reader.getLocalName())) {
                    String traceId = reader.getAttributeValue(null, "id");
                    if (traceId != null && !traceId.isEmpty()) {
                        StreamedTrace trace =
                            traceParser.parseStreamedTrace(reader, traceId);
                        if (trace != null) {
                            fvdlMetadata.getTracePool().put(traceId, trace);
                            fvdlMetadata.setTotalTraces(fvdlMetadata.getTotalTraces() + 1);
                        }
                    } else {
                        logger.warn("Trace missing or invalid ID, skipping");
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("UnifiedTracePool".equals(reader.getLocalName())) {
                    break;
                }
            }
        }
        logger.debug("Traces processed: {}", fvdlMetadata.getTracePool().size());
    }

    /**
     * Parse ContextPool section.
     */
    private void parseContextPool(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT && "Context".equals(reader.getLocalName())) {
                FVDLMetadata.ContextInfo contextInfo = parseContext(reader);
                if (contextInfo != null && contextInfo.getId() != null && !contextInfo.getId().isBlank()) {
                    fvdlMetadata.getContextPool().put(contextInfo.getId(), contextInfo);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "ContextPool".equals(reader.getLocalName())) {
                break;
            }
        }

        logger.info("Contexts processed: {} ", fvdlMetadata.getContextPool().size());
    }

    private FVDLMetadata.ContextInfo parseContext(XMLStreamReader reader) throws XMLStreamException {
        FVDLMetadata.ContextInfo contextInfo = new FVDLMetadata.ContextInfo();
        contextInfo.setId(reader.getAttributeValue(null, "id"));

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "NamespaceIdent":
                        contextInfo.setNamespace(reader.getAttributeValue(null, "name"));
                        break;
                    case "ClassIdent":
                        contextInfo.setNamespace(reader.getAttributeValue(null, "namespace"));
                        contextInfo.setClassName(reader.getAttributeValue(null, "name"));
                        break;
                    case "Function":
                        contextInfo.setNamespace(reader.getAttributeValue(null, "namespace"));
                        contextInfo.setClassName(reader.getAttributeValue(null, "enclosingClass"));
                        contextInfo.setFunctionName(reader.getAttributeValue(null, "name"));
                        break;
                    case "FunctionDeclarationSourceLocation":
                        contextInfo.setFilename(reader.getAttributeValue(null, "path"));
                        contextInfo.setStartLine(parseIntSafe(reader.getAttributeValue(null, "line")));
                        break;
                    default:
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Context".equals(reader.getLocalName())) {
                return contextInfo;
            }
        }

        return contextInfo;
    }

    /**
     * Parse Descriptions section for vulnerability descriptions.
     * Delegates to DescriptionParser.
     */
    private void parseDescriptions(XMLStreamReader reader) throws XMLStreamException {
        descriptionParser.parseDescriptions(reader, fvdlMetadata.getDescriptionCache());
    }

    private void parseVulnerabilities(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Vulnerability".equals(reader.getLocalName())) {
                    vulnerabilities.add(processVulnerability(parseVulnerability(reader), vulnFinalizer));
                    if (vulnerabilities.size() % MEMORY_SAMPLE_INTERVAL == 0) {
                        memoryTracker.sample();
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("Vulnerabilities".equals(reader.getLocalName())) {
                    return;
                }
            }
        }
    }

    private StreamedVulnerability parseVulnerability(XMLStreamReader reader) throws XMLStreamException {
        StreamedVulnerability.StreamedVulnerabilityBuilder builder = StreamedVulnerability.builder();
        List<StreamedVulnerability.Trace> traces = new ArrayList<>();
        List<Map<String, String>> auxiliaryDataList = new ArrayList<>();
        List<StreamedVulnerability.ExternalEntry> externalEntriesList = new ArrayList<>();
        Map<String, String> externalIdsMap = new HashMap<>();

        String currentSection = null;

        // Process until we hit the CLOSING </Vulnerability> tag
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "ClassInfo":
                        currentSection = "ClassInfo";
                        break;
                    case "InstanceInfo":
                        currentSection = "InstanceInfo";
                        builder.minVirtualCallConfidence(parseDoubleSafe(reader.getAttributeValue(null, "MinVirtualCallConfidence")));
                        break;
                    case "AnalysisInfo":
                        currentSection = "AnalysisInfo";
                        break;
                    case "ClassID":
                        if ("ClassInfo".equals(currentSection)) {
                            builder.classId(readElementText(reader));
                        }
                        break;
                    case "Kingdom":
                        builder.kingdom(readElementText(reader));
                        break;
                    case "Type":
                        builder.type(readElementText(reader));
                        break;
                    case "Subtype":
                        builder.subType(readElementText(reader));
                        break;
                    case "AnalyzerName":
                        builder.analyzerName(readElementText(reader));
                        break;
                    case "DefaultSeverity":
                        builder.defaultSeverity(parseDoubleSafe(readElementText(reader)));
                        break;
                    case "InstanceID":
                        builder.instanceId(readElementText(reader));
                        break;
                    case "InstanceSeverity":
                        builder.instanceSeverity(parseDoubleSafe(readElementText(reader)));
                        break;
                    case "MetaInfo":
                        // Parse MetaInfo element - contains instance-specific metadata overrides
                        parseMetaInfo(reader, builder);
                        break;
                    case "Confidence":
                        builder.confidence(parseDoubleSafe(readElementText(reader)));
                        break;
                    case "Trace":
                        StreamedVulnerability.Trace trace = parseTrace(reader);
                        if (trace != null) {
                            traces.add(trace);
                        }
                        break;
                    case "ReplacementDefinitions":
                        // Parse ReplacementDefinitions from AnalysisInfo->Unified->ReplacementDefinitions
                        builder.replacementData(parseReplacementDefinitions(reader));
                        break;
                    case "Context":
                        if ("AnalysisInfo".equals(currentSection)) {
                            FVDLMetadata.ContextInfo contextInfo = parseContext(reader);
                            builder.contextNamespace(contextInfo.getNamespace());
                            builder.contextClassName(contextInfo.getClassName());
                            builder.contextFunctionName(contextInfo.getFunctionName());
                        }
                        break;
                    case "AuxiliaryData":
                        parseAuxiliaryData(reader, auxiliaryDataList);
                        break;
                    case "ExternalEntries":
                        parseExternalEntries(reader, externalEntriesList);
                        break;
                    case "ExternalID":
                        parseExternalID(reader, externalIdsMap);
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String localName = reader.getLocalName();

                // Exit when we close the Vulnerability element
                if ("Vulnerability".equals(localName)) {
                    break;
                }

                // Reset section tracking
                if ("ClassInfo".equals(localName) ||
                    "InstanceInfo".equals(localName) ||
                    "AnalysisInfo".equals(localName)) {
                    currentSection = null;
                }
            }
        }

        builder.traces(traces);
        builder.auxiliaryData(auxiliaryDataList);
        builder.externalEntries(externalEntriesList);
        builder.externalIds(externalIdsMap);
        return builder.build();
    }


    /**
     * Parse MetaInfo element and extract metadata groups.
     * MetaInfo structure: <MetaInfo><Group name="key">value</Group>...</MetaInfo>
     * This is used for instance-specific metadata overrides in InstanceInfo section.
     *
     * @param reader XMLStreamReader positioned at MetaInfo start element
     * @param builder StreamedVulnerability builder to populate metadata
     * @throws XMLStreamException If XML parsing fails
     */
    private void parseMetaInfo(XMLStreamReader reader, StreamedVulnerability.StreamedVulnerabilityBuilder builder)
        throws XMLStreamException {

        Map<String, String> instanceMetadata = new HashMap<>();
        int depth = 1;

        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("Group".equals(localName)) {
                    // Get the "name" attribute
                    String groupName = reader.getAttributeValue(null, "name");

                    // Get the text content (value)
                    String groupValue = readElementText(reader);

                    if (groupName != null && groupValue != null) {
                        instanceMetadata.put(groupName, groupValue.trim());
                        logger.trace("Parsed MetaInfo Group: '{}' = '{}'", groupName, groupValue);
                    }
                    continue; // Skip depth++ since we consumed the element text
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if ("MetaInfo".equals(reader.getLocalName()) && depth == 0) {
                    break; // Exit when we close the MetaInfo element
                }
            }
        }

        // Store the parsed metadata in the builder
        if (!instanceMetadata.isEmpty()) {
            builder.metadata(instanceMetadata);
            logger.trace("Parsed {} MetaInfo groups", instanceMetadata.size());
        }
    }

    /**
     * Parse inline Trace from Vulnerability.
     * Delegates to TraceParser.
     */
    private StreamedVulnerability.Trace parseTrace(XMLStreamReader reader) throws XMLStreamException {
        return traceParser.parseTrace(reader);
    }

    /**
     * Parse AuxiliaryData element.
     * Structure: <AuxiliaryData contentType="..."><AuxField name="..." value="..."><SourceLocation/>     </AuxField></AuxiliaryData>
          *
          * @param reader XMLStreamReader positioned at AuxiliaryData start element
     * @param auxiliaryDataList List to add parsed auxiliary data map to
     */
    private void parseAuxiliaryData(XMLStreamReader reader, List<Map<String, String>> auxiliaryDataList) throws XMLStreamException {
        Map<String, String> auxMap = new HashMap<>();

        // Get contentType attribute
        String contentType = reader.getAttributeValue(null, "contentType");
        auxMap.put("contentType", contentType != null ? contentType : "");

        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("AuxField".equals(localName)) {
                    // Get AuxField attributes
                    String fieldName = reader.getAttributeValue(null, "name");
                    String fieldValue = reader.getAttributeValue(null, "value");

                    if (fieldName != null) {
                        auxMap.put(fieldName, fieldValue != null ? fieldValue : "");
                    }

                    // Check for nested SourceLocation
                    parseNestedSourceLocation(reader, auxMap);
                    continue; // Skip depth++ since we handled the element
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }

        auxiliaryDataList.add(auxMap);
    }

    /**
     * Parse nested SourceLocation within AuxField.
     * Adds location attributes to the map with "loc" prefix.
     *
     * @param reader XMLStreamReader positioned after AuxField start
     * @param auxMap Map to add location data to
     */
    private void parseNestedSourceLocation(XMLStreamReader reader, Map<String, String> auxMap) throws XMLStreamException {
        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("SourceLocation".equals(reader.getLocalName())) {
                    String path = reader.getAttributeValue(null, "path");
                    String line = reader.getAttributeValue(null, "line");
                    String colStart = reader.getAttributeValue(null, "colStart");
                    String colEnd = reader.getAttributeValue(null, "colEnd");

                    auxMap.put("locPath", path != null ? path : "");
                    auxMap.put("locLine", line != null ? line : "0");
                    auxMap.put("locColStart", colStart != null ? colStart : "0");
                    auxMap.put("locColEnd", colEnd != null ? colEnd : "0");
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if ("AuxField".equals(reader.getLocalName())) {
                    return; // Exit when we close AuxField
                }
            }
        }
    }

    /**
     * Parse ExternalEntries element.
     * Structure: <ExternalEntries><Entry><URL>...</URL><Function/><SourceLocation/><Fields><Field/>     </Fields></Entry></ExternalEntries>
          *
          * @param reader XMLStreamReader positioned at ExternalEntries start element
     * @param externalEntriesList List to add parsed entries to
     */
    private void parseExternalEntries(XMLStreamReader reader, List<StreamedVulnerability.ExternalEntry> externalEntriesList)
        throws XMLStreamException {

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT && "Entry".equals(reader.getLocalName())) {
                externalEntriesList.add(parseExternalEntry(reader));
            } else if (event == XMLStreamConstants.END_ELEMENT && "ExternalEntries".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    /**
     * Parse a single Entry element within ExternalEntries.
     *
     * @param reader XMLStreamReader positioned at Entry start element
     * @return Parsed ExternalEntry
     */
    private StreamedVulnerability.ExternalEntry parseExternalEntry(XMLStreamReader reader) throws XMLStreamException {
        StreamedVulnerability.ExternalEntry.ExternalEntryBuilder builder =
            StreamedVulnerability.ExternalEntry.builder();

        List<StreamedVulnerability.EntryField> fields = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "URL":
                        builder.url(reader.getElementText());
                        break;
                    case "Function":
                        // Parse Function element attributes
                        String funcName = reader.getAttributeValue(null, "name");
                        String funcNamespace = reader.getAttributeValue(null, "namespace");
                        builder.functionName(funcName);
                        builder.functionNamespace(funcNamespace);
                        break;
                    case "SourceLocation":
                        // Parse SourceLocation attributes
                        String path = reader.getAttributeValue(null, "path");
                        String line = reader.getAttributeValue(null, "line");
                        String colStart = reader.getAttributeValue(null, "colStart");
                        String colEnd = reader.getAttributeValue(null, "colEnd");
                        builder.locationPath(path);
                        builder.locationLine(parseIntSafe(line));
                        builder.locationColStart(parseIntSafe(colStart));
                        builder.locationColEnd(parseIntSafe(colEnd));
                        break;
                    case "Fields":
                        parseEntryFields(reader, fields);
                        break;
                    default:
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Entry".equals(reader.getLocalName())) {
                break;
            }
        }

        builder.fields(fields);
        return builder.build();
    }

    /**
     * Parse Fields element containing multiple Field elements.
     *
     * @param reader XMLStreamReader positioned at Fields start element
     * @param fields List to add parsed fields to
     */
    private void parseEntryFields(XMLStreamReader reader, List<StreamedVulnerability.EntryField> fields)
        throws XMLStreamException {

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT && "Field".equals(reader.getLocalName())) {
                fields.add(parseEntryField(reader));
            } else if (event == XMLStreamConstants.END_ELEMENT && "Fields".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    /**
     * Parse a single Field element. Per the FVDL schema all values are attributes;
     * missing values default to "" to match AuxiliaryProcessor.
     *
     * @param reader XMLStreamReader positioned at Field start element
     * @return Parsed EntryField
     */
    private StreamedVulnerability.EntryField parseEntryField(XMLStreamReader reader) {
        return StreamedVulnerability.EntryField.builder()
            .name(attributeOrEmpty(reader, "name"))
            .value(attributeOrEmpty(reader, "value"))
            .type(attributeOrEmpty(reader, "type"))
            .vulnTag(attributeOrEmpty(reader, "vulnTag"))
            .build();
    }

    private static String attributeOrEmpty(XMLStreamReader reader, String name) {
        String value = reader.getAttributeValue(null, name);
        return value != null ? value : "";
    }

    /**
     * Parse ExternalID element.
     * Structure: <ExternalID name="...">value</ExternalID>
     *
     * @param reader XMLStreamReader positioned at ExternalID start element
     * @param externalIdsMap Map to add parsed external ID to
     */
    private void parseExternalID(XMLStreamReader reader, Map<String, String> externalIdsMap) throws XMLStreamException {
        String name = reader.getAttributeValue(null, "name");
        String value = readElementText(reader);

        if (name != null && value != null) {
            externalIdsMap.put(name, value);
        }
    }

    /**
     * Parse ReplacementDefinitions element.
     * Structure:
     * <ReplacementDefinitions>
     *   <Def key="..." value="..."><SourceLocation path="..." line="..." colStart="..." colEnd="..."/></Def>
     *   <LocationDef key="..." path="..." line="..." colStart="..." colEnd="..."/>
     * </ReplacementDefinitions>
     *
     * @param reader XMLStreamReader positioned at ReplacementDefinitions start element
     * @return Populated ReplacementData object or null if no definitions found
     */
    private ReplacementData parseReplacementDefinitions(XMLStreamReader reader)
        throws XMLStreamException {

        ReplacementData replacementData =
            new ReplacementData();

        int depth = 1;

        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("Def".equals(localName)) {
                    // Parse <Def> element: has key and value attributes, may have nested SourceLocation
                    String key = reader.getAttributeValue(null, "key");
                    String value = reader.getAttributeValue(null, "value");

                    // Initialize location fields
                    String path = null;
                    String line = null;
                    String colStart = null;
                    String colEnd = null;

                    // Check for nested SourceLocation element
                    int defDepth = 1;
                    while (reader.hasNext() && defDepth > 0) {
                        int defEvent = reader.next();

                        if (defEvent == XMLStreamConstants.START_ELEMENT) {
                            if ("SourceLocation".equals(reader.getLocalName())) {
                                // Extract SourceLocation attributes
                                path = reader.getAttributeValue(null, "path");
                                line = reader.getAttributeValue(null, "line");
                                colStart = reader.getAttributeValue(null, "colStart");
                                colEnd = reader.getAttributeValue(null, "colEnd");
                            }
                            defDepth++;
                        } else if (defEvent == XMLStreamConstants.END_ELEMENT) {
                            defDepth--;
                        }
                    }

                    // Add replacement to data object
                    if (key != null) {
                        replacementData.addReplacement(key, value, path, line, colStart, colEnd);
                        logger.trace("Parsed Def: key={}, value={}, path={}", key, value, path);
                    }
                    continue; // Skip depth increment since we consumed the element

                } else if ("LocationDef".equals(localName)) {
                    // Parse <LocationDef> element: has key and location attributes directly
                    String key = reader.getAttributeValue(null, "key");
                    String path = reader.getAttributeValue(null, "path");
                    String line = reader.getAttributeValue(null, "line");
                    String colStart = reader.getAttributeValue(null, "colStart");
                    String colEnd = reader.getAttributeValue(null, "colEnd");

                    if (key != null) {
                        Map<String, String> attrs = new HashMap<>();
                        attrs.put("path", path != null ? path : "");
                        attrs.put("line", line != null ? line : "0");
                        attrs.put("colStart", colStart != null ? colStart : "0");
                        attrs.put("colEnd", colEnd != null ? colEnd : "0");

                        replacementData.addLocationReplacement(key, attrs);
                        logger.trace("Parsed LocationDef: key={}, path={}", key, path);
                    }
                }
                depth++;

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }

        logger.trace("Parsed ReplacementDefinitions: {} replacements, {} location replacements",
            replacementData.getReplacements().size(),
            replacementData.getLocationReplacements().size());

        return replacementData;
    }

    /**
     * Helper to read element text safely.
     */
    private String readElementText(XMLStreamReader reader) throws XMLStreamException {
        if (reader.hasNext()) {
            reader.next();
            if (reader.isCharacters()) {
                return reader.getText();
            }
        }
        return null;
    }

    /**
     * Helper to parse integer safely.
     */
    private Integer parseIntSafe(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Helper to parse double safely.
     */
    private Double parseDoubleSafe(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    /**
     * Process a StreamedVulnerability into a fully populated internal Vulnerability object.
     * This method uses streaming-parsed metadata from FVDLMetadata instead of DOM-based processors.
     *
     * @param streamedVuln StreamedVulnerability object from streaming parser
     * @param vulnFinalizer VulnFinalizer for calculating derived fields
     * @return Fully processed Vulnerability object or null if creation fails
     */
    public Vulnerability processVulnerability(
        StreamedVulnerability streamedVuln,
        VulnFinalizer vulnFinalizer) {

        if (streamedVuln == null || streamedVuln.getInstanceId() == null || streamedVuln.getClassId() == null) {
            String instanceId = (streamedVuln != null) ? streamedVuln.getInstanceId() : "UNKNOWN";
            logger.warn("Skipping streamed vulnerability with instance ID [{}] due to missing critical data.", instanceId);
            return null;
        }

        // Create internal Vulnerability from StreamedVulnerability
        Vulnerability vulnCustom = Vulnerability.builder()
            .classID(streamedVuln.getClassId())
            .instanceID(streamedVuln.getInstanceId())
            .analyzerName(AnalyzerType.canonicalizeAnalyzerName(streamedVuln.getAnalyzerName()))
            .type(streamedVuln.getType())
            .subType(streamedVuln.getSubType())
            .kingdom(streamedVuln.getKingdom())
            .defaultSeverity(streamedVuln.getDefaultSeverity())
            .instanceSeverity(streamedVuln.getInstanceSeverity())
            .confidence(streamedVuln.getConfidence())
            .minVirtualCallConfidence(streamedVuln.getMinVirtualCallConfidence())
            .analysis(streamedVuln.getShortDescription())
            .analysisType(fvdlMetadata.getAnalysisType())
            .packageName(streamedVuln.getContextNamespace())
            .className(streamedVuln.getContextClassName())
            .functionName(streamedVuln.getContextFunctionName())
            .build();

        // 1. Get the base metadata from streaming-parsed ruleMetadata in FVDLMetadata
        Map<String, String> finalMetadata = new HashMap<>();
        Map<String, String> ruleMetadata = fvdlMetadata.getRuleMetadata().get(vulnCustom.getClassID());

        if (ruleMetadata != null) {
            finalMetadata.putAll(ruleMetadata);
        }

        // 2. Apply any instance-specific metadata overrides from streaming parse
        if (streamedVuln.getMetadata() != null) {
            finalMetadata.putAll(streamedVuln.getMetadata());
        }

        // 3. Merge the final metadata into the vulnerability's knowledge map
        vulnCustom.getKnowledge().putAll(finalMetadata);

        // 4. Populate high-level fields using merged data
        vulnCustom.setAccuracy(XmlUtils.safeParseDouble(finalMetadata.get("Accuracy"), 0.0));
        vulnCustom.setImpact(XmlUtils.safeParseDouble(finalMetadata.get("Impact"), 0.0));
        vulnCustom.setProbability(XmlUtils.safeParseDouble(finalMetadata.get("Probability"), 0.0));

        String audience = finalMetadata.getOrDefault("audience", "");
        vulnCustom.setAudience(audience);
        vulnCustom.setFiletype(finalMetadata.getOrDefault("DefaultFile", ""));

        List<List<StackTraceElement>> stackTraces =
            stackTraceConverter.toStackTraces(streamedVuln.getTraces());
        vulnCustom.setStackTrace(stackTraces);

        if (!stackTraces.isEmpty()) {
            List<StackTraceElement> firstStackTrace = stackTraces.get(0);
            List<StackTraceElement> lastStackTrace = stackTraces.get(stackTraces.size() - 1);
            vulnCustom.setFirstStackTrace(firstStackTrace);
            vulnCustom.setSource(lastStackTrace.isEmpty() ? null : lastStackTrace.get(0));
            vulnCustom.setSink(lastStackTrace.isEmpty() ? null : lastStackTrace.get(lastStackTrace.size() - 1));
            vulnCustom.setLastStackTraceElement(lastStackTrace.isEmpty() ? null : lastStackTrace.get(lastStackTrace.size() - 1));
            vulnCustom.setLongestStackTrace(findLongestList(stackTraces));
        }

        applyContextAttributes(vulnCustom, streamedVuln);

        aggregateFromTraces(vulnCustom);

        // Process DAST / Auxiliary data - replicate auxiliaryProcessor.process() and processRequestRelated()
        processAuxiliaryAndRequestData(streamedVuln, vulnCustom);

        // Process descriptions from streaming-parsed descriptionCache in FVDLMetadata
        ReplacementData replacementData = streamedVuln.getReplacementData();
        String[] descs = descriptionProcessor.processForVuln(vulnCustom, vulnCustom.getClassID(), replacementData, fvdlMetadata);
        vulnCustom.setShortDescription(sharedDescription(descriptionCleanupCache.clean(descs[0])));
        vulnCustom.setExplanation(sharedDescription(descriptionCleanupCache.clean(descs[1])));

        // Set package name from the first file path when the FVDL doesn't provide namespace data.
        if ((vulnCustom.getPackageName() == null || vulnCustom.getPackageName().isEmpty())
            && !stackTraces.isEmpty() && !stackTraces.get(0).isEmpty()) {
            StackTraceElement firstElement = stackTraces.get(0).get(0);
            if (firstElement != null && firstElement.getFilename() != null) {
                String firstFilePath = firstElement.getFilename();
                vulnCustom.setPackageName(createPackageName(firstFilePath));
            }
        }

        // Finalize to calculate derived fields
        vulnFinalizer.finalize(vulnCustom);
        vulnCustom.setKnowledge(sharedKnowledgeMaps.computeIfAbsent(vulnCustom.getKnowledge(), Collections::unmodifiableMap));

        return vulnCustom;
    }

    private String sharedDescription(String description) {
        return renderedDescriptions.computeIfAbsent(description, d -> d);
    }

    private void applyContextAttributes(Vulnerability vulnerability, StreamedVulnerability streamedVulnerability) {
        FVDLMetadata.ContextInfo sourceContextInfo = resolveContextInfo(getSourceNode(streamedVulnerability));
        FVDLMetadata.ContextInfo sinkContextInfo = resolveContextInfo(getSinkNode(streamedVulnerability));

        if (sourceContextInfo != null) {
            vulnerability.setSourceContext(sourceContextInfo.getContextString());
            vulnerability.setSourceFunction(sourceContextInfo.getQualifiedFunctionName());
            if (StringUtil.isEmpty(vulnerability.getPackageName())) {
                vulnerability.setPackageName(sourceContextInfo.getNamespace());
            }
            if (StringUtil.isEmpty(vulnerability.getClassName())) {
                vulnerability.setClassName(sourceContextInfo.getClassName());
            }
            if (StringUtil.isEmpty(vulnerability.getFunctionName())) {
                vulnerability.setFunctionName(sourceContextInfo.getFunctionName());
            }
        }

        if (sinkContextInfo != null) {
            vulnerability.setSinkContext(sinkContextInfo.getContextString());
            vulnerability.setSinkFunction(sinkContextInfo.getQualifiedFunctionName());
            if (StringUtil.isEmpty(vulnerability.getPackageName())) {
                vulnerability.setPackageName(sinkContextInfo.getNamespace());
            }
            if (StringUtil.isEmpty(vulnerability.getClassName())) {
                vulnerability.setClassName(sinkContextInfo.getClassName());
            }
            if (StringUtil.isEmpty(vulnerability.getFunctionName())) {
                vulnerability.setFunctionName(sinkContextInfo.getFunctionName());
            }
        }
    }

    private FVDLMetadata.ContextInfo resolveContextInfo(Node node) {
        if (node == null || StringUtil.isEmpty(node.getContextId())) {
            return null;
        }
        return fvdlMetadata.getContextPool().get(node.getContextId());
    }

    private Node getSourceNode(StreamedVulnerability streamedVulnerability) {
        List<Node> sourceTraceNodes = getSourceTraceNodes(streamedVulnerability);
        return sourceTraceNodes.isEmpty() ? null : sourceTraceNodes.get(0);
    }

    private Node getSinkNode(StreamedVulnerability streamedVulnerability) {
        List<Node> sinkTraceNodes = getSinkTraceNodes(streamedVulnerability);
        return sinkTraceNodes.isEmpty() ? null : sinkTraceNodes.get(sinkTraceNodes.size() - 1);
    }

    private List<Node> getSourceTraceNodes(StreamedVulnerability streamedVulnerability) {
        return getTraceNodes(streamedVulnerability, 0);
    }

    private List<Node> getSinkTraceNodes(StreamedVulnerability streamedVulnerability) {
        if (streamedVulnerability.getTraces() == null || streamedVulnerability.getTraces().isEmpty()) {
            return Collections.emptyList();
        }
        return getTraceNodes(streamedVulnerability, streamedVulnerability.getTraces().size() - 1);
    }

    private List<Node> getTraceNodes(StreamedVulnerability streamedVulnerability, int traceIndex) {
        if (streamedVulnerability.getTraces() == null || streamedVulnerability.getTraces().isEmpty()) {
            return Collections.emptyList();
        }

        StreamedVulnerability.Trace trace = streamedVulnerability.getTraces().get(traceIndex);
        if (trace.getNodes() == null || trace.getNodes().isEmpty()) {
            return Collections.emptyList();
        }

        return trace.getNodes();
    }

    /**
     * Initialize package name from a file path.
     * Extracts the first directory component as the project name.
     *
     * @param filePath File path to derive package from
     * @return Derived package name or the full path if no separator found
     */
    static String createPackageName(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "";
        }

        String shortFileName = new java.io.File(filePath).getName();
        String dirName = filePath.substring(0, Math.max(0, filePath.length() - shortFileName.length()));
        if (dirName.isEmpty()) {
            return "<none>";
        }

        String packageName = dirName.replace('\\', '.').replace('/', '.');
        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }

        String[] packageHeaders = new String[]{"com", "org", "net", "src"};
        for (String packageHeader : packageHeaders) {
            String marker = "." + packageHeader + ".";
            int index = packageName.lastIndexOf(marker);
            if (index != -1) {
                return packageName.substring(index + 1);
            }
        }
        return packageName;
    }

    /**
     * Process auxiliary data and request-related fields from StreamedVulnerability.
     * Replicates the functionality of auxiliaryProcessor.process() and processRequestRelated().
     *
     * Now uses the fully parsed auxiliaryData and externalEntries from streaming parse.
     *
     * @param streamedVuln StreamedVulnerability from streaming parser
     * @param vulnCustom Vulnerability object to populate
     */
    private void processAuxiliaryAndRequestData(
        StreamedVulnerability streamedVuln,
        Vulnerability vulnCustom) {

        // Set auxiliaryData and externalEntries on vulnerability
        // Convert StreamedVulnerability.ExternalEntry to Entry objects
        List<FvdlExternalEntry> convertedEntries = convertExternalEntries(streamedVuln.getExternalEntries());
        vulnCustom.setAuxiliaryData(streamedVuln.getAuxiliaryData());
        vulnCustom.setExternalEntries(convertedEntries);

        // Process external IDs - add to knowledge map
        if (streamedVuln.getExternalIds() != null) {
            for (Map.Entry<String, String> externalId : streamedVuln.getExternalIds().entrySet()) {
                vulnCustom.getKnowledge().put("externalID." + externalId.getKey(), externalId.getValue());
            }
        }

        // Extract DAST fields from auxiliary data and external entries
        processRequestRelatedFromData(vulnCustom, streamedVuln.getAuxiliaryData(), convertedEntries);
    }

    /**
     * Convert streamed external entries to the Entry type used by Vulnerability.
     *
     * @param streamedEntries List of StreamedVulnerability.ExternalEntry
     * @return List of converted Entry objects
     */
    private List<FvdlExternalEntry> convertExternalEntries(
        List<StreamedVulnerability.ExternalEntry> streamedEntries) {

        if (streamedEntries == null) {
            return new ArrayList<>();
        }

        List<FvdlExternalEntry> result = new ArrayList<>(streamedEntries.size());
        for (StreamedVulnerability.ExternalEntry streamedEntry : streamedEntries) {
            FvdlExternalEntry entry = new FvdlExternalEntry();
            entry.setUrl(streamedEntry.getUrl());

            // Convert fields
            List<FvdlExternalEntry.Field> fields = entry.getFields();
            if (streamedEntry.getFields() != null) {
                for (StreamedVulnerability.EntryField streamedField : streamedEntry.getFields()) {
                    FvdlExternalEntry.Field field = new FvdlExternalEntry.Field();
                    field.setName(streamedField.getName());
                    field.setValue(streamedField.getValue());
                    field.setType(streamedField.getType());
                    field.setVulnTag(streamedField.getVulnTag());
                    fields.add(field);
                }
            }

            // Note: Function and SourceLocation objects are not fully converted here
            // If needed, create JAXB objects from the streamed data
            // For now, the essential fields (URL and Fields) are converted

            result.add(entry);
        }

        return result;
    }

    /**
     * Process request-related fields from auxiliary data and external entries.
     *
     * To use this, you need to parse and store auxiliaryData and externalEntries during
     * streaming parse phase.
     *
     * @param vulnCustom Vulnerability object to populate
     * @param auxData List of auxiliary data maps
     * @param externalEntries List of external entries
     */
    private void processRequestRelatedFromData(
        Vulnerability vulnCustom,
        List<Map<String, String>> auxData,
        List<FvdlExternalEntry> externalEntries) {

        // Process auxiliary data
        if (auxData != null) {
            for (Map<String, String> aux : auxData) {
                String contentType = aux.get("contentType");
                if (contentType != null) {
                    switch (contentType.toLowerCase()) {
                        case "requestheaders":
                            vulnCustom.setRequestHeaders(aux.values().stream()
                                .filter(v -> !v.equals(contentType))
                                .collect(java.util.stream.Collectors.joining(",")));
                            break;
                        case "requestparameters":
                            vulnCustom.setRequestParameters(aux.values().stream()
                                .filter(v -> !v.equals(contentType))
                                .collect(java.util.stream.Collectors.joining(",")));
                            break;
                        case "requestbody":
                            vulnCustom.setRequestBody(aux.get("value"));
                            break;
                        case "requestmethod":
                            vulnCustom.setRequestMethod(aux.get("value"));
                            break;
                        case "requestcookies":
                            vulnCustom.setRequestCookies(aux.get("value"));
                            break;
                        case "requesthttpversion":
                            vulnCustom.setRequestHttpVersion(aux.get("value"));
                            break;
                        case "attackpayload":
                            vulnCustom.setAttackPayload(aux.get("value"));
                            break;
                        case "attacktype":
                            vulnCustom.setAttackType(aux.get("value"));
                            break;
                        case "response":
                            vulnCustom.setResponse(aux.get("value"));
                            break;
                        case "trigger":
                            vulnCustom.setTrigger(aux.get("value"));
                            break;
                        case "vulnerableparameter":
                            vulnCustom.setVulnerableParameter(aux.get("value"));
                            break;
                    }
                }
            }
        }

        // Process external entries
        if (externalEntries != null) {
            for (FvdlExternalEntry entry : externalEntries) {
                if (entry.getUrl() != null && entry.getUrl().toLowerCase().contains("request")) {
                    for (FvdlExternalEntry.Field field : entry.getFields()) {
                        switch (field.getName().toLowerCase()) {
                            case "requestheaders":
                                vulnCustom.setRequestHeaders(field.getValue());
                                break;
                            case "requestparameters":
                                vulnCustom.setRequestParameters(field.getValue());
                                break;
                            case "requestbody":
                                vulnCustom.setRequestBody(field.getValue());
                                break;
                            case "requestmethod":
                                vulnCustom.setRequestMethod(field.getValue());
                                break;
                            case "requestcookies":
                                vulnCustom.setRequestCookies(field.getValue());
                                break;
                            case "requesthttpversion":
                                vulnCustom.setRequestHttpVersion(field.getValue());
                                break;
                            case "attackpayload":
                                vulnCustom.setAttackPayload(field.getValue());
                                break;
                            case "attacktype":
                                vulnCustom.setAttackType(field.getValue());
                                break;
                            case "response":
                                vulnCustom.setResponse(field.getValue());
                                break;
                            case "trigger":
                                vulnCustom.setTrigger(field.getValue());
                                break;
                            case "vulnerableparameter":
                                vulnCustom.setVulnerableParameter(field.getValue());
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Find the longest stack trace.
     */
    private List<StackTraceElement> findLongestList(
        List<List<StackTraceElement>> listOfLists) {

        if (listOfLists == null || listOfLists.isEmpty()) {
            return new ArrayList<>();
        }
        return listOfLists.stream()
            .max(Comparator.comparingInt(this::getTotalTraceSize))
            .orElse(new ArrayList<>());
    }

    /**
     * Calculate total trace size including nested elements.
     */
    private int getTotalTraceSize(List<StackTraceElement> trace) {
        if (trace == null) {
            return 0;
        }
        return trace.stream()
            .mapToInt(this::countNodesRecursive)
            .sum();
    }

    /**
     * Recursively count nodes in a stack trace element.
     */
    private int countNodesRecursive(StackTraceElement element) {
        if (element == null) {
            return 0;
        }
        int count = 1;
        if (element.getInnerStackTrace() != null) {
            for (StackTraceElement inner : element.getInnerStackTrace()) {
                count += countNodesRecursive(inner);
            }
        }
        return count;
    }

    /**
     * Aggregate taint flags and knowledge from all traces.
     */
    private void aggregateFromTraces(Vulnerability vulnCustom) {
        Set<String> allTaintFlags = new HashSet<>();
        Map<String, String> allKnowledge = new HashMap<>(vulnCustom.getKnowledge());

        for (List<StackTraceElement> trace : vulnCustom.getStackTrace()) {
            for (StackTraceElement ste : trace) {
                if (ste.getTaintflags() != null && !ste.getTaintflags().isEmpty()) {
                    allTaintFlags.addAll(Arrays.stream(ste.getTaintflags().split(","))
                        .map(String::trim)
                        .collect(java.util.stream.Collectors.toSet()));
                }
                allKnowledge.putAll(ste.getKnowledge());

                if (ste.getInnerStackTrace() != null) {
                    for (StackTraceElement inner : ste.getInnerStackTrace()) {
                        if (inner.getTaintflags() != null && !inner.getTaintflags().isEmpty()) {
                            allTaintFlags.addAll(Arrays.stream(inner.getTaintflags().split(","))
                                .map(String::trim)
                                .collect(java.util.stream.Collectors.toSet()));
                        }
                        allKnowledge.putAll(inner.getKnowledge());
                    }
                }
            }
        }
        vulnCustom.setTaintFlags(new ArrayList<>(allTaintFlags));
        vulnCustom.setKnowledge(allKnowledge);
    }

}
