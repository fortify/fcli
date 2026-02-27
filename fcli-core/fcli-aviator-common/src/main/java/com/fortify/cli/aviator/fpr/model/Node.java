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
package com.fortify.cli.aviator.fpr.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.utils.Searchable;
import com.fortify.cli.aviator.util.StringUtil;

import lombok.Getter;
import lombok.Setter;


/**
 * Represents a node in the FVDL UnifiedNodePool or trace entry.
 * Enhanced with taintFlags, knowledge, secondary location, detailsOnly, label, and reasonText for full coverage.
 * Implements Searchable for description conditionals.
 *
 * Note: Uses manual constructors instead of @AllArgsConstructor for constructor overloading
 * to support both DOM parser (22 params) and Streaming parser (24 params) use cases.
 */
@Getter
@Setter
public class Node implements Searchable {
    // Existing fields
    private String id;
    private String filePath;
    private int line;
    private int lineEnd;
    private int colStart;
    private int colEnd;
    private String contextId;
    private String snippet;
    private String actionType;
    private String additionalInfo;
    private String associatedRuleId;

    // Existing enhanced fields
    private List<String> taintFlags = new ArrayList<>(); // From Knowledge/Fact type="TaintFlags"
    private Map<String, String> knowledge = new HashMap<>(); // From Knowledge/Fact (type -> value)

    // New fields
    private String secondaryPath = ""; // From SecondaryLocation
    private int secondaryLine = 0;
    private int secondaryLineEnd = 0;
    private int secondaryColStart = 0;
    private int secondaryColEnd = 0;
    private String secondaryContextId = "";
    private boolean detailsOnly = false; // From @detailsOnly
    private String label = ""; // From @label
    private String reasonText = ""; // From Reason/Internal or concatenated reason info

    // InnerStackTrace support fields (Option B - Simplified approach)
    // Used by Streaming parser to store Reason trace data for building innerStackTrace
    // DOM parser doesn't use these (uses rawNodePool with JAXB objects instead)
    private List<String> reasonTraceRefs = new ArrayList<>(); // TraceRef IDs from Reason element
    private List<StreamedTrace> reasonInlineTraces = new ArrayList<>(); // Inline Traces from Reason element

    /**
     * Checks if any searchable field contains the given string.
     *
     * @param searchString String to search for
     * @return true if found in searchable fields
     */
    @Override
    public boolean contains(String searchString) {
        if (searchString == null || searchString.isEmpty()) {
            return false;
        }
        String lowerSearch = searchString.toLowerCase();
        return (filePath != null && filePath.toLowerCase().contains(lowerSearch))
            || (secondaryPath != null && secondaryPath.toLowerCase().contains(lowerSearch))
            || (actionType != null && actionType.toLowerCase().contains(lowerSearch))
            || (additionalInfo != null && additionalInfo.toLowerCase().contains(lowerSearch))
            || (associatedRuleId != null && associatedRuleId.toLowerCase().contains(lowerSearch))
            || (reasonText != null && reasonText.toLowerCase().contains(lowerSearch))
            || (label != null && label.toLowerCase().contains(lowerSearch))
            || taintFlags.stream().anyMatch(flag -> flag.toLowerCase().contains(lowerSearch))
            || knowledge.values().stream().anyMatch(value -> value != null && value.toLowerCase().contains(lowerSearch));
    }

    /**
     * Checks if any searchable field exactly matches the given string.
     *
     * @param matchString String to match exactly
     * @return true if exact match in searchable fields
     */
    @Override
    public boolean matches(String matchString) {
        if (matchString == null || matchString.isEmpty()) {
            return false;
        }
        String lowerMatch = matchString.toLowerCase();
        return (filePath != null && filePath.toLowerCase().equals(lowerMatch))
            || (secondaryPath != null && secondaryPath.toLowerCase().equals(lowerMatch))
            || (actionType != null && actionType.toLowerCase().equals(lowerMatch))
            || (additionalInfo != null && additionalInfo.toLowerCase().equals(lowerMatch))
            || (associatedRuleId != null && associatedRuleId.toLowerCase().equals(lowerMatch))
            || (reasonText != null && reasonText.toLowerCase().equals(lowerMatch))
            || (label != null && label.toLowerCase().equals(lowerMatch))
            || taintFlags.stream().anyMatch(flag -> flag.toLowerCase().equals(lowerMatch))
            || knowledge.values().stream().anyMatch(value -> value != null && value.toLowerCase().equals(lowerMatch));
    }

