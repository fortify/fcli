package com.fortify.cli.aviator.fpr.model;

import com.fortify.cli.aviator.fpr.jaxb.UnifiedNode;
import com.fortify.cli.aviator.fpr.jaxb.UnifiedNodeRef;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an entry in a UnifiedTrace Primary sequence.
 * Holds either a full UnifiedNode or a UnifiedNodeRef, with isDefault flag.
 * Used to simplify trace processing and resolution.
 */
@Getter
@Setter
@AllArgsConstructor
public class TraceEntry {
    private Object node; // UnifiedNode (from Entry.Node) or UnifiedNodeRef

    private boolean isDefault;

    /**
     * Checks if this entry contains a full UnifiedNode (from Entry.Node).
     *
     * @return true if node is UnifiedNode instance
     */
    public boolean isFullNode() {
        return node instanceof UnifiedNode;
    }

    /**
     * Checks if this entry contains a UnifiedNodeRef.
     *
     * @return true if node is UnifiedNodeRef instance
     */
    public boolean isNodeRef() {
        return node instanceof UnifiedNodeRef;
    }

    /**
     * Gets the UnifiedNode if present, else null.
     *
     * @return UnifiedNode or null if not a full node
     */
    public UnifiedNode getAsUnifiedNode() {
        return isFullNode() ? (UnifiedNode) node : null;
    }

    /**
     * Gets the UnifiedNodeRef if present, else null.
     *
     * @return UnifiedNodeRef or null if not a ref
     */
    public UnifiedNodeRef getAsUnifiedNodeRef() {
        return isNodeRef() ? (UnifiedNodeRef) node : null;
    }
}