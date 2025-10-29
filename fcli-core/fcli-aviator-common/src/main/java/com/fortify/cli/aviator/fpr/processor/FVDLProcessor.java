/*
 * Copyright 2021-2025 Open Text.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.audit.model.File;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.VulnerabilityMapper;
import com.fortify.cli.aviator.fpr.jaxb.FVDL;
import com.fortify.cli.aviator.fpr.jaxb.MetaInfo;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNode;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedTrace;
import com.fortify.cli.aviator.fpr.model.Entry;
import com.fortify.cli.aviator.fpr.model.ReplacementData;
import com.fortify.cli.aviator.fpr.utils.FileUtils;
import com.fortify.cli.aviator.fpr.utils.XmlUtils;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.StringUtil;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.Getter;

/**
 * Orchestrates the processing of an FVDL file, extracting and finalizing vulnerabilities.
 */
public class FVDLProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FVDLProcessor.class);
    private final NodeProcessor nodeProcessor;
    private final TraceProcessor traceProcessor;
    private final SnippetProcessor snippetProcessor;
    private final DescriptionProcessor descriptionProcessor;
    private final MetaInfoProcessor metaInfoProcessor;
    private final AuxiliaryProcessor auxiliaryProcessor;
    private final VulnFinalizer vulnFinalizer;
    private final FileUtils fileUtils;
    private final FprHandle fprHandle;
    @Getter
    private List<Vulnerability> vulnerabilities;

    public FVDLProcessor(FprHandle fprHandle) {
        this.fprHandle = fprHandle;
        this.fileUtils = new FileUtils();

        Map<String, String> correctSourceMap = fprHandle.getSourceFileMap();

        this.nodeProcessor = new NodeProcessor(this.fprHandle, fileUtils, correctSourceMap);
        this.traceProcessor = new TraceProcessor(this.fprHandle, nodeProcessor, new SnippetProcessor(), fileUtils, correctSourceMap);
        this.snippetProcessor = new SnippetProcessor();
        this.descriptionProcessor = new DescriptionProcessor();
        this.metaInfoProcessor = new MetaInfoProcessor();
        this.auxiliaryProcessor = new AuxiliaryProcessor();
        this.vulnFinalizer = new VulnFinalizer();
    }

    /**
     * Processes an FVDL file and returns a list of vulnerabilities.
     *
     * @return List of processed Vulnerability objects
     * @throws JAXBException If XML unmarshalling fails
     * @throws IOException   If file access fails
     */
    public List<Vulnerability> processXML() throws Exception {
        Path fvdlFilePath = fprHandle.getPath("/audit.fvdl");

        List<Vulnerability> vulnerabilities = new ArrayList<>();
        FVDL fvdl = unmarshalFVDL(fvdlFilePath);
        if (fvdl == null) {
            logger.error("Failed to unmarshal FVDL file: {}", fvdlFilePath);
            return vulnerabilities;
        }

        // Process global sections
        metaInfoProcessor.process(fvdl.getEngineData());
        nodeProcessor.process(fvdl.getUnifiedNodePool());
        traceProcessor.process(fvdl.getUnifiedTracePool());
        snippetProcessor.process(fvdl.getSnippets());
        descriptionProcessor.process(fvdl.getDescription());

        for (com.fortify.cli.aviator.fpr.jaxb.Vulnerability vulnJAXB : fvdl.getVulnerabilities().getVulnerability()) {
            Vulnerability vulnCustom = processVulnerability(vulnJAXB);
            if (vulnCustom != null) {
                vulnerabilities.add(vulnCustom);
            }
        }
        this.vulnerabilities = vulnerabilities;
        return vulnerabilities;
    }

    /**
     * Unmarshals the FVDL file into a JAXB object.
     *
     * @param fvdlFilePath Path to the FVDL file
     * @return FVDL object or null if unmarshalling fails
     * @throws JAXBException If unmarshalling fails
     * @throws IOException   If file access fails
     */
    private FVDL unmarshalFVDL(Path fvdlFilePath) throws JAXBException, IOException {
        try (InputStream fis = Files.newInputStream(fvdlFilePath)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(FVDL.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            javax.xml.stream.XMLInputFactory xmlInputFactory = javax.xml.stream.XMLInputFactory.newInstance();
            xmlInputFactory.setProperty(javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(javax.xml.stream.XMLInputFactory.SUPPORT_DTD, false);
            javax.xml.stream.XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(fis);
            return (FVDL) unmarshaller.unmarshal(xmlStreamReader);
        } catch (javax.xml.stream.XMLStreamException e) {
            throw new JAXBException("Error creating secure XML stream reader", e);
        }
    }

    public Optional<String> getSourceFileContent(String relativePath) {
        String internalPath = fprHandle.getSourceFileMap().get(relativePath);
        if (internalPath == null) {
            logger.warn("Source file key not found in sourceFileMap: {}", relativePath);
            return Optional.empty();
        }
        Path actualSourcePath = fprHandle.getPath("/" + internalPath);
        try {
            return Optional.of(String.join("\n", fileUtils.readFileWithFallback(actualSourcePath)));
        } catch (RuntimeException e) {
            logger.warn("WARN: Could not read source file content for internal path {}: {}", actualSourcePath, e.getMessage());
            return Optional.empty();
        }
    }
    /**
     * Processes a single JAXB vulnerability into a rich, fully populated internal Vulnerability object.
     * This method orchestrates the aggregation of data from the rule definitions, instance-specific overrides,
     * the vulnerability trace, and other sections of the FVDL.
     *
     * @param vulnJAXB JAXB Vulnerability object from the FVDL.
     * @return A fully processed Vulnerability object ready for filtering and auditing, or null if creation fails.
     */
    private Vulnerability processVulnerability(com.fortify.cli.aviator.fpr.jaxb.Vulnerability vulnJAXB) {
        Optional<Vulnerability> optionalVuln = VulnerabilityMapper.fromJAXB(vulnJAXB);
        if (optionalVuln.isEmpty()) {
            String instanceId = (vulnJAXB != null && vulnJAXB.getInstanceInfo() != null) ? vulnJAXB.getInstanceInfo().getInstanceID() : "UNKNOWN";
            logger.warn("Skipping vulnerability with instance ID [{}] due to missing critical data.", instanceId);
            return null;
        }
        Vulnerability vulnCustom = optionalVuln.get();


        // 1. Get the base metadata from the global Rule definition.
        // We create a mutable copy to allow for overrides.
        Map<String, String> finalMetadata = new HashMap<>(metaInfoProcessor.getMetadataForRule(vulnCustom.getClassID()));

        // 2. Check for and apply any instance-specific metadata overrides from <InstanceInfo>.
        if (vulnJAXB.getInstanceInfo() != null && vulnJAXB.getInstanceInfo().getMetaInfo() != null) {
            for (MetaInfo.Group group : vulnJAXB.getInstanceInfo().getMetaInfo().getGroup()) {
                if (group.getName() != null && group.getValue() != null) {
                    finalMetadata.put(group.getName(), group.getValue().trim());
                    logger.trace("Overriding metadata for vuln [{}]: '{}' -> '{}'", vulnCustom.getInstanceID(), group.getName(), group.getValue());
                }
            }
        }

        // 3. Merge the final, combined metadata into the vulnerability's central knowledge map.
        vulnCustom.getKnowledge().putAll(finalMetadata);

        // 4. Populate the specific, high-level fields using the final merged data.
        vulnCustom.setAccuracy(XmlUtils.safeParseDouble(finalMetadata.get("Accuracy"), 0.0));
        vulnCustom.setImpact(XmlUtils.safeParseDouble(finalMetadata.get("Impact"), 0.0));
        vulnCustom.setProbability(XmlUtils.safeParseDouble(finalMetadata.get("Probability"), 0.0));

        String audience = finalMetadata.getOrDefault("audience", "");
        if (StringUtil.isEmpty(audience)) {
            audience = processVulnerabilityAudienceFromJAXB(vulnJAXB);
        }
        vulnCustom.setAudience(audience);
        vulnCustom.setFiletype(finalMetadata.getOrDefault("DefaultFile", ""));

        try {
            List<List<StackTraceElement>> stackTraces = new ArrayList<>();
            if (vulnJAXB.getAnalysisInfo() != null && vulnJAXB.getAnalysisInfo().getUnified() != null) {
                for (UnifiedTrace trace : vulnJAXB.getAnalysisInfo().getUnified().getTrace()) {
                    stackTraces.addAll(traceProcessor.resolveTrace(trace));
                }
            }
            vulnCustom.setStackTrace(stackTraces);

            Map<String, File> uniqueFiles = new LinkedHashMap<>();
            if (!stackTraces.isEmpty()) {
                List<StackTraceElement> firstStackTrace = stackTraces.get(0);
                List<StackTraceElement> lastStackTrace = stackTraces.get(stackTraces.size() - 1);

                if (!lastStackTrace.isEmpty()) {
                    processFileForElement(lastStackTrace.get(0), uniqueFiles);
                    processFileForElement(lastStackTrace.get(lastStackTrace.size() - 1), uniqueFiles);
                }
                processStackTraceElements(stackTraces, uniqueFiles);

                vulnCustom.setFiles(new ArrayList<>(uniqueFiles.values()));
                vulnCustom.setFirstStackTrace(firstStackTrace);
                vulnCustom.setSource(lastStackTrace.isEmpty() ? null : lastStackTrace.get(0));
                vulnCustom.setSink(lastStackTrace.isEmpty() ? null : lastStackTrace.get(lastStackTrace.size() - 1));
                vulnCustom.setLastStackTraceElement(lastStackTrace.isEmpty() ? null : lastStackTrace.get(lastStackTrace.size() - 1));
                vulnCustom.setLongestStackTrace(findLongestList(stackTraces));
                vulnCustom.setSource(firstStackTrace.isEmpty() ? null : firstStackTrace.get(0));
                vulnCustom.setSink(firstStackTrace.isEmpty() ? null : firstStackTrace.get(firstStackTrace.size() - 1));
            }
        } catch (IOException e) {
            logger.error("Failed to resolve traces for vuln ID: {}", vulnCustom.getInstanceID(), e);
            vulnCustom.setFiles(new ArrayList<>());
        }

        // Aggregate dynamic knowledge (like TaintFlags) from the processed trace nodes up to the vulnerability level.
        aggregateFromTraces(vulnCustom);

        // Process DAST / Auxiliary data into their respective fields
        auxiliaryProcessor.process(vulnJAXB, vulnCustom);
        processRequestRelated(vulnCustom, vulnCustom.getAuxiliaryData(), vulnCustom.getExternalEntries());

        // Process descriptions, providing the replacement data for rendering.
        ReplacementData replacementData = ReplacementParser.parse(vulnJAXB.getAnalysisInfo().getUnified().getReplacementDefinitions());
        String[] descs = descriptionProcessor.processForVuln(vulnCustom, vulnCustom.getClassID(), replacementData);
        vulnCustom.setShortDescription(StringUtil.stripTags(descs[0], true));
        vulnCustom.setExplanation(StringUtil.stripTags(descs[1], true));

        // The finalizer is now responsible for calculating derived fields (like likelihood and priority)
        // and applying any necessary fallbacks for data that was missing from the rule metadata.
        vulnFinalizer.finalize(vulnCustom);

        return vulnCustom;
    }

    private void processFileForElement(StackTraceElement element, Map<String, File> uniqueFiles) {
        if (element == null) return;

        String filename = element.getFilename();
        if (!StringUtil.isEmpty(filename) && fprHandle.getSourceFileMap().containsKey(filename) && !uniqueFiles.containsKey(filename)) {
            String internalPath = fprHandle.getSourceFileMap().get(filename);
            if (internalPath == null) { return; } // Should not happen due to containsKey check, but safe.

            Path actualSourcePath = fprHandle.getPath("/" + internalPath);

            File file = new File();
            file.setName(filename);
            file.setSegment(false);
            file.setStartLine(1);

            try {
                if (Files.exists(actualSourcePath)) {
                    byte[] encodedBytes = Files.readAllBytes(actualSourcePath);
                    file.setContent(new String(encodedBytes));
                    file.setEndLine(fileUtils.countLines(actualSourcePath));
                } else {
                    // This warning is now more accurate.
                    logger.warn("Source file not found at internal path: {}. This may indicate a corrupt FPR.", actualSourcePath);
                    file.setContent("");
                    file.setEndLine(0);
                }
            } catch (IOException e) {
                logger.warn("Error processing file: {}", filename, e);
                file.setContent("");
                file.setEndLine(0);
            }
            uniqueFiles.put(filename, file);
        }
    }

    private void processStackTraceElements(List<List<StackTraceElement>> stackTraces, Map<String, File> uniqueFiles) {
        for (List<StackTraceElement> stackTrace : stackTraces) {
            if (stackTrace == null) continue;
            for (StackTraceElement element : stackTrace) {
                processFileForElement(element, uniqueFiles);
                if (element.getInnerStackTrace() != null) {
                    for (StackTraceElement innerElement : element.getInnerStackTrace()) {
                        processFileForElement(innerElement, uniqueFiles);
                    }
                }
            }
        }
    }

    /**
     * Finds the stack trace with the most total nodes, including nested traces.
     *
     * @param listOfLists A list containing all traces for a vulnerability.
     * @return The single trace (a list of StackTraceElement) that is the "longest".
     */
    private List<StackTraceElement> findLongestList(List<List<StackTraceElement>> listOfLists) {
        if (listOfLists == null || listOfLists.isEmpty()) {
            return new ArrayList<>();
        }
        // This performs the same deep count as before, but in a more concise way.
        return listOfLists.stream()
                .max(Comparator.comparingInt(this::getTotalTraceSize))
                .orElse(new ArrayList<>());
    }

    /**
     * Calculates the total number of nodes in a single trace by summing the recursive
     * size of each of its top-level elements.
     *
     * @param trace A list of top-level StackTraceElements.
     * @return The total count of all nodes in the trace tree.
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
     * Recursively counts a StackTraceElement and all of its descendants in inner traces.
     *
     * @param element The root element to start counting from.
     * @return The total number of nodes in this element's tree.
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

    private void aggregateFromTraces(Vulnerability vulnCustom) {
        Set<String> allTaintFlags = new HashSet<>();
        Map<String, String> allKnowledge = new HashMap<>();
        for (List<StackTraceElement> trace : vulnCustom.getStackTrace()) {
            for (StackTraceElement ste : trace) {
                // TODO Fix names
                if (ste.getTaintflags() != null && !ste.getTaintflags().isEmpty()) {
                    allTaintFlags.addAll(Arrays.stream(ste.getTaintflags().split(",")).map(String::trim).collect(Collectors.toSet()));
                }
                allKnowledge.putAll(ste.getKnowledge());
                if (ste.getInnerStackTrace() != null) {
                    for (StackTraceElement inner : ste.getInnerStackTrace()) {
                        if (inner.getTaintflags() != null && !inner.getTaintflags().isEmpty()) {
                            allTaintFlags.addAll(Arrays.stream(inner.getTaintflags().split(",")).map(String::trim).collect(Collectors.toSet()));
                        }
                        allKnowledge.putAll(inner.getKnowledge());
                    }
                }
            }
        }
        vulnCustom.setTaintFlags(new ArrayList<>(allTaintFlags));
        vulnCustom.setKnowledge(allKnowledge);
    }

    private void processRequestRelated(com.fortify.cli.aviator.fpr.Vulnerability vulnCustom, List<Map<String, String>> auxData, List<Entry> externalEntries) {
        for (Map<String, String> aux : auxData) {
            String contentType = aux.get("contentType");
            if (contentType != null) {
                switch (contentType.toLowerCase()) {
                    case "requestheaders":
                        vulnCustom.setRequestHeaders(aux.values().stream().filter(v -> !v.equals(contentType)).collect(Collectors.joining(",")));
                        break;
                    case "requestparameters":
                        vulnCustom.setRequestParameters(aux.values().stream().filter(v -> !v.equals(contentType)).collect(Collectors.joining(",")));
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
        for (Entry entry : externalEntries) {
            if (entry.getUrl() != null && entry.getUrl().toLowerCase().contains("request")) {
                for (Entry.Field field : entry.getFields()) {
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

    private String processVulnerabilityAudienceFromJAXB(com.fortify.cli.aviator.fpr.jaxb.Vulnerability vulnJAXB) {
        Set<String> allRuleIds = new HashSet<>();
        allRuleIds.add(vulnJAXB.getClassInfo().getClassID());
        if (vulnJAXB.getAnalysisInfo().getUnified() != null && !vulnJAXB.getAnalysisInfo().getUnified().getTrace().isEmpty()) {
            for (UnifiedTrace trace : vulnJAXB.getAnalysisInfo().getUnified().getTrace()) {
                if (trace.getPrimary() == null) continue;
                for (UnifiedTrace.Primary.Entry entry : trace.getPrimary().getEntry()) {
                    if (entry.getNode() != null) {
                        UnifiedNode node = entry.getNode();
                        if (node.getReason() != null && node.getReason().getTraceOrTraceRefOrInductionRef() != null) {
                            for (Object reasonObj : node.getReason().getTraceOrTraceRefOrInductionRef()) {
                                if (reasonObj instanceof UnifiedNode.Reason.Rule rule) {
                                    allRuleIds.add(rule.getRuleID());
                                }
                            }
                        }
                    }
                }
            }
        }

        Set<String> intersection = null;
        for (String ruleId : allRuleIds) {
            Map<String, String> ruleMetadata = metaInfoProcessor.getMetadataForRule(ruleId);
            String aud = ruleMetadata.get("audience");

            if (!StringUtil.isEmpty(aud)) {
                Set<String> current = Arrays.stream(aud.split(",")).map(String::trim).collect(Collectors.toSet());
                if (intersection == null) {
                    intersection = new HashSet<>(current);
                } else {
                    intersection.retainAll(current);
                }
                if (intersection.isEmpty()) break;
            }
        }
        return intersection == null || intersection.isEmpty() ? "" : String.join(",", intersection);
    }
}