    /**
     * Checks if any searchable field matches the given regex pattern.
     *
     * @param pattern Regex pattern to match
     * @return true if pattern matches searchable fields
     */
    @Override
    public boolean matchesPattern(Pattern pattern) {
        if (pattern == null) {
            return false;
        }
        return (filePath != null && pattern.matcher(filePath).matches())
            || (secondaryPath != null && pattern.matcher(secondaryPath).matches())
            || (actionType != null && pattern.matcher(actionType).matches())
            || (additionalInfo != null && pattern.matcher(additionalInfo).matches())
            || (associatedRuleId != null && pattern.matcher(associatedRuleId).matches())
            || (reasonText != null && pattern.matcher(reasonText).matches())
            || (label != null && pattern.matcher(label).matches())
            || taintFlags.stream().anyMatch(flag -> pattern.matcher(flag).matches())
            || knowledge.values().stream().anyMatch(value -> value != null && pattern.matcher(value).matches());
    }

    // ========================================
    // Constructors (Manual - Lombok @AllArgsConstructor removed for overloading)
    // ========================================

    /**
     * Default constructor.
     * Initializes collection fields to empty lists/maps.
     */
    public Node() {
        this.taintFlags = new ArrayList<>();
        this.knowledge = new HashMap<>();
        this.reasonTraceRefs = new ArrayList<>();
        this.reasonInlineTraces = new ArrayList<>();
    }

