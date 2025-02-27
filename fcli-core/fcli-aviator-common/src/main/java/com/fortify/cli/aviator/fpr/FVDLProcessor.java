package com.fortify.cli.aviator.fpr;

import com.fortify.cli.aviator.core.model.File;
import com.fortify.cli.aviator.core.model.Fragment;
import com.fortify.cli.aviator.util.StringUtil;
import com.fortify.cli.aviator.core.model.StackTraceElement;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FVDLProcessor {

    Logger logger = LoggerFactory.getLogger(FVDLProcessor.class);
    private static Path extractedPath = null;
    @Getter
    private final List<Vulnerability> vulnerabilities = new ArrayList<>();
    private static Map<String, String> sourceFileMap;
    private Map<String, Node> nodePool = new HashMap<>();
    private Set<String> processedFiles;
    private Document inputDoc;
    private final Map<Path, List<String>> fileContentCache = new ConcurrentHashMap<>();
    private final Map<String, Element> ruleInfoCache = new HashMap<>();
    private final Map<String, String[]> metaInfoCache = new HashMap<>();
    private Map<String, Element> descriptionCache;
    private final Map<String, String> tagReplacementMap;
    private Map<String, Element> nodeElementMap = new HashMap<>();

    private static final List<Charset> CHARSETS = Arrays.asList(
            StandardCharsets.UTF_8,
            StandardCharsets.ISO_8859_1
    );

    public FVDLProcessor(Path extractedPath) {
        this.extractedPath = extractedPath;
        this.descriptionCache = new HashMap<>();
        this.tagReplacementMap = initializeTagReplacementMap();
    }

    public void processXML() throws Exception {
        Path auditPath = extractedPath.resolve("audit.fvdl");

        if (!Files.exists(auditPath)) {
            throw new IllegalStateException("audit.fvdl not found in " + extractedPath);
        }

        loadSourceFileMap();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        inputDoc = builder.parse(auditPath.toFile());

        processedFiles = new HashSet<>();

        optimizedPopulateNodePool();

        NodeList vulnerabilityNodes = inputDoc.getElementsByTagName("Vulnerability");
        for (int i = 0; i < vulnerabilityNodes.getLength(); i++) {
            Element vulnerabilityElement = (Element) vulnerabilityNodes.item(i);
            Vulnerability vulnerability = processVulnerability(vulnerabilityElement);
            vulnerabilities.add(vulnerability);
        }

        nodeElementMap.clear();
    }

    private void optimizedPopulateNodePool() {
        Element nodePoolElement = (Element) inputDoc.getElementsByTagName("UnifiedNodePool").item(0);
        if (nodePoolElement == null) {
            logger.warn("No UnifiedNodePool found in the document");
            return;
        }

        NodeList nodes = nodePoolElement.getElementsByTagName("Node");
        logger.info("Processing {} nodes", nodes.getLength());

        for (int i = 0; i < nodes.getLength(); i++) {
            Element nodeElement = (Element) nodes.item(i);
            processNodeElement(nodeElement);
        }
    }

    private void processNodeElement(Element nodeElement) {
        String id = nodeElement.getAttribute("id");
        if (nodePool.containsKey(id)) {
            logger.debug("Skipping duplicate node ID: {}", id);
            return;
        }

        Element sourceLocation = getFirstChildElement(nodeElement, "SourceLocation");
        if (sourceLocation == null) {
            logger.debug("Skipping node {} - no SourceLocation found", id);
            return;
        }

        Element action = getFirstChildElement(nodeElement, "Action");
        if (action == null) {
            logger.debug("Skipping node {} - no Action found", id);
            return;
        }

        String filePath = sourceLocation.getAttribute("path");
        int line = parseInt(sourceLocation.getAttribute("line"), 0);
        int lineEnd = parseInt(sourceLocation.getAttribute("lineEnd"), 0);
        int colStart = parseInt(sourceLocation.getAttribute("colStart"), 0);
        int colEnd = parseInt(sourceLocation.getAttribute("colEnd"), 0);
        String contextId = sourceLocation.getAttribute("contextId");
        String snippet = sourceLocation.getAttribute("snippet");
        String actionType = action.getAttribute("type");
        String additionalInfo = action.getTextContent();
        String ruleId = extractRuleId(nodeElement);

        Node node = new Node(
                id, filePath, line, lineEnd, colStart, colEnd,
                contextId, snippet, actionType, additionalInfo, ruleId
        );

        nodePool.put(id, node);
        nodeElementMap.put(id, nodeElement);
    }

    private Element getFirstChildElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
    }

    private String extractRuleId(Element nodeElement) {
        Element reasonElement = getFirstChildElement(nodeElement, "Reason");
        if (reasonElement != null) {
            Element ruleElement = getFirstChildElement(reasonElement, "Rule");
            if (ruleElement != null) {
                return ruleElement.getAttribute("ruleID");
            }
        }
        return null;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse integer value: {}, using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    public Vulnerability processVulnerability(Element vulnerabilityElement) throws IOException {
        Vulnerability.VulnerabilityBuilder vulnerabilityBuilder = Vulnerability.builder();

        // Process ClassInfo
        Optional.ofNullable(vulnerabilityElement.getElementsByTagName("ClassInfo").item(0))
                .map(Element.class::cast)
                .ifPresent(classInfo -> {
                    String classID = getFirstElementContent(classInfo, "ClassID", "").orElse(null);
                    vulnerabilityBuilder.classID(classID);
                    vulnerabilityBuilder.analyzerName(getFirstElementContent(classInfo, "AnalyzerName", "Data Flow").orElse(null));
                    vulnerabilityBuilder.defaultSeverity(parseIntegerContent(getFirstElementContent(classInfo, "DefaultSeverity", "0").orElse("0")));
                    vulnerabilityBuilder.kingdom(getFirstElementContent(classInfo, "Kingdom", "").orElse(null));
                    String type = getFirstElementContent(classInfo, "Type", "").orElse(null);
                    vulnerabilityBuilder.type(type);
                    String subType = getFirstElementContent(classInfo, "Subtype", "").orElse(null);
                    vulnerabilityBuilder.subType(subType);
                });

        // Process InstanceInfo
        Optional.ofNullable(vulnerabilityElement.getElementsByTagName("InstanceInfo").item(0))
                .map(Element.class::cast)
                .ifPresent(instanceInfo -> {
                    Integer confidence = parseIntegerContent(getFirstElementContent(instanceInfo, "Confidence", "0").orElse("0"));
                    vulnerabilityBuilder.confidence(confidence);
                    vulnerabilityBuilder.instanceID(getFirstElementContent(instanceInfo, "InstanceID", "").orElse(null));
                    vulnerabilityBuilder.instanceSeverity(parseIntegerContent(getFirstElementContent(instanceInfo, "InstanceSeverity", "0").orElse("0")));
                });

        String classID = vulnerabilityBuilder.build().getClassID();
        String type = vulnerabilityBuilder.build().getType();
        String subType = vulnerabilityBuilder.build().getSubType();
        Integer confidence = vulnerabilityBuilder.build().getConfidence();

        // Process additional metadata
        String audience = processVulnerabilityAudience(vulnerabilityElement);
        String[] metaInfo = getMetaInfo(classID);
        Double accuracy = parseDoubleContent(metaInfo[0]);
        Double impact = parseDoubleContent(metaInfo[1]);
        Double probability = parseDoubleContent(metaInfo[2]);

        vulnerabilityBuilder.accuracy(accuracy);
        vulnerabilityBuilder.impact(impact);
        vulnerabilityBuilder.probability(probability);
        vulnerabilityBuilder.audience(audience);

        // Calculate and set vulnerability attributes
        String category = getCategory(type, subType);
        Double likelihood = getLikelihood(accuracy, confidence, probability);
        String priority = getFriority(impact, likelihood);
        vulnerabilityBuilder.likelihood(String.valueOf(likelihood));
        vulnerabilityBuilder.priority(priority);
        vulnerabilityBuilder.category(category);
        vulnerabilityBuilder.filetype("");

        // Process AnalysisInfo and stack traces
        Optional.ofNullable(vulnerabilityElement.getElementsByTagName("AnalysisInfo").item(0))
                .map(Element.class::cast)
                .ifPresent(analysisInfo -> {
                    try {
                        List<List<StackTraceElement>> stackTraces = processAnalysisInfo(analysisInfo);
                        vulnerabilityBuilder.stackTrace(stackTraces);

                        Map<String, File> uniqueFiles = new LinkedHashMap<>();
                        processStackTraceElements(stackTraces, uniqueFiles);

                        vulnerabilityBuilder.files(new ArrayList<>(uniqueFiles.values()));

                        if (!stackTraces.isEmpty()) {
                            List<StackTraceElement> firstStackTrace = stackTraces.get(0);
                            vulnerabilityBuilder.firstStackTrace(firstStackTrace);
                            vulnerabilityBuilder.lastStackTraceElement(firstStackTrace.isEmpty() ? null : firstStackTrace.get(firstStackTrace.size() - 1));
                            vulnerabilityBuilder.longestStackTrace(findLongestList(stackTraces));
                            vulnerabilityBuilder.source(firstStackTrace.isEmpty() ? null : firstStackTrace.get(0));
                            vulnerabilityBuilder.sink(firstStackTrace.isEmpty() ? null : firstStackTrace.get(firstStackTrace.size() - 1));
                        }
                    } catch (Exception e) {
                        logger.error("Error processing AnalysisInfo for vulnerability: {}", classID, e);
                        vulnerabilityBuilder.files(Collections.emptyList());
                    }
                });

        addDescriptionElements(vulnerabilityBuilder, classID, vulnerabilityElement);
        return vulnerabilityBuilder.build();
    }

    private void processStackTraceElements(List<List<StackTraceElement>> stackTraces, Map<String, File> uniqueFiles) throws IOException {
        for (List<StackTraceElement> stackTrace : stackTraces) {
            if (stackTrace == null) continue;

            for (StackTraceElement element : stackTrace) {
                processFileForElement(element, uniqueFiles);
                processInnerStackTraceElements(element, uniqueFiles);
            }
        }
    }

    private void processInnerStackTraceElements(StackTraceElement element, Map<String, File> uniqueFiles) throws IOException {
        if (element == null || element.getInnerStackTrace().isEmpty()) return;

        for (StackTraceElement innerElement : element.getInnerStackTrace()) {
            processFileForElement(innerElement, uniqueFiles);
            processInnerStackTraceElements(innerElement, uniqueFiles);
        }
    }

    private void processFileForElement(StackTraceElement element, Map<String, File> uniqueFiles) throws IOException {
        if (element == null) return;

        String filename = element.getFilename();
        if (!StringUtil.isEmpty(filename) && sourceFileMap.containsKey(filename) && !uniqueFiles.containsKey(filename)) {
            String sourceFilePath = sourceFileMap.get(filename);
            Path actualSourcePath = extractedPath.resolve(sourceFilePath);

            File.FileBuilder fileBuilder = File.builder().name(filename);
            fileBuilder.segment(false).startLine(1);

            try {
                if (Files.exists(actualSourcePath)) {
                    byte[] encodedBytes = Files.readAllBytes(actualSourcePath);
                    String content = new String(encodedBytes);
                    fileBuilder.content(content);
                    fileBuilder.endLine(countLines(actualSourcePath));
                } else {
                    logger.warn("Source file not found: {}", actualSourcePath);
                    fileBuilder.content("");
                    fileBuilder.endLine(0);
                }
            } catch (IOException e) {
                logger.warn("Error processing file: {}", filename, e);
                fileBuilder.content("");
                fileBuilder.endLine(0);
            }

            uniqueFiles.put(filename, fileBuilder.build());
        }
    }

    public Double getLikelihood(Double accuracy, Integer confidence, Double probability) {
        return Optional.ofNullable(accuracy).orElse(0.0) *
                Optional.ofNullable(confidence).map(Integer::doubleValue).orElse(0.0) *
                Optional.ofNullable(probability).orElse(0.0) / 25.0;
    }

    public String getFriority(Double impact, Double likelihood) {
        double impactValue = Optional.ofNullable(impact).orElse(0.0);
        double likelihoodValue = Optional.ofNullable(likelihood).orElse(0.0);

        if (impactValue >= 2.5) {
            return likelihoodValue >= 2.5 ? "Critical" : "High";
        } else {
            return likelihoodValue >= 2.5 ? "Medium" : "Low";
        }
    }

    private int countLines(Path filePath) throws IOException {
        try {
            return readFileWithFallback(filePath).size();
        } catch (IOException e) {
            logger.error("Error counting lines in file: {}", filePath, e);
            return 0;
        }
    }

    private List<List<StackTraceElement>> processAnalysisInfo(Element analysisInfoElement) throws IOException {
        if (analysisInfoElement == null) {
            logger.warn("AnalysisInfo element is null; returning empty stack traces");
            return new ArrayList<>();
        }

        List<List<StackTraceElement>> stackTraces = new ArrayList<>();
        NodeList traceNodes = analysisInfoElement.getElementsByTagName("Trace");

        for (int i = 0; i < traceNodes.getLength(); i++) {
            Element traceElement = (Element) traceNodes.item(i);
            if (traceElement == null) continue;

            NodeList entryNodes = traceElement.getElementsByTagName("Entry");
            List<StackTraceElement> stackTrace = new ArrayList<>();

            for (int j = 0; j < entryNodes.getLength(); j++) {
                Element entryElement = (Element) entryNodes.item(j);
                if (entryElement == null) continue;

                Element nodeRefElement = getFirstChildElement(entryElement, "NodeRef");
                Element nodeElement = getFirstChildElement(entryElement, "Node");

                if (nodeRefElement != null) {
                    String nodeId = nodeRefElement.getAttribute("id");
                    if (!StringUtil.isEmpty(nodeId)) {
                        Node node = nodePool.get(nodeId);
                        if (node != null) {
                            stackTrace.add(createStackTraceElement(node));
                        } else {
                            logger.warn("Node ID {} not found in nodePool", nodeId);
                        }
                    }
                } else if (nodeElement != null) {
                    stackTrace.add(createStackTraceElementFromNode(nodeElement));
                }
            }

            if (!stackTrace.isEmpty()) {
                stackTraces.add(stackTrace);
            }
        }

        return stackTraces;
    }

    private StackTraceElement createStackTraceElement(Node node) throws IOException {
        return createStackTraceElement(node, new HashSet<>());
    }

    private StackTraceElement createStackTraceElement(Node node, Set<String> visited) throws IOException {
        if (visited.contains(node.getId())) {
            logger.warn("Circular reference detected in node {}", node.getId());
            return StackTraceElement.builder()
                    .filename(node.getFilePath())
                    .line(node.getLine())
                    .code("")
                    .nodeType(node.getActionType())
                    .additionalInfo(node.getAdditionalInfo())
                    .build();
        }
        visited.add(node.getId());

        StackTraceElement.StackTraceElementBuilder builder = StackTraceElement.builder()
                .filename(node.getFilePath())
                .line(node.getLine())
                .code(getCodeSnippet(node.getSnippet(), node.getFilePath()).orElse(""))
                .nodeType(node.getActionType())
                .fragment(getFragmentFromFile(node.getFilePath(), node.getLine(), 5, 2))
                .additionalInfo(node.getAdditionalInfo());

        List<StackTraceElement> innerStackTrace = new ArrayList<>();
        Element nodeElement = nodeElementMap.get(node.getId());
        if (nodeElement != null) {
            Element reason = getFirstChildElement(nodeElement, "Reason");
            if (reason != null) {
                Element trace = getFirstChildElement(reason, "Trace");
                if (trace != null) {
                    NodeList entries = trace.getElementsByTagName("Entry");
                    for (int i = 0; i < entries.getLength(); i++) {
                        Element entry = (Element) entries.item(i);
                        NodeList nodeRefs = entry.getElementsByTagName("NodeRef");
                        for (int j = 0; j < nodeRefs.getLength(); j++) {
                            Element nodeRef = (Element) nodeRefs.item(j);
                            String refId = nodeRef.getAttribute("id");
                            if (!StringUtil.isEmpty(refId)) {
                                Node refNode = nodePool.get(refId);
                                if (refNode != null) {
                                    innerStackTrace.add(createStackTraceElement(refNode, new HashSet<>(visited)));
                                }
                            }
                        }
                    }
                }
            }
        }
        builder.innerStackTrace(innerStackTrace);

        return builder.build();
    }

    private StackTraceElement createStackTraceElementFromNode(Element nodeElement) throws IOException {
        return createStackTraceElementFromNode(nodeElement, new HashSet<>());
    }

    private StackTraceElement createStackTraceElementFromNode(Element nodeElement, Set<String> visited) throws IOException {
        Element sourceLocation = getFirstChildElement(nodeElement, "SourceLocation");
        Element action = getFirstChildElement(nodeElement, "Action");
        Element knowledge = getFirstChildElement(nodeElement, "Knowledge");
        String taintFlags = getTaintFlags(knowledge);

        String filePath = sourceLocation.getAttribute("path");
        int line = parseInt(sourceLocation.getAttribute("line"), 0);
        String codeSnippet = sourceLocation.getAttribute("snippet");
        String actionType = action != null ? action.getAttribute("type") : "GENERIC";
        String actionContent = action != null ? action.getTextContent() : "";

        String syntheticId = filePath + ":" + line;
        if (visited.contains(syntheticId)) {
            logger.warn("Circular reference detected in inline node {}", syntheticId);
            return StackTraceElement.builder()
                    .filename(filePath)
                    .line(line)
                    .code("")
                    .nodeType(actionType)
                    .additionalInfo(actionContent)
                    .taintflags(taintFlags)
                    .build();
        }
        visited.add(syntheticId);

        StackTraceElement.StackTraceElementBuilder builder = StackTraceElement.builder()
                .filename(filePath)
                .line(line)
                .code(getCodeSnippet(codeSnippet, filePath).orElse(""))
                .nodeType(actionType)
                .fragment(getFragmentFromFile(filePath, line, 5, 2))
                .additionalInfo(actionContent)
                .taintflags(taintFlags);

        List<StackTraceElement> innerStackTrace = new ArrayList<>();
        Element reason = getFirstChildElement(nodeElement, "Reason");
        if (reason != null) {
            Element trace = getFirstChildElement(reason, "Trace");
            if (trace != null) {
                NodeList entries = trace.getElementsByTagName("Entry");
                for (int i = 0; i < entries.getLength(); i++) {
                    Element entry = (Element) entries.item(i);
                    NodeList nodeRefs = entry.getElementsByTagName("NodeRef");
                    for (int j = 0; j < nodeRefs.getLength(); j++) {
                        Element nodeRef = (Element) nodeRefs.item(j);
                        String refId = nodeRef.getAttribute("id");
                        if (!StringUtil.isEmpty(refId)) {
                            Node refNode = nodePool.get(refId);
                            if (refNode != null) {
                                innerStackTrace.add(createStackTraceElement(refNode, new HashSet<>(visited)));
                            }
                        }
                    }
                }
            }
        }
        builder.innerStackTrace(innerStackTrace);

        return builder.build();
    }

    private String getTaintFlags(Element knowledgeElement) {
        if (knowledgeElement != null) {
            NodeList facts = knowledgeElement.getElementsByTagName("Fact");
            for (int i = 0; i < facts.getLength(); i++) {
                Element factNode = (Element) facts.item(i);
                if (factNode != null) {
                    Element fact = (Element) factNode;
                    if ("TaintFlags".equalsIgnoreCase(fact.getAttribute("type"))) {
                        return fact.getTextContent();
                    }
                }
            }
        }
        return "";
    }

    private Optional<String> getCodeSnippet(String snippetId, String filePath) {
        try {
            if (StringUtil.isEmpty(snippetId)) {
                return Optional.empty();
            }

            String[] parts = snippetId.split("#");
            if (parts.length != 2) {
                return Optional.empty();
            }

            String file = parts[1];
            Matcher matcher = Pattern.compile("(.+):(\\d+):(\\d+)").matcher(file);
            if (!matcher.find()) {
                return Optional.empty();
            }

            String filename = matcher.group(1);
            int startLine = Integer.parseInt(matcher.group(2));

            String sourceFilePath = sourceFileMap.get(filename);
            if (sourceFilePath == null) {
                return Optional.empty();
            }

            Path actualSourcePath = extractedPath.resolve(sourceFilePath);
            List<String> lines = readFileWithFallback(actualSourcePath);

            if (startLine > 0 && startLine <= lines.size()) {
                return Optional.of(lines.get(startLine - 1));
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error getting code snippet for snippet ID: {}, file path: {}", snippetId, filePath, e);
            return Optional.empty();
        }
    }

    private Fragment getFragmentFromFile(String filename, int linenumber, int maxBefore, int maxAfter) throws IOException {
        String sourceFilePath = sourceFileMap.get(filename);
        if (sourceFilePath == null) {
            return new Fragment("", 0, 0);
        }

        Path actualSourcePath = extractedPath.resolve(sourceFilePath);
        if (!Files.exists(actualSourcePath)) {
            logger.warn("Source file not found: {}", actualSourcePath);
            return new Fragment("", 0, 0);
        }

        try {
            List<String> lines = readFileWithFallback(actualSourcePath);
            String[] linesArray = lines.toArray(new String[0]);

            int first = Math.max(0, linenumber - maxBefore) + 1;
            int last = Math.min(linesArray.length, linenumber + maxAfter);
            StringBuilder sb = new StringBuilder();
            for (int i = first - 1; i < last; i++) {
                sb.append(linesArray[i]).append(System.lineSeparator());
            }

            return new Fragment(sb.toString(), first, last);
        } catch (IOException e) {
            logger.error("Error reading fragment from file: {}", actualSourcePath, e);
            return new Fragment("", 0, 0);
        }
    }

    public String processVulnerabilityAudience(Element vulnerabilityElement) {
        String mainRuleId = Optional.ofNullable(vulnerabilityElement.getElementsByTagName("ClassInfo").item(0))
                .map(Element.class::cast)
                .flatMap(classInfo -> getFirstElementContent(classInfo, "ClassID", ""))
                .orElse("");

        Set<String> allRuleIds = new HashSet<>();
        allRuleIds.add(mainRuleId);

        Optional.ofNullable(vulnerabilityElement.getElementsByTagName("AnalysisInfo").item(0))
                .map(Element.class::cast)
                .ifPresent(analysisInfo -> collectRuleIdsFromTraces(analysisInfo, allRuleIds));

        return intersectAudiences(allRuleIds);
    }

    private void collectRuleIdsFromTraces(Element analysisInfo, Set<String> ruleIds) {
        NodeList traceNodes = analysisInfo.getElementsByTagName("Trace");
        for (int i = 0; i < traceNodes.getLength(); i++) {
            Element traceElement = (Element) traceNodes.item(i);
            NodeList entryNodes = traceElement.getElementsByTagName("Entry");
            for (int j = 0; j < entryNodes.getLength(); j++) {
                Element entryElement = (Element) entryNodes.item(j);
                Element nodeRef = getFirstChildElement(entryElement, "NodeRef");
                if (nodeRef != null) {
                    String nodeId = nodeRef.getAttribute("id");
                    Node node = nodePool.get(nodeId);
                    if (node != null && node.getAssociatedRuleId() != null) {
                        ruleIds.add(node.getAssociatedRuleId());
                    }
                }
                Element nodeElement = getFirstChildElement(entryElement, "Node");
                if (nodeElement != null) {
                    Optional.ofNullable(getFirstChildElement(nodeElement, "Reason"))
                            .map(reason -> getFirstChildElement(reason, "Rule"))
                            .map(rule -> rule.getAttribute("ruleID"))
                            .ifPresent(ruleIds::add);
                }
            }
        }
    }

    private String intersectAudiences(Set<String> ruleIds) {
        Set<String> intersection = null;
        Element rulesInfo = (Element) inputDoc.getElementsByTagName("RuleInfo").item(0);
        if (rulesInfo == null) return "";

        for (String ruleId : ruleIds) {
            Set<String> currentAudience = getAudienceForRule(rulesInfo, ruleId);
            if (currentAudience == null || currentAudience.isEmpty()) {
                continue;
            }
            if (intersection == null) {
                intersection = currentAudience;
            } else {
                intersection.retainAll(currentAudience);
            }
            if (intersection.isEmpty()) {
                return "";
            }
        }
        return intersection == null ? "" : String.join(",", intersection);
    }

    private Set<String> getAudienceForRule(Element rulesInfo, String ruleId) {
        return Optional.ofNullable(rulesInfo)
                .map(ri -> {
                    NodeList rules = ri.getElementsByTagName("Rule");
                    for (int i = 0; i < rules.getLength(); i++) {
                        Element rule = (Element) rules.item(i);
                        if (ruleId.equals(rule.getAttribute("id"))) {
                            return Optional.ofNullable(getFirstChildElement(rule, "MetaInfo"))
                                    .map(metaInfo -> {
                                        NodeList groups = metaInfo.getElementsByTagName("Group");
                                        for (int j = 0; j < groups.getLength(); j++) {
                                            Element group = (Element) groups.item(j);
                                            if ("audience".equals(group.getAttribute("name"))) {
                                                return Arrays.stream(group.getTextContent().split(","))
                                                        .map(String::trim)
                                                        .collect(Collectors.toSet());
                                            }
                                        }
                                        return Collections.<String>emptySet();
                                    })
                                    .orElse(Collections.emptySet());
                        }
                    }
                    return Collections.<String>emptySet();
                })
                .orElse(Collections.emptySet());
    }

    private List<StackTraceElement> findLongestList(List<List<StackTraceElement>> listOfLists) {
        return listOfLists.stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(new ArrayList<>());
    }

    private String getCategory(String type, String subType) {
        return Optional.ofNullable(subType)
                .filter(s -> !s.isEmpty())
                .map(s -> type + ": " + s)
                .orElse(type);
    }

    private Optional<String> getFirstElementContent(Element parent, String tagName, String defaultValue) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
            return Optional.of(nodes.item(0).getTextContent());
        }
        return Optional.of(defaultValue);
    }

    private List<String> readFileWithFallback(Path filePath) throws IOException {
        return fileContentCache.computeIfAbsent(filePath, path -> {
            for (Charset charset : CHARSETS) {
                try {
                    return Files.readAllLines(path, charset);
                } catch (IOException e) {
                    logger.debug("Failed to read file with charset {}", charset);
                }
            }
            try {
                throw new IOException("Unable to read file: " + filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private int parseIntegerContent(String content) {
        try {
            return (int) Double.parseDouble(content);
        } catch (NumberFormatException e) {
            logger.error("Error parsing integer: {}", content, e);
            return 0;
        }
    }

    private Double parseDoubleContent(String content) {
        try {
            return Double.parseDouble(content);
        } catch (NumberFormatException e) {
            logger.error("Error parsing double: {}", content, e);
            return 0.0;
        }
    }

    private String[] getMetaInfo(String ruleId) {
        return metaInfoCache.computeIfAbsent(ruleId, id -> {
            String[] metaInfo = new String[]{"0", "0", "0", ""};
            Element ruleElement = getRuleInfo(id);
            if (ruleElement != null) {
                Element metaInfoElement = getFirstChildElement(ruleElement, "MetaInfo");
                if (metaInfoElement != null) {
                    NodeList groups = metaInfoElement.getElementsByTagName("Group");
                    for (int i = 0; i < groups.getLength(); i++) {
                        Element group = (Element) groups.item(i);
                        String groupName = group.getAttribute("name");
                        String content = group.getTextContent();
                        switch (groupName.toLowerCase()) {
                            case "accuracy": metaInfo[0] = content; break;
                            case "impact": metaInfo[1] = content; break;
                            case "probability": metaInfo[2] = content; break;
                            case "audience": metaInfo[3] = content; break;
                        }
                    }
                }
            }
            return metaInfo;
        });
    }

    private Element getRuleInfo(String ruleId) {
        return ruleInfoCache.computeIfAbsent(ruleId, id -> {
            NodeList ruleInfoNodes = inputDoc.getElementsByTagName("RuleInfo");
            for (int i = 0; i < ruleInfoNodes.getLength(); i++) {
                Element ruleInfoElement = (Element) ruleInfoNodes.item(i);
                NodeList rules = ruleInfoElement.getElementsByTagName("Rule");
                for (int j = 0; j < rules.getLength(); j++) {
                    Element rule = (Element) rules.item(j);
                    if (ruleId.equals(rule.getAttribute("id"))) {
                        return rule;
                    }
                }
            }
            return null;
        });
    }

    private Map<String, String> initializeTagReplacementMap() {
        Map<String, String> map = new HashMap<>();
        map.put("<pre>", "<code>");
        map.put("</pre>", "</code>");
        map.put("<p>", "");
        map.put("</p>", "\n");
        map.put("<table>", "");
        map.put("</table>", "");
        map.put("<tr>", "");
        map.put("</tr>", "");
        map.put("<td>", "\t");
        map.put("</td>", "");
        map.put("<th>", "<b>\t");
        map.put("</th>", "</b>");
        map.put("<li>", "-");
        map.put("</li>", "");
        map.put("<blockquote>", "");
        map.put("</blockquote>", "");
        map.put("<b>", "");
        map.put("</b>", "");
        map.put("<code>", "");
        map.put("</code>", "");
        map.put("<h1>", "");
        map.put("</h1>", "");
        map.put("<ul>", "");
        map.put("</ul>", "");
        map.put("<Content>", "");
        map.put("</Content>", "");
        map.put("<Paragraph>", "");
        map.put("</Paragraph>", "");
        return map;
    }

    private void addDescriptionElements(Vulnerability.VulnerabilityBuilder vulnerabilityBuilder, String classId, Element vulnerabilityElement) {
        Element descriptionElement = descriptionCache.computeIfAbsent(classId, this::findDescriptionElement);
        if (descriptionElement == null) {
            return;
        }

        ReplacementData replacementData = computeReplacementsForVulnerability(vulnerabilityElement);

        Element abstractElement = getFirstChildElement(descriptionElement, "Abstract");
        if (abstractElement != null) {
            String shortDescription = processText(abstractElement.getTextContent(), replacementData);
            vulnerabilityBuilder.shortDescription(shortDescription);
        }

        Element explanationElement = getFirstChildElement(descriptionElement, "Explanation");
        if (explanationElement != null) {
            String explanation = processText(explanationElement.getTextContent(), replacementData);
            if (explanation.contains("Example")) {
                String[] lines = explanation.split("\n");
                StringBuilder formatted = new StringBuilder();
                for (String line : lines) {
                    if (line.matches(".*<b>Example\\s+\\d+:</b>.*") || line.matches(".*Example\\s+\\d+:.*")) {
                        if (formatted.length() > 0) {
                            formatted.append("\n");
                        }
                        String exampleLine = line.replaceAll("(<b>)?Example\\s+(\\d+):(\\s*</b>)?", "**Example $2:**");
                        formatted.append(exampleLine).append("\n");
                    } else {
                        formatted.append(line).append("\n");
                    }
                }
                explanation = formatted.toString();
            }
            vulnerabilityBuilder.explanation(explanation.trim());
        }
    }

    private Element findDescriptionElement(String classId) {
        NodeList descriptions = inputDoc.getElementsByTagName("Description");
        for (int i = 0; i < descriptions.getLength(); i++) {
            Element desc = (Element) descriptions.item(i);
            if (classId.equals(desc.getAttribute("classID"))) {
                return desc;
            }
        }
        return null;
    }

    private static class ReplacementData {
        final Map<String, Replacement> replacements;
        final Map<String, Map<String, String>> locationReplacements;

        ReplacementData(Map<String, Replacement> replacements, Map<String, Map<String, String>> locationReplacements) {
            this.replacements = replacements;
            this.locationReplacements = locationReplacements;
        }
    }

    private record Replacement(String value, String path, String line, String colStart, String colEnd) {
        public boolean hasLocation() {
            return path != null && line != null && colStart != null && colEnd != null;
        }
    }

    private ReplacementData computeReplacementsForVulnerability(Element vulnerabilityElement) {
        Map<String, Replacement> replacements = new HashMap<>();
        Map<String, Map<String, String>> locationReplacements = new HashMap<>();

        Element analysisInfoElement = getFirstChildElement(vulnerabilityElement, "AnalysisInfo");
        if (analysisInfoElement != null) {
            Element replacementDefsElement = getFirstChildElement(analysisInfoElement, "ReplacementDefinitions");
            if (replacementDefsElement != null) {
                NodeList defNodes = replacementDefsElement.getElementsByTagName("Def");
                for (int i = 0; i < defNodes.getLength(); i++) {
                    Element def = (Element) defNodes.item(i);
                    String key = def.getAttribute("key");
                    String value = def.getAttribute("value");
                    Element sourceLocation = getFirstChildElement(def, "SourceLocation");
                    if (sourceLocation != null) {
                        String path = sourceLocation.getAttribute("path");
                        String line = sourceLocation.getAttribute("line");
                        String colStart = sourceLocation.getAttribute("colStart");
                        String colEnd = sourceLocation.getAttribute("colEnd");
                        replacements.put(key, new Replacement(value, path, line, colStart, colEnd));
                    } else {
                        replacements.put(key, new Replacement(value, null, null, null, null));
                    }
                }

                NodeList locationDefNodes = replacementDefsElement.getElementsByTagName("LocationDef");
                for (int i = 0; i < locationDefNodes.getLength(); i++) {
                    Element loc = (Element) locationDefNodes.item(i);
                    String key = loc.getAttribute("key");
                    Map<String, String> attrs = Map.of(
                            "path", loc.getAttribute("path"),
                            "line", loc.getAttribute("line"),
                            "colStart", loc.getAttribute("colStart"),
                            "colEnd", loc.getAttribute("colEnd")
                    );
                    locationReplacements.put(key, attrs);
                }
            }
        }
        return new ReplacementData(replacements, locationReplacements);
    }

    private String processText(String text, ReplacementData replacementData) {
        if (StringUtil.isEmpty(text)) {
            return text;
        }
        text = text.replaceAll("<AltParagraph>.*?</AltParagraph>", "");
        text = replacePlaceholders(text, replacementData);
        return stripTags(text);
    }

    private String stripTags(String text) {
        if (StringUtil.isEmpty(text)) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : tagReplacementMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result.replace(" ", " ");
    }

    private String replacePlaceholders(String text, ReplacementData replacementData) {
        if (!text.contains("<Replace")) {
            return text;
        }

        Pattern pattern = Pattern.compile("<Replace key=\"([^\"]+)\"(?: link=\"([^\"]+)\")?/>");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String link = matcher.group(2);
            Replacement replacement = replacementData.replacements.get(key);
            if (replacement != null) {
                String value = replacement.value();
                Map<String, String> locAttrs = null;
                if (link != null) {
                    locAttrs = replacementData.locationReplacements.get(link);
                } else if (replacement.hasLocation()) {
                    locAttrs = Map.of(
                            "path", replacement.path(),
                            "line", replacement.line(),
                            "colStart", replacement.colStart(),
                            "colEnd", replacement.colEnd()
                    );
                }
                if (locAttrs != null) {
                    String href = String.format("location://%s###%s###%s###%s",
                            locAttrs.get("path"),
                            locAttrs.get("line"),
                            locAttrs.get("colStart"),
                            locAttrs.get("colEnd"));
                    matcher.appendReplacement(sb, "<a href=\"" + href + "\">" + value + "</a>");
                } else {
                    matcher.appendReplacement(sb, value);
                }
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void loadSourceFileMap() throws Exception {
        sourceFileMap = new HashMap<>();
        Path indexPath = extractedPath.resolve("src-archive/index.xml");
        if (!Files.exists(indexPath)) {
            throw new IllegalStateException("index.xml not found in " + extractedPath);
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
}