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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.model.*;

/**
 * Parses Node elements from FVDL XML.
 * Handles both UnifiedNodePool nodes and inline nodes within traces.
 */
public class NodeParser {
    private static final Logger logger = LoggerFactory.getLogger(NodeParser.class);
    private final TraceParser traceParser;

    public NodeParser(TraceParser traceParser) {
        this.traceParser = traceParser;
    }

    /**
     * Parse a Node element and create a Node object.
     *
     * @param reader XMLStreamReader positioned at Node start element
     * @param passedNodeId Optional node ID (can be null, will read from attributes if needed)
     * @return Parsed Node object, or null if parsing fails
     */
    public Node parseNode(XMLStreamReader reader, String passedNodeId) throws XMLStreamException {
        String nodeId = passedNodeId;
        if (nodeId == null) {
            nodeId = reader.getAttributeValue(null, "id");
        }

        if (nodeId == null) {
            nodeId = "inline_" + System.nanoTime();
            logger.debug("Node has no ID attribute - generating temporary ID: {}", nodeId);
        }

        logger.debug("Start parseNode for nodeId: {}", nodeId);

        String filePath = "";
        int line = 0;
        int lineEnd = 0;
        int colStart = 0;
        int colEnd = 0;
        String contextId = "";
        String snippet = "";
        String actionType = "";
        String additionalInfo = "";
        String ruleId = "";
        String label = reader.getAttributeValue(null, "label");
        if (label == null) label = "";

        String detailsOnlyStr = reader.getAttributeValue(null, "detailsOnly");
        boolean detailsOnly = "true".equalsIgnoreCase(detailsOnlyStr);

        String secondaryPath = "";
        int secondaryLine = 0;
        int secondaryLineEnd = 0;
        int secondaryColStart = 0;
        int secondaryColEnd = 0;
        String secondaryContextId = "";

        List<String> taintFlags = new ArrayList<>();
        Map<String, String> knowledge = new HashMap<>();
        StringBuilder reasonTextBuilder = new StringBuilder();

        List<String> reasonTraceRefs = new ArrayList<>();
        List<StreamedTrace> reasonInlineTraces = new ArrayList<>();

        logger.debug("Node {} - Initial attributes: label={}, detailsOnly={}", nodeId, label, detailsOnly);

        while (reader.hasNext()) {
            int event = reader.next();

            if((event == XMLStreamConstants.START_ELEMENT || event == XMLStreamConstants.END_ELEMENT))
                logger.debug("[parseNode {}] Event: {}, LocalName: {}", nodeId, getEventTypeName(event), reader.getLocalName());

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("SourceLocation".equals(localName)) {
                    filePath = getAttributeOrDefault(reader, "path", "");
                    snippet = "";
                    line = parseIntOrDefault(reader, "line", 0);
                    lineEnd = parseIntOrDefault(reader, "lineEnd", line);
                    colStart = parseIntOrDefault(reader, "colStart", 0);
                    colEnd = parseIntOrDefault(reader, "colEnd", 0);
                    contextId = getAttributeOrDefault(reader, "contextId", "");
                    logger.debug("Node {} - Parsed SourceLocation: path={}, line={}", nodeId, filePath, line);

                } else if ("SecondaryLocation".equals(localName)) {
                    secondaryPath = getAttributeOrDefault(reader, "path", "");
                    secondaryLine = parseIntOrDefault(reader, "line", 0);
                    secondaryLineEnd = parseIntOrDefault(reader, "lineEnd", secondaryLine);
                    secondaryColStart = parseIntOrDefault(reader, "colStart", 0);
                    secondaryColEnd = parseIntOrDefault(reader, "colEnd", 0);
                    secondaryContextId = getAttributeOrDefault(reader, "contextId", "");
                    logger.debug("Node {} - Parsed SecondaryLocation: path={}, line={}", nodeId, secondaryPath, secondaryLine);

                } else if ("Action".equals(localName)) {
                    actionType = getAttributeOrDefault(reader, "type", "");
                    additionalInfo = readElementText(reader);
                    logger.debug("Node {} - Parsed Action: type={}", nodeId, actionType);
                    continue;

                } else if ("Fact".equals(localName)) {
                    String factType = reader.getAttributeValue(null, "type");
                    String factValue = readElementText(reader);
                    if (factValue != null) {
                        if ("TaintFlags".equalsIgnoreCase(factType)) {
                            taintFlags.add(factValue);
                        } else {
                            knowledge.put(factType, factValue);
                        }
                    }
                    continue;

                } else if ("Rule".equals(localName)) {
                    String ruleID = reader.getAttributeValue(null, "ruleID");
                    if (ruleID != null && ruleId.isEmpty()) {
                        ruleId = ruleID;
                    }
                    if (ruleID != null) {
                        reasonTextBuilder.append("Rule: ").append(ruleID).append("\n");
                    }

                } else if ("Reason".equals(localName)) {
                    logger.debug("Node {} - Parsing Reason element", nodeId);
                    ruleId = parseReasonElement(reader, reasonTraceRefs, reasonInlineTraces,
                                                reasonTextBuilder, ruleId, nodeId);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("Node".equals(reader.getLocalName())) {
                    logger.debug("Node {} - Parsing complete", nodeId);
                    break;
                }
            }
        }

        return new Node(
            nodeId, filePath, line, lineEnd, colStart, colEnd, contextId, snippet,
            actionType, additionalInfo, ruleId, taintFlags, knowledge,
            secondaryPath, secondaryLine, secondaryLineEnd, secondaryColStart,
            secondaryColEnd, secondaryContextId, detailsOnly, label,
            reasonTextBuilder.toString().trim(), reasonTraceRefs, reasonInlineTraces
        );
    }

