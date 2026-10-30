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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Single SAST issue trace, from dataflow source (first node) to sink (last node). For fixed issues,
 * traces reflect the last scan in which the issue was found; for new and reopened issues, the current scan.
 * Nodes without location are retained to preserve source and sink positions.
 */
public record FoDIssueTrace(List<Node> nodes) {
    /** @param location Node location, or null if not available */
    public record Node(String location, Integer lineNumber) {
        public boolean hasLocation() {
            return StringUtils.isNotBlank(location);
        }
    }

    public FoDIssueTrace {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }

    /** @return First trace node, or null if this trace has no nodes or the first node has no location */
    public Node getSource() {
        return nodes.isEmpty() || !nodes.get(0).hasLocation() ? null : nodes.get(0);
    }

    /** @return Trace nodes with location between source and sink */
    public List<Node> getInterimNodes() {
        return nodes.size() < 3 ? List.of() : nodes.subList(1, nodes.size() - 1).stream().filter(Node::hasLocation).toList();
    }
}
