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
package com.fortify.cli.aviator.fpr.processor;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Pure DOM helper that retains only selected {@code <Issue>} nodes in {@code audit.xml}.
 * Used for SSC upload isolation: non-retained issues are omitted so SSC does not overwrite
 * their concurrent audit-tag edits.
 */
final class AuditXmlIssuePruner {
    private static final Logger LOG = LoggerFactory.getLogger(AuditXmlIssuePruner.class);
    private static final String AUDIT_NAMESPACE_URI = "xmlns://www.fortify.com/schema/audit";

    private AuditXmlIssuePruner() {}

    /**
     * Removes every {@code <Issue>} whose {@code instanceId} is absent from {@code retainIds}.
     * Issues with a blank {@code instanceId} are always removed.
     * <p>
     * Performance: one reverse pass over the live {@link NodeList} with O(1) set lookups.
     *
     * @param auditDoc   in-memory audit.xml document (mutated in place); no-op if null
     * @param retainIds  instance IDs written in the current save; treated as empty if null
     * @return number of Issue nodes removed
     */
    static int retainOnly(Document auditDoc, Set<String> retainIds) {
        if (auditDoc == null) {
            LOG.warn("Cannot prune audit.xml issues: document is null.");
            return 0;
        }
        Set<String> safeRetainIds = retainIds == null ? Set.of() : retainIds;
        NodeList issueNodes = auditDoc.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Issue");
        int beforeCount = issueNodes.getLength();
        int removedCount = 0;

        for (int i = beforeCount - 1; i >= 0; i--) {
            if (!(issueNodes.item(i) instanceof Element issueElement)) {
                continue;
            }
            String instanceId = issueElement.getAttribute("instanceId");
            if (instanceId == null || instanceId.isBlank() || !safeRetainIds.contains(instanceId)) {
                Node parent = issueElement.getParentNode();
                if (parent != null) {
                    parent.removeChild(issueElement);
                    removedCount++;
                }
            }
        }

        int retainedCount = beforeCount - removedCount;
        LOG.info("audit.xml isolation: retained {} of {} issue(s) (pruned {})",
                retainedCount, beforeCount, removedCount);
        return removedCount;
    }
}
