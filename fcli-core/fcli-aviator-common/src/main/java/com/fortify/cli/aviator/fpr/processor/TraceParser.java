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

import static com.fortify.cli.aviator.fpr.processor.XmlParserUtils.getEventTypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.model.*;

/**
 * Parses Trace elements from FVDL XML.
 * Handles both UnifiedTracePool traces and inline traces within vulnerabilities.
 */
public class TraceParser {
    private static final Logger logger = LoggerFactory.getLogger(TraceParser.class);
    private NodeParser nodeParser;
    private Map<String, Node> nodePool;

    public TraceParser(Map<String, Node> nodePool) {
        this.nodePool = nodePool;
    }

    public void setNodeParser(NodeParser nodeParser) {
        this.nodeParser = nodeParser;
    }

    /**
     * Parse trace for UnifiedTracePool - returns StreamedTrace.
     */
    public StreamedTrace parseStreamedTrace(XMLStreamReader reader, String traceId) throws XMLStreamException {
        logger.debug("start parseTrace for traceId: {}", traceId);

        StreamedVulnerability.Trace trace = parseTrace(reader);

        if (trace == null || trace.getNodes() == null) {
            logger.warn("parseTrace returned null or empty trace for traceId: {}", traceId);
            return null;
        }

        List<StreamedTrace.Primary.Entry> entries = new ArrayList<>();

        for (Node node : trace.getNodes()) {
            StreamedTrace.Primary.Entry entry =
                StreamedTrace.Primary.Entry.builder()
                    .nodeId(node.getId())
                    .node(node)
                    .isDefault(node.isDetailsOnly())
                    .build();
            entries.add(entry);
        }

        StreamedTrace.Primary primary =
            StreamedTrace.Primary.builder()
                .entries(entries)
                .build();

        logger.debug("Completed parseTrace for traceId: {}, total entries: {}", traceId, entries.size());
        return StreamedTrace.builder()
            .id(traceId)
            .primary(primary)
            .build();
    }

    /**
     * Parse inline trace from Vulnerability.
     */
    public StreamedVulnerability.Trace parseTrace(XMLStreamReader reader) throws XMLStreamException {
        logger.debug("=== START parseTrace (vulnerability inline trace) ===");

        List<Node> nodes = new ArrayList<>();
        boolean inPrimary = false;

        while (reader.hasNext()) {
            int event = reader.next();

            if((event == XMLStreamConstants.START_ELEMENT || event == XMLStreamConstants.END_ELEMENT))
                logger.debug("[parseTrace] Event: {}, LocalName: {}", getEventTypeName(event), reader.getLocalName());

            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();

                if ("Primary".equals(elementName)) {
                    inPrimary = true;
                    logger.debug("[parseTrace] Entered Primary section");

                } else if (inPrimary && "Entry".equals(elementName)) {
                    logger.debug("[parseTrace] Parsing Entry element");
                    parseTraceEntry(reader, nodes);
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String elementName = reader.getLocalName();

                if ("Primary".equals(elementName)) {
                    inPrimary = false;

                } else if ("Trace".equals(elementName)) {
                    logger.debug("=== END parseTrace - Parsed {} nodes ===", nodes.size());
                    return StreamedVulnerability.Trace.builder()
                        .nodes(nodes)
                        .build();
                }
            }
        }

        logger.warn("=== END parseTrace (unexpected exit) - Parsed {} nodes ===", nodes.size());
        return StreamedVulnerability.Trace.builder()
            .nodes(nodes)
            .build();
    }

    /**
     * Parse a single Entry element and add Node to the list.
     */
    private void parseTraceEntry(XMLStreamReader reader, List<Node> nodes) throws XMLStreamException {
        logger.debug("[parseTraceEntry] Start parsing Entry");

        String nodeRefId = null;
        Node inlineNode = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if((event == XMLStreamConstants.START_ELEMENT || event == XMLStreamConstants.END_ELEMENT))
                logger.debug("[parseTraceEntry] Event: {}, LocalName: {}", getEventTypeName(event), reader.getLocalName());

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("NodeRef".equals(localName)) {
                    nodeRefId = reader.getAttributeValue(null, "id");
                    logger.debug("[parseTraceEntry] Found NodeRef with id: {}", nodeRefId);

                } else if ("Node".equals(localName)) {
                    String nodeId = reader.getAttributeValue(null, "id");
                    logger.debug("[parseTraceEntry] Found inline Node with id: {}", nodeId);
                    inlineNode = nodeParser.parseNode(reader, nodeId);
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("Entry".equals(reader.getLocalName())) {
                    break;
                }
            }
        }

        if (nodeRefId != null) {
            Node node = nodePool.get(nodeRefId);
            if (node != null) {
                nodes.add(node);
                logger.debug("[parseTraceEntry] Added Node for nodeId {}. Total nodes: {}", nodeRefId, nodes.size());
            } else {
                logger.debug("[parseTraceEntry] NodeRef {} - Forward reference, creating placeholder", nodeRefId);
                Node placeholder = nodeParser.createPlaceholderNode(nodeRefId);
                nodes.add(placeholder);
            }
        } else if (inlineNode != null) {
            nodes.add(inlineNode);
            logger.debug("[parseTraceEntry] Added inline Node. Total nodes: {}", nodes.size());
        } else {
            logger.warn("[parseTraceEntry] Entry has neither NodeRef nor inline Node - skipping");
        }
    }
}
