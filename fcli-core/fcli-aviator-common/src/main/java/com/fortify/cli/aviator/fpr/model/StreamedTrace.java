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



import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Streaming representation of a UnifiedTrace from FVDL.
 * Replaces JAXB UnifiedTrace for streaming parsing.
 *
 * IMPORTANT: Entry now stores full Node objects to preserve inline nodes
 * through Post-Processing (required for innerStackTrace building).
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
         * Single trace entry - can contain inline Node or NodeRef.
         *
         * CHANGED: Now stores full Node object instead of just nodeId.
         * This allows inline nodes (without IDs) to be preserved through Post-Processing.
         * Previously, inline nodes were lost because NodePool lookup with null ID failed.
         */
        @Data
        @Builder
        public static class Entry {
            private String nodeId;      // For backward compatibility and debugging (can be null for inline nodes)
            private Node node;           // CHANGED: Full Node object (from com.fortify.aviator.cli.fpr.models.Node)
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
