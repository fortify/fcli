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

    // Rule metadata cache: classId -> metadata map
    private Map<String, Map<String, String>> ruleMetadata = new ConcurrentHashMap<>();

    // Node pool: nodeId -> minimal node data
    private Map<String, Node> nodePool = new ConcurrentHashMap<>();

    // Trace pool: traceId -> StreamedTrace
    private Map<String, StreamedTrace> tracePool = new ConcurrentHashMap<>();

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
}
