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
     * Parse a pool or Reason trace. NodeRefs keep only their id, because the referenced node
     * can come later in the file; {@link StackTraceConverter} resolves them after pass 1.
     */
    public StreamedTrace parseStreamedTrace(XMLStreamReader reader, String traceId) throws XMLStreamException {
        StreamedTrace.Primary primary = StreamedTrace.Primary.builder()
            .entries(parseEntries(reader))
            .build();
        return StreamedTrace.builder()
            .id(traceId)
            .primary(primary)
            .build();
    }

    /**
     * Parse a vulnerability trace. Called in pass 2, so NodeRefs resolve from the complete node pool.
     */
    public StreamedVulnerability.Trace parseTrace(XMLStreamReader reader) throws XMLStreamException {
        List<StreamedTrace.Primary.Entry> entries = parseEntries(reader);
        List<Node> nodes = new ArrayList<>(entries.size());
        for (StreamedTrace.Primary.Entry entry : entries) {
            nodes.add(entry.hasNode() ? entry.getNode() : resolveNodeRef(entry.getNodeId()));
        }
        return StreamedVulnerability.Trace.builder()
            .nodes(nodes)
            .build();
    }

    private Node resolveNodeRef(String nodeRefId) {
        Node node = nodePool.get(nodeRefId);
        if (node == null) {
            logger.debug("NodeRef {} not found in NodePool, using placeholder", nodeRefId);
            return nodeParser.createPlaceholderNode(nodeRefId);
        }
        return node;
    }

    private List<StreamedTrace.Primary.Entry> parseEntries(XMLStreamReader reader) throws XMLStreamException {
        List<StreamedTrace.Primary.Entry> entries = new ArrayList<>();
        boolean inPrimary = false;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();

                if ("Primary".equals(elementName)) {
                    inPrimary = true;

                } else if (inPrimary && "Entry".equals(elementName)) {
                    StreamedTrace.Primary.Entry entry = parseTraceEntry(reader);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String elementName = reader.getLocalName();

                if ("Primary".equals(elementName)) {
                    inPrimary = false;

                } else if ("Trace".equals(elementName)) {
                    return entries;
                }
            }
        }

        logger.warn("Trace ended without a closing Trace element - parsed {} entries", entries.size());
        return entries;
    }

    /**
     * Parse a single Entry element into either a NodeRef id or an inline Node.
     */
    private StreamedTrace.Primary.Entry parseTraceEntry(XMLStreamReader reader) throws XMLStreamException {
        String nodeRefId = null;
        Node inlineNode = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("NodeRef".equals(localName)) {
                    nodeRefId = reader.getAttributeValue(null, "id");

                } else if ("Node".equals(localName)) {
                    String nodeId = reader.getAttributeValue(null, "id");
                    inlineNode = nodeParser.parseNode(reader, nodeId);
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("Entry".equals(reader.getLocalName())) {
                    break;
                }
            }
        }

        if (nodeRefId != null) {
            return StreamedTrace.Primary.Entry.builder().nodeId(nodeRefId).build();
        }
        if (inlineNode != null) {
            return StreamedTrace.Primary.Entry.builder()
                .node(inlineNode)
                .isDefault(inlineNode.isDetailsOnly())
                .build();
        }
        logger.warn("[parseTraceEntry] Entry has neither NodeRef nor inline Node - skipping");
        return null;
    }
}
