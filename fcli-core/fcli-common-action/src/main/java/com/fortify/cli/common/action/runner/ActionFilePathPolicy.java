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
import java.nio.file.LinkOption;
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
     * Symbolic links anywhere in the path may point outside the working directory, so we compare real
     * paths: of the file itself if it exists (following a symbolic link at the target, where a dangling
     * link is rejected as its destination can't be verified), otherwise of its nearest existing ancestor
     * directory (the file and possibly some parent directories usually don't exist yet).
     */
    private boolean isInsideRootDir(Path path) {
        try {
            if ( Files.exists(path, LinkOption.NOFOLLOW_LINKS) ) {
                return Files.exists(path) && path.toRealPath().startsWith(rootDir);
            }
            var existingAncestor = path.getParent();
            while ( existingAncestor!=null && !Files.exists(existingAncestor) ) {
                existingAncestor = existingAncestor.getParent();
            }
            return existingAncestor!=null && existingAncestor.toRealPath().startsWith(rootDir);
        } catch ( IOException e ) {
            throw new FcliTechnicalException("Unable to resolve path "+path, e);
        }
    }
}
