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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.fpr.model.FVDLMetadata;
import com.fortify.cli.common.fpr.model.Fragment;
import com.fortify.cli.common.fpr.model.Node;
import com.fortify.cli.common.fpr.model.StackTraceElement;
import com.fortify.cli.common.fpr.model.StreamedTrace;
import com.fortify.cli.common.fpr.model.StreamedVulnerability;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.utils.FileUtils;

/**
 * Converts streamed FVDL traces into {@link StackTraceElement} lists for a single FPR parse.
 * <p>
 * An element depends only on its {@link Node}, so each Node instance is converted once and the
 * element is shared by every vulnerability whose traces pass through that node. Callers must
 * treat returned elements as read-only. Not thread-safe.
 */
final class StackTraceConverter {
    private static final Logger logger = LoggerFactory.getLogger(StackTraceConverter.class);
    private static final int FRAGMENT_LINES_BEFORE = 5;
    private static final int FRAGMENT_LINES_AFTER = 2;

    private final FileUtils fileUtils;
    private final FprHandle fprHandle;
    private final FVDLMetadata fvdlMetadata;

    // Identity keys: node ids are not unique (forward-reference placeholders, inline nodes).
    // Two caches so an element never receives a Reason trace containing itself.
    private final Map<Node, StackTraceElement> traceElements = new IdentityHashMap<>();
    private final Map<Node, StackTraceElement> plainElements = new IdentityHashMap<>();
    // Inline nodes repeat the same paths and calls; keep one copy of each string.
    private final Map<String, String> sharedStrings = new HashMap<>();

    StackTraceConverter(FileUtils fileUtils, FprHandle fprHandle, FVDLMetadata fvdlMetadata) {
        this.fileUtils = fileUtils;
        this.fprHandle = fprHandle;
        this.fvdlMetadata = fvdlMetadata;
    }

    /** Converts each trace into its own stack trace list, preserving trace boundaries. */
    List<List<StackTraceElement>> toStackTraces(List<StreamedVulnerability.Trace> traces) {
        if (traces == null || traces.isEmpty()) {
            return new ArrayList<>();
        }
        List<List<StackTraceElement>> stackTraces = new ArrayList<>(traces.size());
        for (StreamedVulnerability.Trace trace : traces) {
            if (trace.getNodes() == null || trace.getNodes().isEmpty()) {
                logger.warn("Trace has no nodes, skipping");
                continue;
            }
            List<StackTraceElement> stackTrace = new ArrayList<>(trace.getNodes().size());
            for (Node node : trace.getNodes()) {
                stackTrace.add(isPoolNode(node) ? traceElement(node) : uncachedTraceElement(node));
            }
            stackTraces.add(stackTrace);
        }
        return stackTraces;
    }

    /** Releases cache references; elements already handed out stay valid. */
    void clear() {
        traceElements.clear();
        plainElements.clear();
        sharedStrings.clear();
    }

    private boolean isPoolNode(Node node) {
        return fvdlMetadata.getNodePool().get(node.getId()) == node;
    }

    /** Inline vulnerability nodes occur once, so caching them would only keep them alive until the parse ends. */
    private StackTraceElement uncachedTraceElement(Node node) {
        StackTraceElement element = createElement(node);
        List<StackTraceElement> reasonStackTrace = buildReasonStackTrace(node);
        if (!reasonStackTrace.isEmpty()) {
            element.setInnerStackTrace(reasonStackTrace);
        }
        return element;
    }

    /** Element for a primary trace node, with the node's Reason traces as its inner stack trace. */
    private StackTraceElement traceElement(Node node) {
        return traceElements.computeIfAbsent(node, this::createTraceElement);
    }

    /** Element without an inner stack trace; used for Reason trace nodes and nodes without Reason traces. */
    private StackTraceElement plainElement(Node node) {
        return plainElements.computeIfAbsent(node, this::createElement);
    }

    private StackTraceElement createTraceElement(Node node) {
        List<StackTraceElement> reasonStackTrace = buildReasonStackTrace(node);
        if (reasonStackTrace.isEmpty()) {
            return plainElement(node);
        }
        StackTraceElement element = createElement(node);
        element.setInnerStackTrace(reasonStackTrace);
        return element;
    }

    private List<StackTraceElement> buildReasonStackTrace(Node node) {
        List<StackTraceElement> reasonStackTrace = new ArrayList<>();
        if (node.getReasonInlineTraces() != null) {
            for (StreamedTrace inlineTrace : node.getReasonInlineTraces()) {
                addReasonElements(inlineTrace, reasonStackTrace);
            }
        }
        if (node.getReasonTraceRefs() != null) {
            for (String traceRefId : node.getReasonTraceRefs()) {
                StreamedTrace referencedTrace = fvdlMetadata.getTracePool().get(traceRefId);
                if (referencedTrace == null) {
                    logger.warn("TraceRef {} not found in TracePool for node {}", traceRefId, node.getId());
                } else {
                    addReasonElements(referencedTrace, reasonStackTrace);
                }
            }
        }
        return reasonStackTrace;
    }

    private void addReasonElements(StreamedTrace trace, List<StackTraceElement> target) {
        if (trace == null || trace.getPrimary() == null || trace.getPrimary().getEntries() == null) {
            return;
        }
        for (StreamedTrace.Primary.Entry entry : trace.getPrimary().getEntries()) {
            Node node = resolveEntryNode(entry);
            if (node != null) {
                target.add(plainElement(node));
            }
        }
    }

    /** Inline entries carry their node; NodeRef entries resolve from the complete node pool. */
    private Node resolveEntryNode(StreamedTrace.Primary.Entry entry) {
        if (entry.getNode() != null) {
            return entry.getNode();
        }
        String nodeId = entry.getNodeId();
        if (nodeId == null) {
            logger.warn("Entry has null node and null nodeId - skipping");
            return null;
        }
        Node node = fvdlMetadata.getNodePool().get(nodeId);
        if (node == null) {
            logger.warn("Node {} referenced in trace not found in NodePool, using placeholder", nodeId);
            return NodeParser.createPlaceholderNode(nodeId);
        }
        return node;
    }

    private StackTraceElement createElement(Node node) {
        String taintFlags = node.getTaintFlags() == null ? "" : String.join(", ", node.getTaintFlags());
        Fragment fragment = fileUtils.getFragmentFromFile(fprHandle, node.getFilePath(), node.getLine(),
            FRAGMENT_LINES_BEFORE, FRAGMENT_LINES_AFTER);
        String codeLine = fileUtils.getLineFromFile(fprHandle, node.getFilePath(), node.getLine());

        StackTraceElement element = StackTraceElement.builder()
            .filename(share(node.getFilePath()))
            .line(node.getLine())
            .code(codeLine)
            .nodeType(share(node.getActionType() != null ? node.getActionType() : ""))
            .fragment(fragment)
            .additionalInfo(share(node.getAdditionalInfo() != null ? node.getAdditionalInfo() : ""))
            .taintflags(share(taintFlags))
            .build();
        if (node.getLabel() != null && !node.getLabel().isEmpty()) {
            element.setReason(share(node.getLabel()));
        }
        if (node.getKnowledge() != null) {
            element.setKnowledge(node.getKnowledge());
        }
        return element;
    }

    private String share(String value) {
        return value == null ? null : sharedStrings.computeIfAbsent(value, v -> v);
    }
}
