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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.jaxb.SourceLocationType;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNode;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNode.Knowledge.Fact;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNode.Reason;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNodePoolType;
import com.fortify.cli.aviator.fpr.model.Node;
import com.fortify.cli.aviator.fpr.utils.FileUtils;
import com.fortify.cli.aviator.util.FprHandle;

import lombok.Getter;

/**
 * Processor for UnifiedNodePool in FVDL. Extracts Node objects and caches them by ID.
 */
public class NodeProcessor {
    private static final Logger logger = LoggerFactory.getLogger(NodeProcessor.class);
    @Getter private final Map<String, Node> nodePool = new ConcurrentHashMap<>();
    @Getter private final Map<String, UnifiedNode> rawNodePool = new ConcurrentHashMap<>();
    private final Map<String, String> sourceFileCache = new ConcurrentHashMap<>();
    private final FileUtils fileUtils;
    private final Map<String, String> sourceFileMap;
    private final FprHandle fprHandle;



    public NodeProcessor(FprHandle fprHandle, FileUtils fileUtils, Map<String, String> sourceFileMap) {
        this.fprHandle = fprHandle;
        this.fileUtils = fileUtils;
        this.sourceFileMap = sourceFileMap;
    }

    /**
     * Processes UnifiedNodePool and populates nodePool.
     */
    public void process(UnifiedNodePoolType nodePoolElement) {
        if (nodePoolElement == null || nodePoolElement.getNode() == null) {
            logger.debug("No UnifiedNodePool or empty Node list");
            return;
        }

        for (UnifiedNodePoolType.Node jaxbNodeFromPool : nodePoolElement.getNode()) {
            String nodeId = Integer.toString(jaxbNodeFromPool.getId());
            rawNodePool.put(nodeId, jaxbNodeFromPool);

            Node customNode = processNode(jaxbNodeFromPool);
            if (customNode != null) {
                nodePool.put(customNode.getId(), customNode);
            }
        }
        logger.info("NodeProcessor finished. Received sourceFileMap with {} unique file paths.", sourceFileMap.size());
    }

    /**
     * Processes a single UnifiedNode into a Node object.
     */
    public Node processNode(UnifiedNode jaxbNode) {
        if (jaxbNode == null) {
            logger.warn("Invalid Node: null");
            return null;
        }

        String id = "";
        if (jaxbNode instanceof UnifiedNodePoolType.Node) {
            Integer nodeId = ((UnifiedNodePoolType.Node) jaxbNode).getId();
            if (nodeId == null) {
                logger.warn("Invalid UnifiedNodePool Node: missing ID");
                return null;
            }
            id = nodeId.toString();
        }

        // Extract from base UnifiedNode
        SourceLocationType loc = jaxbNode.getSourceLocation();
        String filePath = loc != null ? loc.getPath() : "";

        String snippet = "";
        if (filePath != null && !filePath.isEmpty()) {
            snippet = sourceFileCache.computeIfAbsent(filePath, key ->
                    fileUtils.getSourceFileContent(this.fprHandle, key) // Pass FprHandle
                            .orElse(""));
        }

        int line = loc != null && loc.getLine() != null ? loc.getLine().intValue() : 0;
        int lineEnd = loc != null && loc.getLineEnd() != null ? loc.getLineEnd().intValue() : line;
        int colStart = loc != null && loc.getColStart() != null ? loc.getColStart().intValue() : 0;
        int colEnd = loc != null && loc.getColEnd() != null ? loc.getColEnd().intValue() : 0;
        String contextId = loc != null && loc.getContextId() != null ? loc.getContextId().toString() : "";
        String snippetId = loc != null ? loc.getSnippet() : "";

        String actionType = jaxbNode.getAction() != null ? jaxbNode.getAction().getType() : "";
        String additionalInfo = jaxbNode.getAction() != null ? jaxbNode.getAction().getValue() : "";

        SourceLocationType secondaryLoc = jaxbNode.getSecondaryLocation();
        String secondaryPath = secondaryLoc != null ? secondaryLoc.getPath() : "";
        int secondaryLine = secondaryLoc != null && secondaryLoc.getLine() != null ? secondaryLoc.getLine().intValue() : 0;
        int secondaryLineEnd = secondaryLoc != null && secondaryLoc.getLineEnd() != null ? secondaryLoc.getLineEnd().intValue() : secondaryLine;
        int secondaryColStart = secondaryLoc != null && secondaryLoc.getColStart() != null ? secondaryLoc.getColStart().intValue() : 0;
        int secondaryColEnd = secondaryLoc != null && secondaryLoc.getColEnd() != null ? secondaryLoc.getColEnd().intValue() : 0;
        String secondaryContextId = secondaryLoc != null && secondaryLoc.getContextId() != null ? secondaryLoc.getContextId().toString() : "";

        // Extract detailsOnly and label attributes
        boolean detailsOnly = jaxbNode.isDetailsOnly();
        String label = jaxbNode.getLabel() != null ? jaxbNode.getLabel() : "";

        // Extract ruleId from Reason (first <Rule ruleID="..."/> if present)
        String ruleId = "";
        Reason reason = jaxbNode.getReason();
        String reasonText = "";
        if (reason != null) {
            StringBuilder sb = new StringBuilder();
            for (Object reasonItem : reason.getTraceOrTraceRefOrInductionRef()) {
                if (reasonItem instanceof Reason.Rule) {
                    Reason.Rule rule = (Reason.Rule) reasonItem;
                    if (ruleId.isEmpty()) {
                        ruleId = rule.getRuleID();
                    }
                    sb.append("Rule: ").append(rule.getRuleID()).append("\n");
                }
            }
            reasonText = sb.toString().trim();
        }

        // Extract taintFlags and knowledge from Knowledge/Fact
        List<String> taintFlags = new ArrayList<>();
        Map<String, String> knowledge = new HashMap<>();
        if (jaxbNode.getKnowledge() != null) {
            for (Fact fact : jaxbNode.getKnowledge().getFact()) {
                String factType = fact.getType();
                String factValue = fact.getValue() != null ? fact.getValue() : "";
                if ("TaintFlags".equalsIgnoreCase(factType)) {
                    taintFlags.add(factValue);
                } else {
                    knowledge.put(factType, factValue);
                }
            }
        }

        return new Node(id, filePath, line, lineEnd, colStart, colEnd, contextId, snippet, actionType, additionalInfo, ruleId, taintFlags, knowledge,
                secondaryPath, secondaryLine, secondaryLineEnd, secondaryColStart, secondaryColEnd, secondaryContextId, detailsOnly, label, reasonText);
    }
}
