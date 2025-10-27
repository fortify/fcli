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
package com.fortify.cli.common.json;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliTechnicalException;

/**
 *
 * @author Ruud Senden
 */
public class FortifyTraceNodeHelper {
    public static final ArrayNode normalize(ArrayNode traceNodes) {
        return (ArrayNode) new TraceNodeNormalizer().normalizeArray(traceNodes);
    }

    public static final ArrayNode normalizeAndMerge(ArrayNode traceNodes) {
        return (ArrayNode) new NormalizedTraceNodeMerger().normalizeArray(normalize(traceNodes));
    }

    private static enum NormalizedTraceNodeProperty {
        index(), // Generated, not taken from SSC/FoD
        path("fullPath", "location"), line("line", "lineNumber"), text("text", "displayText"), nodeType("nodeType", "actionType"), // Only
                                                                                                                                // for
                                                                                                                                // non-merged
                                                                                                                                // nodes
        sourceOrSink(); // Generated, not taken from SSC/FoD

        private final String[] originalPropertyNames;

        private NormalizedTraceNodeProperty(String... originalPropertyNames) {
            this.originalPropertyNames = originalPropertyNames;
        }

        public final JsonNode getPropertyValue(ObjectNode traceNode) {
            for (var originalPropertyName : originalPropertyNames) {
                var result = traceNode.get(originalPropertyName);
                if (result != null) {
                    return normalize(result);
                }
            }
            return null;
        }

        private final JsonNode normalize(JsonNode result) {
            if (this == nodeType) {
                // TODO Convert to have consistent representation between SSC & FoD
            }
            return result;
        }
    }

    private static abstract class AbstractTraceNodeNormalizer {
        protected static final ObjectMapper OM = JsonHelper.getObjectMapper();

        public final ArrayNode normalizeArray(ArrayNode traceNodes) {
            var result = OM.createArrayNode();
            if (traceNodes != null) {
                traceNodes.forEach(node -> processNode(result, node));
            }
            return result;
        }

        protected final void processNode(ArrayNode result, JsonNode node) {
            if (node instanceof ArrayNode) {
                result.add(normalizeArray((ArrayNode) node));
            } else if (node instanceof ObjectNode) {
                processObject(result, (ObjectNode) node);
            } else {
                throw new FcliTechnicalException("Unexpected trace node type: " + node.getNodeType().name());
            }
        }

        protected final void updateSourceOrSink(ArrayNode parent, ObjectNode result) {
            var lastNodeAdded = getLastNodeAdded(parent);
            var sourceOrSinkProperty = NormalizedTraceNodeProperty.sourceOrSink.name();
            result.put(sourceOrSinkProperty, parent.size() == 0 ? "SOURCE" : "SINK");
            if (lastNodeAdded != null && "SINK".equals(lastNodeAdded.get(sourceOrSinkProperty).asText())) {
                lastNodeAdded.put(sourceOrSinkProperty, "");
            }
        }

        protected final ObjectNode getLastNodeAdded(ArrayNode result) {
            return result == null || result.size() == 0 ? null : (ObjectNode) result.get(result.size() - 1);
        }

        protected abstract void processObject(ArrayNode result, ObjectNode node);
    }

    private static final class TraceNodeNormalizer extends AbstractTraceNodeNormalizer {
        private static final Set<String> SSC_SKIPPED_NODE_TYPES = Set.of("EXTERNAL_ENTRY", "TRACE_RUNTIME_SOURCE", "TRACE_RUNTIME_SINK");
        @Override
        protected final void processObject(ArrayNode result, ObjectNode node) {
            if (node.has("traceEntries")) {
                result.add(normalizeArray((ArrayNode) node.get("traceEntries")));
            } else if (!isSkippedNodeType(node)) {
                result.add(normalizeObject(result, (ObjectNode) node));
            }
        }

        private final JsonNode normalizeObject(ArrayNode parent, ObjectNode node) {
            var result = (ObjectNode) new JsonNodeDeepCopyWalker().walk(node);
            for (var normalizedProperty : NormalizedTraceNodeProperty.values()) {
                result.set(normalizedProperty.name(), normalizedProperty.getPropertyValue(node));
            }
            result.put(NormalizedTraceNodeProperty.index.name(), parent.size());
            updateSourceOrSink(parent, result);
            result.put(NormalizedTraceNodeProperty.sourceOrSink.name(), "");
            return result;
        }

        private static final boolean isSkippedNodeType(JsonNode node) {
            var sscNodeTypeNode = node.get("nodeType");
            return sscNodeTypeNode != null && SSC_SKIPPED_NODE_TYPES.contains(sscNodeTypeNode.asText());
        }
    }

    private static final class NormalizedTraceNodeMerger extends AbstractTraceNodeNormalizer {
        @Override
        protected void processObject(ArrayNode result, ObjectNode value) {
            var lastNodeAdded = getLastNodeAdded(result);
            if (isSameLocation(lastNodeAdded, value)) {
                ((ArrayNode) lastNodeAdded.get("mergedFrom")).add(value);
            } else {
                var node = createNode(result, value);
                result.add(node);
                lastNodeAdded = node;
            }
        }

        private ObjectNode createNode(ArrayNode parent, ObjectNode value) {
            var indexProperty = NormalizedTraceNodeProperty.index.name();
            var pathProperty = NormalizedTraceNodeProperty.path.name();
            var lineProperty = NormalizedTraceNodeProperty.line.name();
            var result = OM.createObjectNode();
            result.put(indexProperty, parent.size());
            result.set(pathProperty, value.get(pathProperty));
            result.set(lineProperty, value.get(lineProperty));
            updateSourceOrSink(parent, result);
            result.set("mergedFrom", OM.createArrayNode().add(value));

            return result;
        }

        private boolean isSameLocation(ObjectNode lastNodeAdded, ObjectNode value) {
            var pathProperty = NormalizedTraceNodeProperty.path.name();
            var lineProperty = NormalizedTraceNodeProperty.line.name();
            return lastNodeAdded != null && lastNodeAdded.get(pathProperty).equals(value.get(pathProperty))
                    && lastNodeAdded.get(lineProperty).equals(value.get(lineProperty));
        }
    }

}
