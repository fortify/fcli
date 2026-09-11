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
import java.util.List;


public final class FileChange {
    private final String filenameRaw;
    private final String hashRaw;
    private final List<Hunk> hunks;

    public FileChange(String filenameRaw, String hashRaw, List<Hunk> hunks) {
        this.filenameRaw = filenameRaw;
        this.hashRaw = hashRaw;
        this.hunks = hunks;
    }

    public String requiredFilename() {
        return RequiredFields.requireText(filenameRaw, "Filename");
    }

    public String requiredHash() {
        return RequiredFields.requireText(hashRaw, "Hash");
    }

    public List<Hunk> hunks() {
        return hunks;
    }

    public Path resolve(Path sourceBasePath) {
        return sourceBasePath.resolve(requiredFilename()).normalize();
    }
}
