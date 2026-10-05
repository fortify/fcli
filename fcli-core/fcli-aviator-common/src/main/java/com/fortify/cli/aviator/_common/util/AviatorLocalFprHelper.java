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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.AbstractFcliException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;

public final class AviatorLocalFprHelper {
    private AviatorLocalFprHelper() {}

    public static void validateLocalFprs(List<Path> fprPaths) {
        validateLocalFprs(fprPaths, "FPR file");
    }

    public static void validateLocalFprs(List<Path> fprPaths, String sourceLabel) {
        FcliSimpleException.throwIf(fprPaths == null || fprPaths.isEmpty(),
                "%s list must contain at least one FPR file", sourceLabel);
        for (Path fprPath : fprPaths) {
            validateLocalFpr(fprPath, sourceLabel);
        }
    }

    private static void validateLocalFpr(Path fprPath, String sourceLabel) {
        FcliSimpleException.throwIf(fprPath == null,
                "%s path must be a valid FPR file path", sourceLabel);
        FcliSimpleException.throwIf(!Files.exists(fprPath),
                "%s does not exist: %s", sourceLabel, fprPath);
        FcliSimpleException.throwIf(!Files.isRegularFile(fprPath),
                "%s is not a regular file: %s", sourceLabel, fprPath);
        FcliSimpleException.throwIf(!Files.isReadable(fprPath),
                "%s is not readable: %s", sourceLabel, fprPath);
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            fprHandle.validate();
        } catch (AbstractFcliException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FcliSimpleException(sourceLabel + " is not a valid audited SAST FPR: " + fprPath, e);
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to close " + sourceLabel + ": " + fprPath, e);
        }
    }
}
