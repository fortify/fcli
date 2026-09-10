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

public final class RemediationDocument {
    private final List<Remediation> remediations;

    public RemediationDocument(List<Remediation> remediations) {
        this.remediations = remediations;
    }

    public List<Remediation> remediations() {
        return remediations;
    }
}
