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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.common.action.model.FcliActionValidationException;

class ActionFilePathPolicyTest {
    @TempDir Path tempDir;
    private Path root;
    private Path outside;

    @BeforeEach
    void setUp() throws IOException {
        root = Files.createDirectory(tempDir.resolve("root")).toRealPath();
        Files.createDirectory(root.resolve("sub"));
        outside = Files.createDirectory(tempDir.resolve("outside")).toRealPath();
    }

    @Test
    void acceptsRelativePathInsideRoot() {
        assertEquals(root.resolve("scan.fpr"), restricted().resolve("scan.fpr"));
        assertEquals(root.resolve("sub/scan.fpr"), restricted().resolve("sub/scan.fpr"));
    }

    @Test
    void acceptsNormalizedPathInsideRoot() {
        assertEquals(root.resolve("scan.fpr"), restricted().resolve("sub/../scan.fpr"));
    }

    @Test
    void acceptsAbsolutePathInsideRoot() {
        var path = root.resolve("sub/scan.fpr");
        assertEquals(path, restricted().resolve(path.toString()));
    }

    @Test
    void rejectsParentEscape() {
        assertRejected("../scan.fpr");
        assertRejected("sub/../../scan.fpr");
    }

    @Test
    void rejectsAbsolutePathOutsideRoot() {
        assertRejected(outside.resolve("scan.fpr").toString());
    }

    @Test
    void rejectsSymbolicLinkPointingOutsideRoot() throws IOException {
        var link = root.resolve("link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch ( UnsupportedOperationException | IOException e ) {
            Assumptions.abort("Symbolic links not supported: "+e.getMessage());
        }
        assertRejected("link/scan.fpr");
    }

    @Test
    void allowsAnyPathWhenUnrestricted() {
        var policy = new ActionFilePathPolicy(root, true);
        assertEquals(root.getParent().resolve("scan.fpr"), policy.resolve("../scan.fpr"));
        assertEquals(outside.resolve("scan.fpr"), policy.resolve(outside.resolve("scan.fpr").toString()));
    }

    private ActionFilePathPolicy restricted() {
        return new ActionFilePathPolicy(root, false);
    }

    private void assertRejected(String path) {
        var e = assertThrows(FcliActionValidationException.class, () -> restricted().resolve(path));
        assertTrue(e.getMessage().contains("resolves outside the working directory"), e.getMessage());
        assertTrue(e.getMessage().contains("--allow-unrestricted-file-paths"), e.getMessage());
    }
}