    /**
     * Constructor for DOM parser - maintains backward compatibility.
     * 22 parameters (original signature).
     *
     * Reason trace data is not needed as DOM parser uses rawNodePool (JAXB objects)
     * to access Reason elements for building innerStackTrace.
     *
     * @param id Node ID
     * @param filePath Source file path
     * @param line Start line number
     * @param lineEnd End line number
     * @param colStart Start column
     * @param colEnd End column
     * @param contextId Context identifier
     * @param snippet Code snippet
     * @param actionType Action type
     * @param additionalInfo Additional information
     * @param associatedRuleId Associated rule ID
     * @param taintFlags List of taint flags
     * @param knowledge Knowledge map
     * @param secondaryPath Secondary location path
     * @param secondaryLine Secondary line number
     * @param secondaryLineEnd Secondary end line
     * @param secondaryColStart Secondary start column
     * @param secondaryColEnd Secondary end column
     * @param secondaryContextId Secondary context ID
     * @param detailsOnly Details only flag
     * @param label Node label
     * @param reasonText Reason text summary
     */
    public Node(
        String id, String filePath, int line, int lineEnd, int colStart, int colEnd,
        String contextId, String snippet, String actionType, String additionalInfo,
        String associatedRuleId, List<String> taintFlags, Map<String, String> knowledge,
        String secondaryPath, int secondaryLine, int secondaryLineEnd,
        int secondaryColStart, int secondaryColEnd, String secondaryContextId,
        boolean detailsOnly, String label, String reasonText
    ) {
        // Call extended constructor with empty lists for Reason trace data
        this(id, filePath, line, lineEnd, colStart, colEnd, contextId, snippet,
            actionType, additionalInfo, associatedRuleId, taintFlags, knowledge,
            secondaryPath, secondaryLine, secondaryLineEnd, secondaryColStart,
            secondaryColEnd, secondaryContextId, detailsOnly, label, reasonText,
            new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Extended constructor for Streaming parser - includes Reason trace data.
     * 24 parameters (includes reasonTraceRefs and reasonInlineTraces).
     *
     * Enables innerStackTrace building from parsed Reason elements.
     * Streaming parser must store Reason trace data in Node fields since it doesn't
     * have JAXB objects like DOM parser.
     *
     * @param id Node ID
     * @param filePath Source file path
     * @param line Start line number
     * @param lineEnd End line number
     * @param colStart Start column
     * @param colEnd End column
     * @param contextId Context identifier
     * @param snippet Code snippet
     * @param actionType Action type
     * @param additionalInfo Additional information
     * @param associatedRuleId Associated rule ID
     * @param taintFlags List of taint flags
     * @param knowledge Knowledge map
     * @param secondaryPath Secondary location path
     * @param secondaryLine Secondary line number
     * @param secondaryLineEnd Secondary end line
     * @param secondaryColStart Secondary start column
     * @param secondaryColEnd Secondary end column
     * @param secondaryContextId Secondary context ID
     * @param detailsOnly Details only flag
     * @param label Node label
     * @param reasonText Reason text summary
     * @param reasonTraceRefs List of TraceRef IDs from Reason element
     * @param reasonInlineTraces List of inline Traces from Reason element
     */
    public Node(
        String id, String filePath, int line, int lineEnd, int colStart, int colEnd,
        String contextId, String snippet, String actionType, String additionalInfo,
        String associatedRuleId, List<String> taintFlags, Map<String, String> knowledge,
        String secondaryPath, int secondaryLine, int secondaryLineEnd,
        int secondaryColStart, int secondaryColEnd, String secondaryContextId,
        boolean detailsOnly, String label, String reasonText,
        List<String> reasonTraceRefs, List<StreamedTrace> reasonInlineTraces
    ) {
        this.id = id;
        this.filePath = filePath;
        this.line = line;
        this.lineEnd = lineEnd;
        this.colStart = colStart;
        this.colEnd = colEnd;
        this.contextId = contextId;
        this.snippet = snippet;
        this.actionType = actionType;
        this.additionalInfo = additionalInfo;
        this.associatedRuleId = associatedRuleId;
        this.taintFlags = taintFlags != null ? taintFlags : new ArrayList<>();
        this.knowledge = knowledge != null ? knowledge : new HashMap<>();
        this.secondaryPath = secondaryPath;
        this.secondaryLine = secondaryLine;
        this.secondaryLineEnd = secondaryLineEnd;
        this.secondaryColStart = secondaryColStart;
        this.secondaryColEnd = secondaryColEnd;
        this.secondaryContextId = secondaryContextId;
        this.detailsOnly = detailsOnly;
        this.label = label;
        this.reasonText = reasonText;
        this.reasonTraceRefs = reasonTraceRefs != null ? reasonTraceRefs : new ArrayList<>();
        this.reasonInlineTraces = reasonInlineTraces != null ? reasonInlineTraces : new ArrayList<>();
    }

    // ========================================
    // Inner Classes
    // ========================================

    @Getter
    @Setter
    public static class FPRInfo {
        private String uuid;
        private String buildId;
        private String FPRName;
        private String sourceBasePath;
        private int numberOfFiles;
        private int scanTime;
        private FilterTemplate filterTemplate;
        private FilterSet defaultEnabledFilterSet;

        public FPRInfo(Path extractedPath, Path FPRPath) {
            FPRName = String.valueOf(FPRPath.getFileName());
            try {
                // Use streaming parser for better memory efficiency
                extractInfoFromAuditFvdlStreaming(extractedPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Extract FPR metadata from audit.fvdl using DOM parsing (original method).
         * Kept for backward compatibility.
         *
         * @param extractedPath Path to extracted FPR directory
         * @throws Exception if parsing fails
         */
        private void extractInfoFromAuditFvdl(Path extractedPath) throws Exception {
            Path auditPath = extractedPath.resolve("audit.fvdl");

            if (!Files.exists(auditPath)) {
                throw new IllegalStateException("audit.fvdl not found in " + extractedPath);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document auditDoc = builder.parse(auditPath.toFile());

            // Extract UUID
            NodeList uuidNodes = auditDoc.getElementsByTagName("UUID");
            if (uuidNodes.getLength() > 0) {
                this.uuid = uuidNodes.item(0).getTextContent();
            }

            // Extract Build information
            NodeList buildNodes = auditDoc.getElementsByTagName("Build");
            if (buildNodes.getLength() > 0) {
                Element buildElement = (Element) buildNodes.item(0);
                this.buildId = getFirstElementContent(buildElement, "BuildID", "");
                this.sourceBasePath = getFirstElementContent(buildElement, "SourceBasePath", "");
                this.numberOfFiles = parseIntegerContent(getFirstElementContent(buildElement, "NumberFiles", "0"));
                this.scanTime = parseIntegerContent(getFirstElementContent(buildElement, "ScanTime", "0"));
            }
        }

        /**
         * Extract FPR metadata from audit.fvdl using streaming XML parsing (StAX).
         * More memory-efficient than DOM parsing for large files.
         *
         * Extracts:
         * - UUID
         * - Build information (BuildID, SourceBasePath, NumberFiles, ScanTime)
         *
         * @param extractedPath Path to extracted FPR directory
         * @throws Exception if parsing fails
         */
        private void extractInfoFromAuditFvdlStreaming(Path extractedPath) throws Exception {
            Path auditPath = extractedPath.resolve("audit.fvdl");

            if (!Files.exists(auditPath)) {
                throw new IllegalStateException("audit.fvdl not found in " + extractedPath);
            }

            javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
            // Security: Disable external entity processing
            factory.setProperty(javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(javax.xml.stream.XMLInputFactory.SUPPORT_DTD, false);

            try (java.io.InputStream inputStream = Files.newInputStream(auditPath)) {
                javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

                boolean inBuild = false;
                String currentElement = null;

                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                        String localName = reader.getLocalName();

                        if ("UUID".equals(localName)) {
                            // Extract UUID text content
                            this.uuid = readElementText(reader);

                        } else if ("Build".equals(localName)) {
                            // Entering Build section
                            inBuild = true;

                        } else if (inBuild) {
                            // Inside Build section, capture element name
                            currentElement = localName;
                        }

                    } else if (event == javax.xml.stream.XMLStreamConstants.CHARACTERS && inBuild && currentElement != null) {
                        // Read text content for Build child elements
                        String text = reader.getText().trim();
                        if (!text.isEmpty()) {
                            switch (currentElement) {
                                case "BuildID":
                                    this.buildId = text;
                                    break;
                                case "SourceBasePath":
                                    this.sourceBasePath = text;
                                    break;
                                case "NumberFiles":
                                    this.numberOfFiles = parseIntegerContent(text);
                                    break;
                                case "ScanTime":
                                    this.scanTime = parseIntegerContent(text);
                                    break;
                            }
                        }

                    } else if (event == javax.xml.stream.XMLStreamConstants.END_ELEMENT) {
                        String localName = reader.getLocalName();

                        if ("Build".equals(localName)) {
                            // Exiting Build section, we have all needed data
                            inBuild = false;
                            // Early exit: we've extracted all needed metadata
                            if (this.uuid != null) {
                                break; // Stop parsing, we have everything
                            }

                        } else if (inBuild) {
                            // Clear current element when exiting child element
                            currentElement = null;
                        }
                    }
                }

                reader.close();

            } catch (javax.xml.stream.XMLStreamException e) {
                throw new Exception("Failed to parse audit.fvdl using streaming parser", e);
            }
        }

        /**
         * Helper method to read element text content using StAX reader.
         * Advances reader to the text content and returns it.
         *
         * @param reader XMLStreamReader positioned at START_ELEMENT
         * @return Text content of the element, or empty string if no text
         * @throws javax.xml.stream.XMLStreamException if reading fails
         */
        private String readElementText(javax.xml.stream.XMLStreamReader reader) throws javax.xml.stream.XMLStreamException {
            StringBuilder text = new StringBuilder();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == javax.xml.stream.XMLStreamConstants.CHARACTERS) {
                    text.append(reader.getText());
                } else if (event == javax.xml.stream.XMLStreamConstants.END_ELEMENT) {
                    break;
                }
            }
            return text.toString().trim();
        }

        private String getFirstElementContent(Element parent, String tagName, String defaultValue) {
            NodeList nodes = parent.getElementsByTagName(tagName);
            if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
                return nodes.item(0).getTextContent();
            }
            return defaultValue;
        }

        private int parseIntegerContent(String content) {

            if (StringUtil.isEmpty(content)) {
                return 0;
            }

            try {
                return Integer.parseInt(content);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing integer: " + content);
                return 0;
            }
        }

        public Optional<FilterSet> getDefaultEnabledFilterSet() {
            if (filterTemplate == null || filterTemplate.getFilterSets() == null) {
                return Optional.empty();
            }

            return filterTemplate.getFilterSets().stream()
                .filter(FilterSet::isEnabled)
                .findFirst();
        }
    }
}
