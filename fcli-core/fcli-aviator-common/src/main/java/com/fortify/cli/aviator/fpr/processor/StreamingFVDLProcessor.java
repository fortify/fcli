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

import static com.fortify.cli.aviator.fpr.processor.XmlParserUtils.*;

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

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.AnalyzerType;
import com.fortify.cli.aviator.fpr.model.*;
import com.fortify.cli.aviator.fpr.utils.FileUtils;
import com.fortify.cli.aviator.fpr.utils.XmlUtils;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.StringUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class StreamingFVDLProcessor {
    private static final Logger logger = LoggerFactory.getLogger(StreamingFVDLProcessor.class);
    private final XMLInputFactory xmlInputFactory;
    @Getter
    private final FVDLMetadata fvdlMetadata;
    private final VulnFinalizer vulnFinalizer;
    private final FileUtils fileUtils;
    private final DescriptionProcessor descriptionProcessor;
    @Getter
    private final Map<String, String> sourceFileMap;
    private final List<StreamedVulnerability> rawVulnerabilities;
    @Getter
    private final List<Vulnerability> vulnerabilities;
    private final FprHandle fprHandle;
    /*private final Path extractedPath;
    private final IndexXMLProcessor indexXMLProcessor;*/

    // Specialized parsers
    private final MemoryTracker memoryTracker;
    private final NodeParser nodeParser;
    private final TraceParser traceParser;
    private final DescriptionParser descriptionParser;
    private final MetadataParser metadataParser;

    // Peak memory tracking
    private long peakMemoryBeforeParsing = 0;
    private long peakMemoryPass1 = 0;
    private long peakMemoryPass2 = 0;
    private long peakMemoryPostProcessing = 0;

    public StreamingFVDLProcessor(FprHandle fprHandle){
        this.vulnFinalizer = new VulnFinalizer();
        this.fileUtils = new FileUtils();
        this.fprHandle = fprHandle;
        this.sourceFileMap = fprHandle.getSourceFileMap();
        this.xmlInputFactory = XMLInputFactory.newInstance();
        // Security: Disable external entity processing
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        //this.parsingMetadata = new ParsingMetadata();
        this.fvdlMetadata = new FVDLMetadata();
        this.rawVulnerabilities = new ArrayList<>();
        this.vulnerabilities = new ArrayList<>();
        this.descriptionProcessor = new DescriptionProcessor();

        // Initialize specialized parsers
        this.memoryTracker = new MemoryTracker();
        this.traceParser = new TraceParser(fvdlMetadata.getNodePool());
        this.nodeParser = new NodeParser(traceParser);
        this.traceParser.setNodeParser(nodeParser); // Circular dependency for Reason parsing
        this.descriptionParser = new DescriptionParser();
        this.metadataParser = new MetadataParser();
        /*this.extractedPath = extractedPath;
        this.indexXMLProcessor = new IndexXMLProcessor(extractedPath, sourceFileMap);*/
    }


    /**
     * Parse an FPR file using two-pass parsing strategy.
     *
     * TWO-PASS PARSING STRATEGY:
     * Pass 1: Parse metadata and pools (NodePool, TracePool, Descriptions, EngineData)
     *         This populates the reference pools needed for vulnerability parsing.
     * Pass 2: Parse Vulnerabilities with fully populated pools
     *         NodeRef lookups now succeed because NodePool is populated.
     *
     * @param zipFile zipFile for FPR file
     * @param entryName entry name for FVDL file
     * @throws Exception If file access or parsing fails
     */
    public void parse(ZipFile zipFile, String entryName) throws Exception {
        logger.info("=== Two-Pass Streaming Parsing Started for FVDL file ===");

        // Initialize memory tracking
        memoryTracker.initializeBaseline();
        memoryTracker.logMemoryConsumption("Before Parsing (Baseline)");

        /*logger.info("Loading source file map");
        //indexXMLProcessor.loadSourceFileMap();*/

        // Update peak after loading source file map
        memoryTracker.updatePass1Peak();


        ZipEntry fvdlEntry = zipFile.getEntry(entryName);
        if (fvdlEntry == null) {
            throw new IOException("audit.fvdl not found in FPR file");
        }

        try {
            // ========================================
            // PASS 1: Parse Metadata and Pools
            // ========================================
            logger.info(">>> PASS 1: Parsing metadata and pools (NodePool, TracePool, Descriptions, EngineData)");
            long pass1Start = System.currentTimeMillis();
            memoryTracker.initializePass1Peak();

            try (InputStream is1 = zipFile.getInputStream(fvdlEntry)) {
                parseMetadataAndPools(is1);
                memoryTracker.updatePass1Peak();
            }

            long pass1Time = System.currentTimeMillis() - pass1Start;
            logger.info("<<< PASS 1 Complete - Time: {}ms", pass1Time);

            memoryTracker.logMemoryConsumptionWithPeak("After PASS 1", memoryTracker.getPeakMemoryPass1(),
                fvdlMetadata, rawVulnerabilities, vulnerabilities);

            // ========================================
            // PASS 2: Parse Vulnerabilities
            // ========================================
            logger.info(">>> PASS 2: Parsing vulnerabilities (with populated pools)");
            long pass2Start = System.currentTimeMillis();
            memoryTracker.initializePass2Peak();

            try (InputStream is2 = zipFile.getInputStream(fvdlEntry)) {
                parseVulnerabilitiesOnly(is2);
                memoryTracker.updatePass2Peak();
            }

            long pass2Time = System.currentTimeMillis() - pass2Start;
            logger.info("<<< PASS 2 Complete - Time: {}ms", pass2Time);
            logger.info("    - Vulnerabilities parsed: {}", rawVulnerabilities.size());
            memoryTracker.logMemoryConsumptionWithPeak("After PASS 2", memoryTracker.getPeakMemoryPass2(),
                fvdlMetadata, rawVulnerabilities, vulnerabilities);

            // ========================================
            // Post-Processing: Enrich Vulnerabilities (Batch Processing)
            // ========================================
            logger.info(">>> Post-Processing: Enriching vulnerabilities in batches");
            long processStart = System.currentTimeMillis();
            memoryTracker.initializePostProcessingPeak();

            int batchSize = 1000;
            int totalVulns = rawVulnerabilities.size();
            int totalBatches = (int) Math.ceil((double) totalVulns / batchSize);

            for (int batchNum = 0; batchNum < totalBatches; batchNum++) {
                int start = batchNum * batchSize;
                int end = Math.min(start + batchSize, totalVulns);
                // Process current batch
                for (int i = start; i < end; i++) {
                    StreamedVulnerability rawVuln = rawVulnerabilities.get(i);
                    Vulnerability enrichedVuln = processVulnerability(rawVuln, vulnFinalizer);
                    vulnerabilities.add(enrichedVuln);

                    // Null out processed raw vulnerability to enable GC
                    rawVulnerabilities.set(i, null);
                }

                // Track peak memory every 10 batches
                if (batchNum % 10 == 0) {
                    memoryTracker.updatePostProcessingPeak();
                }
            }

            // Final peak check
            memoryTracker.updatePostProcessingPeak();

            long processTime = System.currentTimeMillis() - processStart;
            logger.info("<<< Post-Processing Complete - Time: {}ms", processTime);
            logger.info("    - Processed {} vulnerabilities in {} batches", totalVulns, totalBatches);
            memoryTracker.logMemoryConsumptionWithPeak("After Post-Processing",
                memoryTracker.getPeakMemoryPostProcessing(), fvdlMetadata, rawVulnerabilities, vulnerabilities);

            // ========================================
            // Memory Optimization: Clear DescriptionCache and RuleMetadata
            // ========================================
            logger.info(">>> Memory Optimization: Clearing DescriptionCache and RuleMetadata");

            long descCount = fvdlMetadata.getDescriptionCache().size();
            long descMemoryEstimate = memoryTracker.estimateDescriptionCacheMemory(fvdlMetadata);
            fvdlMetadata.getDescriptionCache().clear();
            logger.info("    - Cleared {} descriptions (~{} MB)",
                descCount, String.format("%.2f", descMemoryEstimate / (1024.0 * 1024.0)));

            long ruleCount = fvdlMetadata.getRuleMetadata().size();
            long ruleMemoryEstimate = memoryTracker.estimateRuleMetadataMemory(fvdlMetadata);
            fvdlMetadata.getRuleMetadata().clear();
            logger.info("    - Cleared {} rule metadata entries (~{} KB)",
                ruleCount, String.format("%.2f", ruleMemoryEstimate / 1024.0));

            memoryTracker.logMemoryConsumption("After Clearing Metadata Caches");

            // ========================================
            // Memory Optimization: Clear Raw Vulnerabilities
            // ========================================
            logger.info(">>> Memory Optimization: Clearing raw vulnerabilities list");
            long rawVulnCount = rawVulnerabilities.size();
            long rawVulnMemoryEstimate = memoryTracker.estimateVulnerabilitiesMemory(rawVulnerabilities);
            rawVulnerabilities.clear();
            logger.info("    - Cleared {} raw vulnerabilities (~{} MB)",
                rawVulnCount, String.format("%.2f", rawVulnMemoryEstimate / (1024.0 * 1024.0)));
            memoryTracker.logMemoryConsumption("After Clearing Raw Vulnerabilities");

            // ========================================
            // OPTIONAL: Clear NodePool and TracePool
            // ========================================
            logger.info(">>> Memory Optimization: Clearing NodePool and TracePool");
            long nodePoolSize = fvdlMetadata.getNodePool().size();
            long tracePoolSize = fvdlMetadata.getTracePool().size();
            long nodePoolMemoryEstimate = memoryTracker.estimateNodePoolMemory(fvdlMetadata);
            long tracePoolMemoryEstimate = memoryTracker.estimateTracePoolMemory(fvdlMetadata);
            fvdlMetadata.getNodePool().clear();
            fvdlMetadata.getTracePool().clear();
            logger.info("    - Cleared NodePool ({} nodes, ~{} MB) and TracePool ({} traces, ~{} MB)",
                nodePoolSize, String.format("%.2f", nodePoolMemoryEstimate / (1024.0 * 1024.0)),
                tracePoolSize, String.format("%.2f", tracePoolMemoryEstimate / (1024.0 * 1024.0)));
            memoryTracker.logMemoryConsumption("After Clearing Pools");

            // ========================================
            // Summary
            // ========================================
            long totalTime = pass1Time + pass2Time + processTime;
            logger.info("=== Two-Pass Parsing Complete ===");
            logger.info("    Total Time: {}ms (Pass1: {}ms, Pass2: {}ms, Process: {}ms)",
                totalTime, pass1Time, pass2Time, processTime);
            logger.info("    Overhead: ~{:.1f}% compared to single-pass estimate",
                ((pass1Time + pass2Time) / (double)(pass1Time + pass2Time) * 100) - 100);

        } catch (XMLStreamException e) {
            throw new IOException("Failed to parse FVDL: " + e.getMessage(), e);
        }

    }


    /**
     * PASS 1: Parse metadata and pools only.
     * Skips Vulnerabilities section for later processing in Pass 2.
     *
     * Parsed sections:
     * - EngineData (rule metadata)
     * - UnifiedNodePool (node definitions)
     * - UnifiedTracePool (trace definitions)
     * - Description (vulnerability descriptions)
     * - Build (skipped)
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
                        case "Build":
                            logger.debug("Pass 1: Skipping Build");
                            // Build is already skipped in parseEngineData
                            break;
                        case "UnifiedNodePool":
                            logger.debug("Pass 1: Parsing UnifiedNodePool");
                            parseNodePool(reader);
                            break;
                        case "UnifiedTracePool":
                            logger.debug("Pass 1: Parsing UnifiedTracePool");
                            parseTracePool(reader);
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
        metadataParser.parseEngineData(reader, fvdlMetadata.getRuleMetadata());
    }

    /**
     * Parse UnifiedNodePool section.
     * Delegates node parsing to NodeParser.
     */
    private void parseNodePool(XMLStreamReader reader) throws XMLStreamException {
        logger.debug("Start parse UnifiedNodePool");

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Node".equals(reader.getLocalName())) {
                    String nodeId = reader.getAttributeValue(null, "id");
                    logger.debug("Found Node with ID: {}", nodeId);

                    // Delegate to NodeParser
                    Node node = nodeParser.parseNode(reader, null);

                    if (node != null) {
                        fvdlMetadata.getNodePool().put(node.getId(), node);
                        fvdlMetadata.setTotalNodes(fvdlMetadata.getTotalNodes() + 1);
                        logger.debug("Successfully added node {} to pool. Total nodes: {}", node.getId(), fvdlMetadata.getNodePool().size());
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
        logger.debug("start Unified Trace pool");

        while (reader.hasNext()) {
            int event = reader.next();

            if((event == XMLStreamConstants.START_ELEMENT || event == XMLStreamConstants.END_ELEMENT))
                logger.debug("[parseTracePool] Event: {}, LocalName: {}", getEventTypeName(event), reader.getLocalName());

            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Trace".equals(reader.getLocalName())) {
                    String traceId = reader.getAttributeValue(null, "id");
                    logger.debug("Trace Id {} ", traceId);
                    if (traceId != null && !traceId.isEmpty()) {
                        logger.debug("TraceId not empty and not null {} ", traceId );
                        // Delegate to TraceParser
                        StreamedTrace trace =
                            traceParser.parseStreamedTrace(reader, traceId);
                        if (trace != null) {
                            fvdlMetadata.getTracePool().put(traceId, trace);
                            fvdlMetadata.setTotalTraces(fvdlMetadata.getTotalTraces() + 1);
                            logger.debug("Successfully added trace {} to pool. Total traces: {}", traceId, fvdlMetadata.getTracePool().size());
                        }
                    } else {
                        logger.warn("Trace missing or invalid ID, skipping");
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("UnifiedTracePool".equals(reader.getLocalName())) {
                    logger.debug("Reached end of UnifiedTracePool, exiting loop");
                    break;
                }
            }
        }
        logger.debug("Trace processed are {} ", fvdlMetadata.getTracePool().size());
    }

    /**
     * Parse Descriptions section for vulnerability descriptions.
     * Delegates to DescriptionParser.
     */
    private void parseDescriptions(XMLStreamReader reader) throws XMLStreamException {
        descriptionParser.parseDescriptions(reader, fvdlMetadata.getDescriptionCache());
    }

    private void parseVulnerabilities(XMLStreamReader reader) throws XMLStreamException {
        logger.info("Start parse Vulnerabilities");

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Vulnerability".equals(reader.getLocalName())) {
                    StreamedVulnerability vuln = parseVulnerability(reader);
                    rawVulnerabilities.add(vuln);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("Vulnerabilities".equals(reader.getLocalName())) {
                    return;
                }
            }
        }
    }

    private StreamedVulnerability parseVulnerability(XMLStreamReader reader) throws XMLStreamException {
        logger.debug("=== START parseVulnerability ===");
        StreamedVulnerability.StreamedVulnerabilityBuilder builder = StreamedVulnerability.builder();
        List<StreamedVulnerability.Trace> traces = new ArrayList<>();
        List<Map<String, String>> auxiliaryDataList = new ArrayList<>();
        List<StreamedVulnerability.ExternalEntry> externalEntriesList = new ArrayList<>();
        Map<String, String> externalIdsMap = new HashMap<>();

        String currentSection = null;
        logger.debug("[parseVulnerability] Initialized traces list: {}", traces.size());

        // Process until we hit the CLOSING </Vulnerability> tag
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                logger.debug("[parseVulnerability] START_ELEMENT: {}", localName);

                switch (localName) {
                    // ...existing code...
                    case "ClassInfo":
                        currentSection = "ClassInfo";
                        break;
                    case "InstanceInfo":
                        currentSection = "InstanceInfo";
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
                        logger.debug("[parseVulnerability] Found Trace element. Current traces count: {}", traces.size());
                        StreamedVulnerability.Trace trace = parseTrace(reader);
                        if (trace != null) {
                            traces.add(trace);
                            logger.debug("[parseVulnerability] Added trace with {} nodes. Total traces: {}",
                                trace.getNodes().size(), traces.size());
                        }
                        break;
                    case "ReplacementDefinitions":
                        // Parse ReplacementDefinitions from AnalysisInfo->Unified->ReplacementDefinitions
                        builder.replacementData(parseReplacementDefinitions(reader));
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

        int totalNodes = traces.stream().mapToInt(t -> t.getNodes() != null ? t.getNodes().size() : 0).sum();
        logger.debug("[parseVulnerability] Building vulnerability - traces: {}, total nodes: {}", traces.size(), totalNodes);
        builder.traces(traces);
        builder.auxiliaryData(auxiliaryDataList);
        builder.externalEntries(externalEntriesList);
        builder.externalIds(externalIdsMap);

        StreamedVulnerability vuln = builder.build();
        int vulnTotalNodes = vuln.getTraces() != null ?
            vuln.getTraces().stream().mapToInt(t -> t.getNodes() != null ? t.getNodes().size() : 0).sum() : 0;
        logger.debug("[parseVulnerability] Built vulnerability - traces: {}, total nodes: {}",
            vuln.getTraces() != null ? vuln.getTraces().size() : "NULL", vulnTotalNodes);

        // Check which fields are populated
        // VulnerabilityLoggingUtils.logPopulatedFields(vuln);

        //System.out.println("Traces count: " + vuln.getTraces().size());
        //populateFilesForVulnerability(vuln, fprPath, sourceFileMap, fileUtils);
        //return builder.build();
        return vuln;
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
            logger.debug("Parsed {} MetaInfo groups", instanceMetadata.size());
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
     * Structure: <AuxiliaryData contentType="..."><AuxField name="..." value="..."><SourceLocation/></AuxField></AuxiliaryData>
     * Replicates AuxiliaryProcessor logic for streaming parsing.
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
     * Structure: <ExternalEntries><Entry><URL>...</URL><Function/><SourceLocation/><Fields><Field/></Fields></Entry></ExternalEntries>
     * Replicates AuxiliaryProcessor ExternalEntries logic.
     *
     * @param reader XMLStreamReader positioned at ExternalEntries start element
     * @param externalEntriesList List to add parsed entries to
     */
    private void parseExternalEntries(XMLStreamReader reader, List<StreamedVulnerability.ExternalEntry> externalEntriesList)
        throws XMLStreamException {

        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Entry".equals(reader.getLocalName())) {
                    StreamedVulnerability.ExternalEntry entry = parseExternalEntry(reader);
                    if (entry != null) {
                        externalEntriesList.add(entry);
                    }
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }
    }

    /**
     * Parse a single Entry element within ExternalEntries.
     *
     * @param reader XMLStreamReader positioned at Entry start element
     * @return Parsed ExternalEntry or null
     */
    private StreamedVulnerability.ExternalEntry parseExternalEntry(XMLStreamReader reader) throws XMLStreamException {
        StreamedVulnerability.ExternalEntry.ExternalEntryBuilder builder =
            StreamedVulnerability.ExternalEntry.builder();

        List<StreamedVulnerability.EntryField> fields = new ArrayList<>();

        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "URL":
                        builder.url(readElementText(reader));
                        continue;
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
                        continue;
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if ("Entry".equals(reader.getLocalName())) {
                    break;
                }
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

        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Field".equals(reader.getLocalName())) {
                    StreamedVulnerability.EntryField field = parseEntryField(reader);
                    if (field != null) {
                        fields.add(field);
                    }
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }
    }

    /**
     * Parse a single Field element within Fields.
     *
     * @param reader XMLStreamReader positioned at Field start element
     * @return Parsed EntryField or null
     */
    private StreamedVulnerability.EntryField parseEntryField(XMLStreamReader reader) throws XMLStreamException {
        StreamedVulnerability.EntryField.EntryFieldBuilder builder =
            StreamedVulnerability.EntryField.builder();

        // Get Field attributes
        String type = reader.getAttributeValue(null, "type");
        String vulnTag = reader.getAttributeValue(null, "vulnTag");
        builder.type(type);
        builder.vulnTag(vulnTag);

        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "Name":
                        builder.name(readElementText(reader));
                        continue;
                    case "Value":
                        builder.value(readElementText(reader));
                        continue;
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if ("Field".equals(reader.getLocalName())) {
                    break;
                }
            }
        }

        return builder.build();
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
     * This replicates the logic from ReplacementParser for streaming parsing.
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
                        logger.debug("Parsed Def: key={}, value={}, path={}", key, value, path);
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
                        logger.debug("Parsed LocationDef: key={}, path={}", key, path);
                    }
                }
                depth++;

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }

        logger.debug("Parsed ReplacementDefinitions: {} replacements, {} location replacements",
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
            .analysis(streamedVuln.getShortDescription())
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

        // Convert StreamedVulnerability Traces to StackTraces (hierarchical structure maintained)
        List<List<com.fortify.cli.aviator.audit.model.StackTraceElement>> stackTraces = new ArrayList<>();

        if (streamedVuln.getTraces() != null && !streamedVuln.getTraces().isEmpty()) {
            int totalNodes = streamedVuln.getTraces().stream()
                .mapToInt(t -> t.getNodes() != null ? t.getNodes().size() : 0)
                .sum();
            //logger.info("Traces count: {}, Total nodes: {}", streamedVuln.getTraces().size(), totalNodes);

            // Convert traces to stackTraces - already returns List<List<StackTraceElement>>
            stackTraces = convertTracesToStackTraces(streamedVuln.getTraces());
        }

        vulnCustom.setStackTrace(stackTraces);

        //Map<String, com.fortify.aviator.appsec.processor.sast.model.File> uniqueFiles = new java.util.LinkedHashMap<>();
        if (!stackTraces.isEmpty()) {
            List<com.fortify.cli.aviator.audit.model.StackTraceElement> firstStackTrace = stackTraces.get(0);
            List<com.fortify.cli.aviator.audit.model.StackTraceElement> lastStackTrace = stackTraces.get(stackTraces.size() - 1);
            vulnCustom.setFirstStackTrace(firstStackTrace);
            vulnCustom.setSource(lastStackTrace.isEmpty() ? null : lastStackTrace.get(0));
            vulnCustom.setSink(lastStackTrace.isEmpty() ? null : lastStackTrace.get(lastStackTrace.size() - 1));
            vulnCustom.setLastStackTraceElement(lastStackTrace.isEmpty() ? null : lastStackTrace.get(lastStackTrace.size() - 1));
            vulnCustom.setLongestStackTrace(findLongestList(stackTraces));
        }

        aggregateFromTraces(vulnCustom);

        // Process DAST / Auxiliary data - replicate auxiliaryProcessor.process() and processRequestRelated()
        processAuxiliaryAndRequestData(streamedVuln, vulnCustom);

        // Process descriptions from streaming-parsed descriptionCache in FVDLMetadata
        ReplacementData replacementData = streamedVuln.getReplacementData();
        String[] descs = descriptionProcessor.processForVuln(vulnCustom, vulnCustom.getClassID(), replacementData, fvdlMetadata);
        vulnCustom.setShortDescription(StringUtil.stripTags(descs[0], true));
        vulnCustom.setExplanation(StringUtil.stripTags(descs[1], true));

        // Set projectName from first file path if not already set
        // Since file content population is disabled, we derive it from stack trace
        if ((vulnCustom.getProjectName() == null || vulnCustom.getProjectName().isEmpty())
            && !stackTraces.isEmpty() && !stackTraces.get(0).isEmpty()) {
            com.fortify.cli.aviator.audit.model.StackTraceElement firstElement = stackTraces.get(0).get(0);
            if (firstElement != null && firstElement.getFilename() != null) {
                String firstFilePath = firstElement.getFilename();
                vulnCustom.setProjectName(initPackageName(firstFilePath));
            }
        }

        // Finalize to calculate derived fields
        vulnFinalizer.finalize(vulnCustom);

        return vulnCustom;
    }

    /**
     * Initialize package name from a file path.
     * Extracts the first directory component as the project name.
     *
     * @param filePath File path to derive package from
     * @return Derived package name or the full path if no separator found
     */
    private String initPackageName(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "";
        }
        int separatorIndex = filePath.indexOf('/');
        return separatorIndex > 0 ? filePath.substring(0, separatorIndex) : filePath;
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
        List<com.fortify.cli.aviator.fpr.model.Entry> convertedEntries = convertExternalEntries(streamedVuln.getExternalEntries());
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
     * Convert StreamedVulnerability.ExternalEntry objects to Entry objects.
     * Needed because Vulnerability class expects com.fortify.aviator.cli.fpr.models.Entry.
     *
     * @param streamedEntries List of StreamedVulnerability.ExternalEntry
     * @return List of converted Entry objects
     */
    private List<com.fortify.cli.aviator.fpr.model.Entry> convertExternalEntries(
        List<StreamedVulnerability.ExternalEntry> streamedEntries) {

        List<com.fortify.cli.aviator.fpr.model.Entry> result = new ArrayList<>();

        if (streamedEntries == null) {
            return result;
        }

        for (StreamedVulnerability.ExternalEntry streamedEntry : streamedEntries) {
            com.fortify.cli.aviator.fpr.model.Entry entry = new com.fortify.cli.aviator.fpr.model.Entry();
            entry.setUrl(streamedEntry.getUrl());

            // Convert fields
            List<com.fortify.cli.aviator.fpr.model.Entry.Field> fields = new ArrayList<>();
            if (streamedEntry.getFields() != null) {
                for (StreamedVulnerability.EntryField streamedField : streamedEntry.getFields()) {
                    com.fortify.cli.aviator.fpr.model.Entry.Field field =
                        new com.fortify.cli.aviator.fpr.model.Entry.Field();
                    field.setName(streamedField.getName());
                    field.setValue(streamedField.getValue());
                    field.setType(streamedField.getType());
                    field.setVulnTag(streamedField.getVulnTag());
                    fields.add(field);
                }
            }
            entry.setFields(fields);

            // Note: Function and SourceLocation objects are not fully converted here
            // If needed, create JAXB objects from the streamed data
            // For now, the essential fields (URL and Fields) are converted

            result.add(entry);
        }

        return result;
    }

    /**
     * Process request-related fields from auxiliary data and external entries.
     * This is a helper method that replicates FVDLProcessor.processRequestRelated() logic.
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
        List<com.fortify.cli.aviator.fpr.model.Entry> externalEntries) {

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
            for (com.fortify.cli.aviator.fpr.model.Entry entry : externalEntries) {
                if (entry.getUrl() != null && entry.getUrl().toLowerCase().contains("request")) {
                    for (com.fortify.cli.aviator.fpr.model.Entry.Field field : entry.getFields()) {
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
     * Convert TraceNodes from StreamedVulnerability to StackTraceElements.
     * Looks up taintFlags and knowledge from the Node pool using nodeId.
     */
    /**
     * Convert hierarchical Trace structure to StackTrace format.
     * Each Trace becomes a List<StackTraceElement>, maintaining the trace boundaries.
     *
     * @param traces List of Trace objects from StreamedVulnerability
     * @return List of StackTrace lists (List<List<StackTraceElement>>)
     */
    private List<List<com.fortify.cli.aviator.audit.model.StackTraceElement>> convertTracesToStackTraces(
        List<StreamedVulnerability.Trace> traces) {
        logger.debug("[StreamingFVDLParser][convertTracesToStackTraces] Converting {} traces",
            traces != null ? traces.size() : 0);

        List<List<com.fortify.cli.aviator.audit.model.StackTraceElement>> allStackTraces = new ArrayList<>();

        if (traces == null || traces.isEmpty()) {
            return allStackTraces;
        }

        // Process each trace separately to maintain boundaries
        for (StreamedVulnerability.Trace trace : traces) {
            List<com.fortify.cli.aviator.audit.model.StackTraceElement> stackTrace = new ArrayList<>();

            if (trace.getNodes() == null || trace.getNodes().isEmpty()) {
                logger.warn("Trace has no nodes, skipping");
                continue;
            }

            logger.debug("Processing trace with {} nodes", trace.getNodes().size());

            // Convert each Node in the trace to a StackTraceElement
            for (Node node : trace.getNodes()) {
                // Convert node using extracted helper method (ensures consistency)
                com.fortify.cli.aviator.audit.model.StackTraceElement element =
                    nodeToStackTraceElement(node);

                // NEW: Build innerStackTrace from Reason data
                List<com.fortify.cli.aviator.audit.model.StackTraceElement> innerStackTrace =
                    buildInnerStackTrace(node);
                if (!innerStackTrace.isEmpty()) {
                    element.setInnerStackTrace(innerStackTrace);
                    logger.debug("Set innerStackTrace with {} elements for node {}",
                        innerStackTrace.size(), node.getId());
                }

                stackTrace.add(element);
            }

            // Add this trace's stack trace to the collection
            allStackTraces.add(stackTrace);
            logger.debug("Converted trace to stackTrace with {} elements", stackTrace.size());
        }

        logger.debug("Converted {} traces to {} stackTraces", traces.size(), allStackTraces.size());
        return allStackTraces;
    }

    /**
     * Build innerStackTrace for a node based on its Reason element data.
     *
     * The Reason element can contain:
     * - Inline Traces: Parsed during node parsing and stored in reasonInlineTraces
     * - TraceRefs: References to TracePool traces, stored as IDs in reasonTraceRefs
     *
     * Both types are converted to StackTraceElements and combined into innerStackTrace.
     *
     * @param node Node with reasonTraceRefs and reasonInlineTraces from Reason parsing
     * @return List of StackTraceElements for innerStackTrace (empty if no Reason data)
     */
    private List<com.fortify.cli.aviator.audit.model.StackTraceElement> buildInnerStackTrace(
        Node node) {

        List<com.fortify.cli.aviator.audit.model.StackTraceElement> innerTrace = new ArrayList<>();

        if (node == null) {
            return innerTrace;
        }

        // Process inline traces from Reason element
        if (node.getReasonInlineTraces() != null && !node.getReasonInlineTraces().isEmpty()) {
            logger.debug("Building innerStackTrace from {} inline traces", node.getReasonInlineTraces().size());

            for (com.fortify.cli.aviator.fpr.model.StreamedTrace inlineTrace :
                node.getReasonInlineTraces()) {
                List<com.fortify.cli.aviator.audit.model.StackTraceElement> traceElements =
                    convertStreamedTraceToStackTrace(inlineTrace);
                innerTrace.addAll(traceElements);
            }
        }

        // Process trace references from Reason element (resolve from TracePool)
        if (node.getReasonTraceRefs() != null && !node.getReasonTraceRefs().isEmpty()) {
            logger.debug("Building innerStackTrace from {} trace refs", node.getReasonTraceRefs().size());

            for (String traceRefId : node.getReasonTraceRefs()) {
                com.fortify.cli.aviator.fpr.model.StreamedTrace referencedTrace =
                    fvdlMetadata.getTracePool().get(traceRefId);

                if (referencedTrace != null) {
                    List<com.fortify.cli.aviator.audit.model.StackTraceElement> traceElements =
                        convertStreamedTraceToStackTrace(referencedTrace);
                    innerTrace.addAll(traceElements);
                } else {
                    logger.warn("TraceRef {} not found in TracePool for node {}", traceRefId, node.getId());
                }
            }
        }

        if (!innerTrace.isEmpty()) {
            logger.debug("Built innerStackTrace with {} elements for node {}", innerTrace.size(), node.getId());
        }

        return innerTrace;
    }

    /**
     * Convert a StreamedTrace to StackTraceElements.
     *
     * This is similar to the main trace conversion logic but specifically for
     * Reason traces (used in innerStackTrace building).
     *
     * @param trace StreamedTrace from TracePool or inline from Reason element
     * @return List of StackTraceElements
     */
    private List<com.fortify.cli.aviator.audit.model.StackTraceElement> convertStreamedTraceToStackTrace(
        com.fortify.cli.aviator.fpr.model.StreamedTrace trace) {

        List<com.fortify.cli.aviator.audit.model.StackTraceElement> elements = new ArrayList<>();

        if (trace == null || trace.getPrimary() == null || trace.getPrimary().getEntries() == null) {
            return elements;
        }

        logger.debug("Converting StreamedTrace with {} entries", trace.getPrimary().getEntries().size());

        for (com.fortify.cli.aviator.fpr.model.StreamedTrace.Primary.Entry entry :
            trace.getPrimary().getEntries()) {

            // CHANGED: Use Node object directly from Entry instead of NodePool lookup
            // This preserves inline nodes (without IDs) that weren't added to NodePool
            Node node = entry.getNode();

            if (node != null) {
                // Convert node to StackTraceElement using extracted helper
                com.fortify.cli.aviator.audit.model.StackTraceElement element =
                    nodeToStackTraceElement(node);
                elements.add(element);
            } else {
                // Fallback: Try NodePool lookup if Entry doesn't have node object (shouldn't happen)
                String nodeId = entry.getNodeId();
                if (nodeId != null) {
                    node = fvdlMetadata.getNodePool().get(nodeId);
                    if (node != null) {
                        com.fortify.cli.aviator.audit.model.StackTraceElement element =
                            nodeToStackTraceElement(node);
                        elements.add(element);
                    } else {
                        logger.warn("Node {} referenced in trace not found (Entry has no node, NodePool lookup failed)", nodeId);
                    }
                } else {
                    logger.warn("Entry has null node and null nodeId - skipping");
                }
            }
        }

        logger.debug("Converted StreamedTrace to {} StackTraceElements", elements.size());
        return elements;
    }

    /**
     * Convert a Node to a StackTraceElement.
     *
     * Extracted from convertTracesToStackTraces() for reuse in innerStackTrace building.
     * This ensures consistent conversion logic for both main traces and inner traces.
     *
     * @param node Node to convert
     * @return StackTraceElement
     */
    private com.fortify.cli.aviator.audit.model.StackTraceElement nodeToStackTraceElement(
        Node node) {

        // Get taintFlags as comma-separated string
        String taintFlagsStr = "";
        if (node.getTaintFlags() != null && !node.getTaintFlags().isEmpty()) {
            taintFlagsStr = String.join(", ", node.getTaintFlags());
        }

        com.fortify.cli.aviator.audit.model.Fragment fragment =
            fileUtils.getFragmentFromFile(
                this.fprHandle,
                node.getFilePath(),
                node.getLine(),
                5,  // contextBefore - 5 lines before target line
                2   // contextAfter - 2 lines after target line
            );

        // Get code line from file
        String codeLine = fileUtils.getLineFromFile(this.fprHandle,
            node.getFilePath(), node.getLine());

        // StackTraceElement constructor: (filename, line, code, nodeType, fragment, additionalInfo, taintflags)
        com.fortify.cli.aviator.audit.model.StackTraceElement element =
            new com.fortify.cli.aviator.audit.model.StackTraceElement(
                node.getFilePath(),
                node.getLine(),
                codeLine,
                node.getActionType() != null ? node.getActionType() : "",
                fragment,
                node.getAdditionalInfo() != null ? node.getAdditionalInfo() : "",
                taintFlagsStr
            );

        // Set additional fields available via setters
        if (node.getLabel() != null && !node.getLabel().isEmpty()) {
            element.setReason(node.getLabel());
        }

        // Set knowledge map from Node
        if (node.getKnowledge() != null) {
            element.setKnowledge(node.getKnowledge());
        }

        return element;
    }
    /**
     * Find the longest stack trace.
     */
    private List<com.fortify.cli.aviator.audit.model.StackTraceElement> findLongestList(
        List<List<com.fortify.cli.aviator.audit.model.StackTraceElement>> listOfLists) {

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
    private int getTotalTraceSize(List<com.fortify.cli.aviator.audit.model.StackTraceElement> trace) {
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
    private int countNodesRecursive(com.fortify.cli.aviator.audit.model.StackTraceElement element) {
        if (element == null) {
            return 0;
        }
        int count = 1;
        if (element.getInnerStackTrace() != null) {
            for (com.fortify.cli.aviator.audit.model.StackTraceElement inner : element.getInnerStackTrace()) {
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
        Map<String, String> allKnowledge = new HashMap<>();

        for (List<com.fortify.cli.aviator.audit.model.StackTraceElement> trace : vulnCustom.getStackTrace()) {
            for (com.fortify.cli.aviator.audit.model.StackTraceElement ste : trace) {
                if (ste.getTaintflags() != null && !ste.getTaintflags().isEmpty()) {
                    allTaintFlags.addAll(Arrays.stream(ste.getTaintflags().split(","))
                        .map(String::trim)
                        .collect(java.util.stream.Collectors.toSet()));
                }
                allKnowledge.putAll(ste.getKnowledge());

                if (ste.getInnerStackTrace() != null) {
                    for (com.fortify.cli.aviator.audit.model.StackTraceElement inner : ste.getInnerStackTrace()) {
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