    /**
     * Parse Reason element for innerStackTrace support.
     */
    private String parseReasonElement(
            XMLStreamReader reader,
            List<String> reasonTraceRefs,
            List<StreamedTrace> reasonInlineTraces,
            StringBuilder reasonTextBuilder,
            String currentRuleId,
            String parentNodeId) throws XMLStreamException {

        logger.debug("[parseReasonElement] Start parsing Reason for node: {}", parentNodeId);
        String ruleId = currentRuleId;
        int depth = 1;

        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("Trace".equals(localName)) {
                    String traceId = reader.getAttributeValue(null, "id");
                    StreamedTrace inlineTrace = traceParser.parseStreamedTrace(reader, traceId);
                    if (inlineTrace != null) {
                        reasonInlineTraces.add(inlineTrace);
                    }
                    continue;

                } else if ("TraceRef".equals(localName)) {
                    String refId = reader.getAttributeValue(null, "id");
                    if (refId != null && !refId.isEmpty()) {
                        reasonTraceRefs.add(refId);
                    }

                } else if ("Rule".equals(localName)) {
                    String ruleID = reader.getAttributeValue(null, "ruleID");
                    if (ruleID != null) {
                        if (ruleId == null || ruleId.isEmpty()) {
                            ruleId = ruleID;
                        }
                        reasonTextBuilder.append("Rule: ").append(ruleID).append("\n");
                    }
                }
                depth++;

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if ("Reason".equals(reader.getLocalName()) && depth == 0) {
                    return ruleId;
                }
            }
        }
        return ruleId;
    }

    /**
     * Create a placeholder Node for forward references.
     */
    public Node createPlaceholderNode(String nodeId) {
        return new Node(
            nodeId, "", 0, 0, 0, 0, "", "", "", "", "",
            new ArrayList<>(), new HashMap<>(), "", 0, 0, 0, 0, "",
            false, "", "", new ArrayList<>(), new ArrayList<>()
        );
    }

    private String getAttributeOrDefault(XMLStreamReader reader, String attrName, String defaultValue) {
        String value = reader.getAttributeValue(null, attrName);
        return value != null ? value : defaultValue;
    }

    private int parseIntOrDefault(XMLStreamReader reader, String attrName, int defaultValue) {
        String value = reader.getAttributeValue(null, attrName);
        Integer parsed = parseIntSafe(value);
        return parsed != null ? parsed : defaultValue;
    }
}
