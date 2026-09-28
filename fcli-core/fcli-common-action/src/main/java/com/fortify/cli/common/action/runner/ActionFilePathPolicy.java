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
package com.fortify.cli.common.action.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.exception.FcliTechnicalException;

import lombok.RequiredArgsConstructor;

/**
 * Resolves and validates file paths that are specified in action definitions, like the
 * rest.call response.file property. As actions may come from untrusted sources, such paths
 * must by default be located in the working directory (or one of its subdirectories); the
 * user running the action can lift this restriction through a command line option.
 */
@RequiredArgsConstructor
public final class ActionFilePathPolicy {
    /** Real path of the directory against which paths are resolved and validated */
    private final Path rootDir;
    private final boolean allowUnrestrictedPaths;

    /**
     * Create a policy for the current working directory.
     */
    public static ActionFilePathPolicy forCurrentDirectory(boolean allowUnrestrictedPaths) {
        try {
            return new ActionFilePathPolicy(Path.of("").toAbsolutePath().toRealPath(), allowUnrestrictedPaths);
        } catch ( IOException e ) {
            throw new FcliTechnicalException("Unable to determine current working directory", e);
        }
    }

    /**
     * Resolve the given path against the working directory.
     *
     * @return absolute, normalized path
     * @throws FcliActionValidationException if the path is located outside the working directory
     *         and unrestricted paths are not allowed
     */
    public Path resolve(String path) {
        var result = rootDir.resolve(path).normalize();
        if ( !allowUnrestrictedPaths && !isInsideRootDir(result) ) {
            throw new FcliActionValidationException(String.format(
                    "response.file resolves outside the working directory: %s; run the action with --allow-unrestricted-file-paths to allow this",
                    path));
        }
        return result;
    }

    /**
     * Symbolic links may point outside the working directory, so we need to compare the real path
     * of the parent directory (the file itself usually doesn't exist yet).
     */
    private boolean isInsideRootDir(Path path) {
        var parent = path.getParent();
        if ( parent==null ) { return false; }
        try {
            var realParent = Files.exists(parent) ? parent.toRealPath() : parent;
            return realParent.startsWith(rootDir);
        } catch ( IOException e ) {
            throw new FcliTechnicalException("Unable to resolve directory "+parent, e);
        }
    }
}
