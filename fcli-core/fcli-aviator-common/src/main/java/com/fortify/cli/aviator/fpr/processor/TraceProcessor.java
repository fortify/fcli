package com.fortify.cli.aviator.fpr.processor;

import com.fortify.cli.aviator.audit.model.Fragment;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNode;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNodeRef;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedTrace;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedTracePool;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedTraceRef;
import com.fortify.cli.aviator.fpr.model.Node;
import com.fortify.cli.aviator.fpr.model.TraceEntry;
import com.fortify.cli.aviator.fpr.utils.FileUtils;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processor for UnifiedTracePool and individual UnifiedTrace elements in FVDL.
 * Builds stack traces with recursive resolution and depth limiting.
 */
public class TraceProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TraceProcessor.class);
    private static final int MAX_RECURSION_DEPTH = 50; // Configurable depth limit to prevent stack overflow
    private final Map<String, UnifiedTrace> tracePool = new ConcurrentHashMap<>();
    private final NodeProcessor nodeProcessor;
    private final SnippetProcessor snippetProcessor;
    private final FileUtils fileUtils;
    private final Map<String, String> sourceFileMap;
    private final Path extractedPath;

    /**
     * Constructor with dependencies.
     *
     * @param nodeProcessor    For node pool access
     * @param snippetProcessor For snippet access
     * @param fileUtils        For file fragment extraction
     * @param sourceFileMap    Map of relative to actual file paths
     */
    public TraceProcessor(Path extractedPath, NodeProcessor nodeProcessor, SnippetProcessor snippetProcessor, FileUtils fileUtils, Map<String, String> sourceFileMap) {
        this.extractedPath = extractedPath; // <-- INITIALIZE IT
        this.nodeProcessor = nodeProcessor;
        this.snippetProcessor = snippetProcessor;
        this.fileUtils = fileUtils;
        this.sourceFileMap = sourceFileMap;
    }

    /**
     * Processes UnifiedTracePool and populates tracePool.
     *
     * @param tracePoolElement JAXB UnifiedTracePool
     */
    public void process(UnifiedTracePool tracePoolElement) {
        if (tracePoolElement == null || tracePoolElement.getTrace() == null) {
            logger.debug("No UnifiedTracePool or empty Trace list");
            return;
        }

        for (UnifiedTrace trace : tracePoolElement.getTrace()) {
            Integer traceId = trace.getId();
            if (traceId != null && traceId != 0) {
                tracePool.put(traceId.toString(), trace);
            } else {
                logger.warn("Trace missing or invalid ID, skipping");
            }
        }
    }

    /**
     * Gets the cached trace pool.
     *
     * @return Map of trace ID to UnifiedTrace
     */
    public Map<String, UnifiedTrace> getTracePool() {
        return tracePool;
    }

    /**
     * Resolves a UnifiedTrace into a list of stack traces.
     *
     * @param trace JAXB UnifiedTrace
     * @return List of stack trace lists
     * @throws IOException If file access fails
     */
    public List<List<StackTraceElement>> resolveTrace(UnifiedTrace trace) throws IOException {
        List<List<StackTraceElement>> stackTraces = new ArrayList<>();
        if (trace == null || trace.getPrimary() == null) {
            logger.debug("Invalid or empty trace");
            return stackTraces;
        }

        Map<String, Boolean> visited = new HashMap<>(); // For cycle detection
        resolveTraceRecursive(trace, stackTraces, 0, visited);
        return stackTraces;
    }

    /**
     * Recursively resolves a UnifiedTrace with depth limiting and cycle detection.
     *
     * @param trace    Current UnifiedTrace
     * @param stackTraces Output list of stack traces
     * @param depth    Current recursion depth
     * @param visited  Map of visited trace IDs to avoid cycles
     * @throws IOException If file access fails
     */
    private void resolveTraceRecursive(UnifiedTrace trace, List<List<StackTraceElement>> stackTraces, int depth, Map<String, Boolean> visited) throws IOException {
        Integer traceIdInt = trace.getId();
        String traceId = traceIdInt != null ? traceIdInt.toString() : "";

        // Cycle detection for top-level traces
        if (!traceId.isEmpty()) {
            if (visited.getOrDefault(traceId, false)) {
                logger.debug("Cycle detected for trace ID: {}", traceId);
                return;
            }
            visited.put(traceId, true);
        }

        // This method now only processes ONE level of a trace into a single list.
        // The recursive building of inner traces is handled by buildStackTraceElement.
        List<StackTraceElement> currentStack = new ArrayList<>();
        for (UnifiedTrace.Primary.Entry entry : trace.getPrimary().getEntry()) {
            // We now pass the 'visited' map down to the builder.
            StackTraceElement ste = buildStackTraceElement(processEntry(entry), depth, visited);
            if (ste != null) {
                currentStack.add(ste);
            }
        }

        if (!currentStack.isEmpty()) {
            stackTraces.add(currentStack);
        }

        if (!traceId.isEmpty()) {
            visited.remove(traceId);
        }
    }

    /**
     * Processes a single entry into a TraceEntry.
     *
     * @param entry JAXB Entry
     * @return TraceEntry with node and isDefault
     */
    private TraceEntry processEntry(UnifiedTrace.Primary.Entry entry) {
        Object nodeObject = entry.getNode() != null ? entry.getNode() : entry.getNodeRef();
        boolean isDefault = entry.getNode() != null && entry.getNode().isIsDefault() != null && entry.getNode().isIsDefault();
        return new TraceEntry(nodeObject, isDefault);
    }

    /**
     * Builds a StackTraceElement from a TraceEntry.
     *
     * @param entry TraceEntry
     * @param depth Recursion depth for logging
     * @return StackTraceElement or null if invalid
     * @throws IOException If file access fails
     */
    private StackTraceElement buildStackTraceElement(TraceEntry entry, int depth, Map<String, Boolean> visited) throws IOException {
        if (entry == null || depth > MAX_RECURSION_DEPTH) {
            return null;
        }

        // --- Resolve the current node (same as before) ---
        Object nodeObject = entry.getNode();
        UnifiedNodeRef nodeRef = nodeObject instanceof UnifiedNodeRef ? (UnifiedNodeRef) nodeObject : null;
        UnifiedNode unifiedNode = nodeObject instanceof UnifiedNode ? (UnifiedNode) nodeObject : null;

        Node resolvedNode;
        if (unifiedNode != null) {
            resolvedNode = nodeProcessor.processNode(unifiedNode);
        } else {
            resolvedNode = resolveNodeRef(nodeRef);
        }

        if (resolvedNode == null) {
            logger.warn("Could not resolve node for trace entry at depth {}", depth);
            return null;
        }

        String filename = resolvedNode.getFilePath();
        int line = resolvedNode.getLine();
        String codeLine = fileUtils.getLineFromFile(this.extractedPath, this.sourceFileMap, filename, line);
        Fragment fragment = fileUtils.getFragmentFromFile(this.extractedPath, this.sourceFileMap, filename, line, 5, 2);
        String nodeType = resolvedNode.getActionType();
        String additionalInfo = resolvedNode.getAdditionalInfo();
        String taintFlags = String.join(", ", resolvedNode.getTaintFlags());

        StackTraceElement ste = new StackTraceElement(filename, line, codeLine, nodeType, fragment, additionalInfo, taintFlags);
        ste.setDefault(resolvedNode.isDetailsOnly());
        ste.setReason(resolvedNode.getReasonText());
        ste.setKnowledge(resolvedNode.getKnowledge());

        if (unifiedNode != null && unifiedNode.getReason() != null) {
            List<StackTraceElement> innerTrace = new ArrayList<>();

            for (Object reasonItem : unifiedNode.getReason().getTraceOrTraceRefOrInductionRef()) {
                if (reasonItem instanceof UnifiedTrace nestedTrace) {
                    // This is a full, inline <Trace>
                    // We process its entries recursively
                    for (UnifiedTrace.Primary.Entry childEntry : nestedTrace.getPrimary().getEntry()) {
                        StackTraceElement innerSte = buildStackTraceElement(processEntry(childEntry), depth + 1, visited);
                        if (innerSte != null) {
                            innerTrace.add(innerSte);
                        }
                    }
                } else if (reasonItem instanceof UnifiedTraceRef traceRef) {
                    // This is a reference to a trace in the pool
                    String refId = Integer.toString(traceRef.getId());
                    if (!refId.isEmpty() && !visited.getOrDefault(refId, false)) {

                        visited.put(refId, true); // Mark as visited to prevent cycles
                        UnifiedTrace referencedTrace = tracePool.get(refId);
                        if (referencedTrace != null) {
                            for (UnifiedTrace.Primary.Entry childEntry : referencedTrace.getPrimary().getEntry()) {
                                StackTraceElement innerSte = buildStackTraceElement(processEntry(childEntry), depth + 1, visited);
                                if (innerSte != null) {
                                    innerTrace.add(innerSte);
                                }
                            }
                        }
                        visited.remove(refId);
                    }
                }
            }
            ste.setInnerStackTrace(innerTrace);
        }

        return ste;
    }

    /**
     * Resolves a NodeRef to a Node from the nodePool.
     *
     * @param nodeRef JAXB UnifiedNodeRef
     * @return Node or null if not found
     */
    private Node resolveNodeRef(UnifiedNodeRef nodeRef) {
        if (nodeRef == null) {
            logger.warn("Invalid NodeRef: reference is null");
            return null;
        }
        String refId = Integer.toString(nodeRef.getId());
        Node foundNode = nodeProcessor.getNodePool().get(refId);
        if (foundNode == null) {
            logger.warn("Could not find Node in pool for ID: {}", refId);
        }
        return foundNode;
    }
}