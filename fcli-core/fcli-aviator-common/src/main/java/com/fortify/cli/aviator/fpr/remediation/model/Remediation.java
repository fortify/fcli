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
