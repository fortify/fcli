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
package com.fortify.cli.common.fpr;

import java.util.List;
import java.util.Set;

/**
 * Product-specific audit tag knowledge used when deriving the audited flag and issue status.
 *
 * @param additionalStatusTagIds tag ids consulted for the issue status after the standard
 *                               Auditor Status, FoD and Analysis tags, in order
 * @param auditedMarkerTagIds    tag ids whose mere presence marks an issue as audited
 */
public record AuditTagConfig(List<String> additionalStatusTagIds, Set<String> auditedMarkerTagIds) {
    public static final AuditTagConfig DEFAULT = new AuditTagConfig(List.of(), Set.of());

    public AuditTagConfig {
        additionalStatusTagIds = List.copyOf(additionalStatusTagIds);
        auditedMarkerTagIds = Set.copyOf(auditedMarkerTagIds);
    }
}
