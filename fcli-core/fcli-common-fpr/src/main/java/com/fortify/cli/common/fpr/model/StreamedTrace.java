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
package com.fortify.cli.common.fpr.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Streaming representation of a UnifiedTrace from FVDL.
 * Replaces JAXB UnifiedTrace for streaming parsing.
 */
@Data
@Builder
public class StreamedTrace {
    private String id;
    private Primary primary;

    /**
     * Primary trace container with entries.
     */
    @Data
    @Builder
    public static class Primary {
        @Builder.Default
        private List<Entry> entries = new ArrayList<>();

        /**
         * Single trace entry: either an inline {@link #node} or a NodeRef {@link #nodeId}.
         * NodeRefs are resolved after parsing, because the referenced node can come later in the file.
         */
        @Data
        @Builder
        public static class Entry {
            private String nodeId;      // NodeRef id; null for inline nodes
            private Node node;           // Inline node; null for NodeRefs
            private boolean isDefault;

            /**
             * Check if this entry is a node reference (vs inline node).
             */
            public boolean isNodeRef() {
                return nodeId != null && !nodeId.isEmpty();
            }

            /**
             * Check if this entry has a valid node object.
             */
            public boolean hasNode() {
                return node != null;
            }
        }
    }
}
