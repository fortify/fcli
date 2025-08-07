package com.fortify.cli.aviator.fpr.processor;

import com.fortify.cli.aviator.audit.model.File;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.VulnerabilityMapper;
import com.fortify.cli.aviator.fpr.jaxb.FVDL;
import com.fortify.cli.aviator.fpr.jaxb.MetaInfo;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNode;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedTrace;
import com.fortify.cli.aviator.fpr.model.Entry;
import com.fortify.cli.aviator.fpr.model.ReplacementData;
import com.fortify.cli.aviator.fpr.utils.XmlUtils;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.util.StringUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Orchestrates the processing of an FVDL file, extracting and finalizing vulnerabilities.
 */
public class FVDLProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FVDLProcessor.class);
    private final Map<String, String> sourceFileMap = new ConcurrentHashMap<>();
    private final NodeProcessor nodeProcessor;
    private final TraceProcessor traceProcessor;
    private final SnippetProcessor snippetProcessor;
    private final DescriptionProcessor descriptionProcessor;
    private final MetaInfoProcessor metaInfoProcessor;
    private final AuxiliaryProcessor auxiliaryProcessor;
    private final VulnFinalizer vulnFinalizer;
    private final com.fortify.cli.aviator.fpr.utils.FileUtils fileUtils;
    private final Path extractedPath;
    @Getter
    private List<Vulnerability> vulnerabilities;

    // Configuration properties (e.g., cutoffs from library)
    private static final int ISSUE_CUTOFF_START = getProperty("PK_ISSUE_CUTOFF_START_INDEX", 0);
    private static final int ISSUE_CUTOFF_END = getProperty("PK_ISSUE_CUTOFF_END_INDEX", Integer.MAX_VALUE);
    private static final Map<String, int[]> CATEGORY_CUTOFFS = new HashMap<>();

    static {
        CATEGORY_CUTOFFS.put("SQL Injection", new int[]{0, 100});
    }

    public FVDLProcessor(Path extractedPath) {
        this.extractedPath = extractedPath;
        this.fileUtils = new com.fortify.cli.aviator.fpr.utils.FileUtils();
        this.nodeProcessor = new NodeProcessor(this.extractedPath, fileUtils, sourceFileMap);
        this.traceProcessor = new TraceProcessor(this.extractedPath, nodeProcessor, new SnippetProcessor(), fileUtils, sourceFileMap);        this.snippetProcessor = new SnippetProcessor();
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
        Path fvdlFilePath = extractedPath.resolve("audit.fvdl");

        List<Vulnerability> vulnerabilities = new ArrayList<>();
        FVDL fvdl = unmarshalFVDL(String.valueOf(fvdlFilePath));
        if (fvdl == null) {
            logger.error("Failed to unmarshal FVDL file: {}", fvdlFilePath);
            return vulnerabilities;
        }

        // Load source file map
        loadSourceFileMap();

        // Process global sections
        metaInfoProcessor.process(fvdl.getEngineData());
        nodeProcessor.process(fvdl.getUnifiedNodePool());
        traceProcessor.process(fvdl.getUnifiedTracePool());
        snippetProcessor.process(fvdl.getSnippets());
        descriptionProcessor.process(fvdl.getDescription());

        // Process vulnerabilities with cutoffs
        int totalCount = 0;
        Map<String, Integer> categoryCounts = new HashMap<>();
        // TODO Remove Limits
        for (com.fortify.cli.aviator.fpr.jaxb.Vulnerability vulnJAXB : fvdl.getVulnerabilities().getVulnerability()) {
            if (totalCount >= ISSUE_CUTOFF_START && totalCount < ISSUE_CUTOFF_END) {
                String category = vulnJAXB.getClassInfo().getType();
                int[] categoryCutoff = CATEGORY_CUTOFFS.getOrDefault(category, new int[]{0, Integer.MAX_VALUE});
                int categoryCount = categoryCounts.getOrDefault(category, 0);
                if (categoryCount >= categoryCutoff[0] && categoryCount < categoryCutoff[1]) {
                    Vulnerability vulnCustom = processVulnerability(vulnJAXB);
                    if (vulnCustom != null) {
                        vulnerabilities.add(vulnCustom);
                    }
                }
                categoryCounts.put(category, categoryCount + 1);
            }
            totalCount++;
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
    private FVDL unmarshalFVDL(String fvdlFilePath) throws JAXBException, IOException {
        try (FileInputStream fis = new FileInputStream(fvdlFilePath)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(FVDL.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (FVDL) unmarshaller.unmarshal(fis);
        }
    }

    /**
     * Loads the source file map from FVDL.
     */
    private void loadSourceFileMap() throws Exception {
        Path srcArchiveDir = extractedPath.resolve("src-archive");
        Path indexPath = null;

        if (directoryContainsSourceFiles(srcArchiveDir)) {
            indexPath = srcArchiveDir.resolve("index.xml");
        }

        if (indexPath == null) {
            throw new NoSuchFileException("'src-archive' contained no source files under " + extractedPath);
        } else if (!Files.exists(indexPath)) {
            throw new NoSuchFileException("A source directory was found, but its 'index.xml' is missing at: " + indexPath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document indexDoc = builder.parse(indexPath.toFile());

        NodeList entryNodes = indexDoc.getElementsByTagName("entry");
        for (int i = 0; i < entryNodes.getLength(); i++) {
            Element entry = (Element) entryNodes.item(i);
            String key = entry.getAttribute("key");
            String value = entry.getTextContent();
            sourceFileMap.put(key, value);
        }
    }

    private boolean directoryContainsSourceFiles(Path dirPath) throws IOException {
        if (!Files.isDirectory(dirPath)) {
            return false;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                boolean isRegularFile = Files.isRegularFile(path);
                boolean isNotIndexXml = !path.getFileName().toString().equals("index.xml");

                if (isRegularFile && isNotIndexXml) {
                    return true;
                }
            }
        }

        return false;
    }

    public Optional<String> getSourceFileContent(String relativePath) {
        String fullPathInZip = sourceFileMap.get(relativePath);
        if (fullPathInZip == null) {
            logger.debug("Source file key not found in sourceFileMap: {}", relativePath);
            return Optional.empty();
        }
        Path actualSourcePath = extractedPath.resolve(fullPathInZip);
        try {
            return Optional.of(String.join(System.lineSeparator(), fileUtils.readFileWithFallback(actualSourcePath)));
        } catch (RuntimeException e) {
            logger.warn("WARN: Could not read source file content: {}", relativePath, e);
            return Optional.empty();
        }
    }

    /**
     * Processes a single JAXB vulnerability into a rich, fully populated internal Vulnerability object.
     * This method orchestrates the aggregation of data from the rule definitions (via MetaInfoProcessor),
     * the vulnerability trace, and other sections of the FVDL.
     *
     * @param vulnJAXB JAXB Vulnerability object from the FVDL.
     * @return A fully processed Vulnerability object ready for filtering and auditing, or null if creation fails.
     */
    private Vulnerability processVulnerability(com.fortify.cli.aviator.fpr.jaxb.Vulnerability vulnJAXB) {
        // Use the safe builder/mapper pattern to create the base object.
        Optional<Vulnerability> optionalVuln = VulnerabilityMapper.fromJAXB(vulnJAXB);
        if (optionalVuln.isEmpty()) {
            String instanceId = (vulnJAXB != null && vulnJAXB.getInstanceInfo() != null) ? vulnJAXB.getInstanceInfo().getInstanceID() : "UNKNOWN";
            logger.warn("Skipping vulnerability with instance ID [{}] due to missing critical data.", instanceId);
            return null;
        }
        Vulnerability vulnCustom = optionalVuln.get();

        // --- METADATA AGGREGATION WITH INSTANCE OVERRIDE ---

        // 1. Get the base metadata from the global Rule definition.
        // We create a mutable copy to allow for overrides.
        Map<String, String> finalMetadata = new HashMap<>(metaInfoProcessor.getMetadataForRule(vulnCustom.getClassID()));

        // 2. Check for and apply any instance-specific metadata overrides from <InstanceInfo>.
        if (vulnJAXB.getInstanceInfo() != null && vulnJAXB.getInstanceInfo().getMetaInfo() != null) {
            for (MetaInfo.Group group : vulnJAXB.getInstanceInfo().getMetaInfo().getGroup()) {
                if (group.getName() != null && group.getValue() != null) {
                    // This will overwrite the value from the rule if the key is the same (e.g., "Probability").
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



        // --- PROCESS DYNAMIC AND TRACE-RELATED DATA ---
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
                if (!firstStackTrace.isEmpty()) {
                    processFileForElement(firstStackTrace.get(0), uniqueFiles);
                    processFileForElement(firstStackTrace.get(firstStackTrace.size() - 1), uniqueFiles);
                }
                processStackTraceElements(stackTraces, uniqueFiles);
                vulnCustom.setFiles(new ArrayList<>(uniqueFiles.values()));
                vulnCustom.setFirstStackTrace(firstStackTrace);
                vulnCustom.setLastStackTraceElement(firstStackTrace.isEmpty() ? null : firstStackTrace.get(firstStackTrace.size() - 1));
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
        vulnFinalizer.finalize(vulnCustom);

        return vulnCustom;
    }

    private void processFileForElement(StackTraceElement element, Map<String, File> uniqueFiles) {
        if (element == null) return;

        String filename = element.getFilename();
        if (!StringUtil.isEmpty(filename) && sourceFileMap.containsKey(filename) && !uniqueFiles.containsKey(filename)) {
            String sourceFilePath = sourceFileMap.get(filename); // This value is "src-archive/0"

            // Resolve it directly against the root extraction path.
            Path actualSourcePath = extractedPath.resolve(sourceFilePath);

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
                    logger.warn("Source file not found: {}", actualSourcePath);
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
                for (StackTraceElement innerElement : element.getInnerStackTrace()) {
                    processFileForElement(innerElement, uniqueFiles);
                }
            }
        }
    }

    private List<StackTraceElement> findLongestList(List<List<StackTraceElement>> listOfLists) {
        return listOfLists.stream()
                .max((l1, l2) -> Integer.compare(l1.size(), l2.size()))
                .orElse(new ArrayList<>());
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
                // Recurse inner
                for (StackTraceElement inner : ste.getInnerStackTrace()) {
                    if (inner.getTaintflags() != null && !inner.getTaintflags().isEmpty()) {
                        allTaintFlags.addAll(Arrays.stream(inner.getTaintflags().split(",")).map(String::trim).collect(Collectors.toSet()));
                    }
                    allKnowledge.putAll(inner.getKnowledge());
                }
            }
        }
        vulnCustom.setTaintFlags(new ArrayList<>(allTaintFlags));
        vulnCustom.setKnowledge(allKnowledge);
    }

    private void processRequestRelated(Vulnerability vulnCustom, List<Map<String, String>> auxData, List<Entry> externalEntries) {
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

    /**
     * Retrieves a system property with a default value.
     *
     * @param key          Property key
     * @param defaultValue Default value if property not set
     * @return Property value or default
     */
    private static int getProperty(String key, int defaultValue) {
        return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
    }
}