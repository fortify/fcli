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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.fpr.model.*;

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
        List<StreamedTrace.Primary.Entry> entries = new ArrayList<>();
        parseEntries(reader, (nodeRefId, inlineNode) -> entries.add(nodeRefId != null
            ? StreamedTrace.Primary.Entry.builder().nodeId(nodeRefId).build()
            : StreamedTrace.Primary.Entry.builder().node(inlineNode).isDefault(inlineNode.isDetailsOnly()).build()));
        StreamedTrace.Primary primary = StreamedTrace.Primary.builder()
            .entries(entries)
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
        List<Node> nodes = new ArrayList<>();
        parseEntries(reader, (nodeRefId, inlineNode) -> nodes.add(nodeRefId != null ? resolveNodeRef(nodeRefId) : inlineNode));
        return StreamedVulnerability.Trace.builder()
            .nodes(nodes)
            .build();
    }

    private Node resolveNodeRef(String nodeRefId) {
        Node node = nodePool.get(nodeRefId);
        if (node == null) {
            logger.debug("NodeRef {} not found in NodePool, using placeholder", nodeRefId);
            return NodeParser.createPlaceholderNode(nodeRefId);
        }
        return node;
    }

    /**
     * Calls {@code entryHandler} with (nodeRefId, null) or (null, inlineNode) for each Primary Entry.
     */
    private void parseEntries(XMLStreamReader reader, BiConsumer<String, Node> entryHandler) throws XMLStreamException {
        int entryCount = 0;
        boolean inPrimary = false;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();

                if ("Primary".equals(elementName)) {
                    inPrimary = true;

                } else if (inPrimary && "Entry".equals(elementName)) {
                    if (parseTraceEntry(reader, entryHandler)) {
                        entryCount++;
                    }
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String elementName = reader.getLocalName();

                if ("Primary".equals(elementName)) {
                    inPrimary = false;

                } else if ("Trace".equals(elementName)) {
                    return;
                }
            }
        }

        logger.warn("Trace ended without a closing Trace element - parsed {} entries", entryCount);
    }

    /**
     * Parse a single Entry element into either a NodeRef id or an inline Node.
     * @return false if the Entry has neither and was skipped
     */
    private boolean parseTraceEntry(XMLStreamReader reader, BiConsumer<String, Node> entryHandler) throws XMLStreamException {
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
            entryHandler.accept(nodeRefId, null);
            return true;
        }
        if (inlineNode != null) {
            entryHandler.accept(null, inlineNode);
            return true;
        }
        logger.warn("[parseTraceEntry] Entry has neither NodeRef nor inline Node - skipping");
        return false;
    }
}
