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


import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

/**
 * Container for FPR metadata extracted during streaming parse.
 */
@Data
public class FVDLMetadata {

    private String buildId;
    private String projectName;
    private String projectVersion;
    private String engineVersion;
    private String analysisType = "SCA";

    // Rule metadata cache: classId -> metadata map
    private Map<String, Map<String, String>> ruleMetadata = new ConcurrentHashMap<>();

    // Node pool: nodeId -> minimal node data
    private Map<String, Node> nodePool = new ConcurrentHashMap<>();

    // Trace pool: traceId -> StreamedTrace
    private Map<String, StreamedTrace> tracePool = new ConcurrentHashMap<>();

    // Context pool: contextId -> parsed context information
    private Map<String, ContextInfo> contextPool = new ConcurrentHashMap<>();

    // Description cache: classID -> StreamedDescription
    private Map<String, StreamedDescription> descriptionCache = new ConcurrentHashMap<>();

    // Statistics
    private long totalVulnerabilities;
    private long totalNodes;
    private long totalTraces;


    @Data
    public static class NodeData {
        private String nodeId;
        private String filePath;
        private Integer line;
        private Integer lineEnd;
        private Integer colStart;
        private Integer colEnd;
        private String actionType;
        private String label;
    }

    @Data
    public static class ContextInfo {
        private String id;
        private String namespace;
        private String className;
        private String functionName;
        private String filename;
        private Integer startLine;

        public String getContextString() {
            return joinNonBlank(namespace, className, functionName);
        }

        public String getQualifiedClassName() {
            return joinNonBlank(namespace, className);
        }

        public String getQualifiedFunctionName() {
            if (functionName == null || functionName.isBlank()) {
                return getQualifiedClassName();
            }

            String qualifiedClassName = getQualifiedClassName();
            StringBuilder builder = new StringBuilder();
            if (!qualifiedClassName.isBlank()) {
                builder.append(qualifiedClassName).append('.');
            }
            builder.append(functionName);
            if (!functionName.startsWith("http")) {
                builder.append("()");
            }
            return builder.toString();
        }

        private String joinNonBlank(String... parts) {
            StringJoiner joiner = new StringJoiner(".");
            for (String part : parts) {
                if (part != null && !part.isBlank()) {
                    joiner.add(part);
                }
            }
            return joiner.toString();
        }
    }
}
