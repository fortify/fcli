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
package com.fortify.cli.fpr._common.cli.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;

import picocli.CommandLine.Option;

/**
 * Shared mixin providing the {@code --fpr} option for specifying a local FPR file path.
 * Creates and returns a validated {@link FprHandle} for accessing the FPR contents.
 */
public class FPRFileMixin {
    @Option(names = {"--fpr"}, required = true, order = 1)
    private Path fprPath;

    public FprHandle createFprHandle() {
        if (fprPath == null || !Files.exists(fprPath)) {
            throw new FcliSimpleException("FPR file not found: " + fprPath);
        }
        if (!fprPath.toString().toLowerCase().endsWith(".fpr")) {
            throw new FcliSimpleException("File does not have .fpr extension: " + fprPath);
        }
        return new FprHandle(fprPath);
    }
}
