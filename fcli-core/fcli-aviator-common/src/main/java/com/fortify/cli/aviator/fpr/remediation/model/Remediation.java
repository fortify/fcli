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
package com.fortify.cli.aviator.fpr.remediation.model;

import java.util.List;

public final class Remediation {
    private final String instanceId;
    private final List<FileChange> fileChanges;

    public Remediation(String instanceId, List<FileChange> fileChanges) {
        this.instanceId = instanceId;
        this.fileChanges = fileChanges;
    }

    public String instanceId() {
        return instanceId;
    }

    public List<FileChange> fileChanges() {
        return fileChanges;
    }
}
