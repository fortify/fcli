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

import java.nio.file.Path;

public record RemediationKey(String fileName, Path filePath, int lineFrom, int lineTo, String comparisonCode) {

    public static RemediationKey of(FileChange fileChange, Hunk hunk, Path sourceBasePath, String comparisonCode) {
        String fileName = fileChange.requiredFilename();
        Path filePath = sourceBasePath.resolve(fileName).normalize();
        int lineFrom = hunk.lineFrom();
        int lineTo = hunk.lineTo();
        return new RemediationKey(fileName, filePath, lineFrom, lineTo, comparisonCode);
    }
}
