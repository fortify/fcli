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
package com.fortify.cli.aviator._common.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;

public final class AviatorLocalFprHelper {
    private AviatorLocalFprHelper() {}

    public static void validateLocalFprs(List<Path> fprPaths) {
        if (fprPaths == null || fprPaths.isEmpty()) {
            throw new FcliSimpleException("--fpr must specify at least one FPR file");
        }
        for (Path fprPath : fprPaths) {
            validateLocalFpr(fprPath);
        }
    }

    private static void validateLocalFpr(Path fprPath) {
        if (fprPath == null) {
            throw new FcliSimpleException("--fpr must specify a valid FPR file path");
        }
        if (!Files.exists(fprPath)) {
            throw new FcliSimpleException("FPR file specified by --fpr does not exist: " + fprPath);
        }
        if (!Files.isRegularFile(fprPath)) {
            throw new FcliSimpleException("FPR file specified by --fpr is not a regular file: " + fprPath);
        }
        if (!Files.isReadable(fprPath)) {
            throw new FcliSimpleException("FPR file specified by --fpr is not readable: " + fprPath);
        }
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            fprHandle.validate();
        } catch (FcliSimpleException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FcliSimpleException("FPR file specified by --fpr is not a valid audited SAST FPR: " + fprPath, e);
        } catch (java.io.IOException e) {
            throw new FcliSimpleException("Failed to close FPR file specified by --fpr: " + fprPath, e);
        }
    }
}