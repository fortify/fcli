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
import java.nio.file.StandardCopyOption

class RuntimeResourcesHelper {
    private static tempDir = TempDirHelper.create();
    
    static void close() {
        TempDirHelper.rm(tempDir);
    }
    
    static Path extractResource(String resourceFile) {
        Path outputFilePath = tempDir.resolve(resourceFile);
        if ( !Files.exists(outputFilePath) ) {
            getResource(resourceFile).withCloseable {
                outputFilePath.parent.toFile().mkdirs()
                Files.copy(it, outputFilePath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return outputFilePath
    }
    
    private static InputStream getResource(String resourceFile) {
        def cl = RuntimeResourcesHelper.class.classLoader
        def stream = cl.getResourceAsStream(resourceFile)
        if ( stream==null ) {
            stream = cl.getResourceAsStream(resourceFile+"-no-shadow")
        }
        if ( stream==null ) {
            throw new IllegalStateException("${resourceFile} (or ${resourceFile}-no-shadow) referenced in @TestResource not found")
        }
        return stream
    }
}
