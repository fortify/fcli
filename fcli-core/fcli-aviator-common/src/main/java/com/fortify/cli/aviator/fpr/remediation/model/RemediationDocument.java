package com.fortify.cli.aviator.fpr.remediation.model;

import java.util.List;

public final class RemediationDocument {
    private final List<Remediation> remediations;

    public RemediationDocument(List<Remediation> remediations) {
        this.remediations = remediations;
    }

    public List<Remediation> remediations() {
        return remediations;
    }
}
