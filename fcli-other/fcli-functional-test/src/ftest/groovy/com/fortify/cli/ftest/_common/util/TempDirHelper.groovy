/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.ftest._common.util

import java.nio.file.Files
import java.nio.file.Path

class TempDirHelper {
    static Path create() {
        Files.createTempDirectory("fcli").toAbsolutePath()
    }
    static void rm(Path tempDir) {
        try {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path.&toFile)
                .forEach(File.&delete); // For some reason this throws an exception on the
                                        // top-level directory, but the full directory tree
                                        // is being deleted anyway. As such, we just swallow
                                        // any exceptions, and print an error if the directory
                                        // still exists afterwards.
        } catch ( IOException e ) {}
        if ( tempDir.toFile().exists() ) {
            println "Error deleting directory "+tempDir+", please clean up manually";
        }
    }
}
