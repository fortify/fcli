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
import com.fortify.cli.common.exception.FcliTechnicalException;

public final class AviatorLocalFprHelper {
    private AviatorLocalFprHelper() {}

    public static void validateLocalFprs(List<Path> fprPaths) {
        validateLocalFprs(fprPaths, "FPR file");
    }

    public static void validateLocalFprs(List<Path> fprPaths, String sourceLabel) {
        if (fprPaths == null || fprPaths.isEmpty()) {
            throw new FcliSimpleException(sourceLabel + " list must contain at least one FPR file");
        }
        for (Path fprPath : fprPaths) {
            validateLocalFpr(fprPath, sourceLabel);
        }
    }

    private static void validateLocalFpr(Path fprPath, String sourceLabel) {
        if (fprPath == null) {
            throw new FcliSimpleException(sourceLabel + " path must be a valid FPR file path");
        }
        if (!Files.exists(fprPath)) {
            throw new FcliSimpleException(sourceLabel + " does not exist: " + fprPath);
        }
        if (!Files.isRegularFile(fprPath)) {
            throw new FcliSimpleException(sourceLabel + " is not a regular file: " + fprPath);
        }
        if (!Files.isReadable(fprPath)) {
            throw new FcliSimpleException(sourceLabel + " is not readable: " + fprPath);
        }
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            fprHandle.validate();
        } catch (FcliSimpleException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FcliSimpleException(sourceLabel + " is not a valid audited SAST FPR: " + fprPath, e);
        } catch (java.io.IOException e) {
            throw new FcliTechnicalException("Failed to close " + sourceLabel + ": " + fprPath, e);
        }
    }
}
