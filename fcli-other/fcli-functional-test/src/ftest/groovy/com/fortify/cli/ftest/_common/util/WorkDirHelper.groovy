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

import groovy.transform.CompileStatic

@CompileStatic
class WorkDirHelper implements Closeable, AutoCloseable {
    private Path workDir;
    
    @Override
    public void close() {
        removeWorkDir();
    }
    
    public final Path getFortifyDataDir() {
        return resolve(".fortify", PathType.DIR);
    }
    
    public final Path getResource(String name) {
        return extractResource(name);
    }
    
    public final Path getTempDir(String name) {
        return getTempPath(name, PathType.DIR);
    }
    
    public final Path getTempFile(String name) {
        return getTempPath(name, PathType.FILE);
    }
    
    private final Path getTempPath(String name, PathType type) {
        return resolve(Path.of("temp", name), type);
    }
    
    private final enum PathType {
        DIR, FILE
    }
    
    private final Path resolve(String relativePath, PathType type) {
        return resolve(Path.of(relativePath), type);
    }
    
    private final Path resolve(Path relativePath, PathType type) {
        def path = getWorkDir().resolve(relativePath);
        ensureDir(type==PathType.DIR ? path : path.getParent());
        return path;
    }
    
    private final void ensureDir(Path path) {
        if ( !Files.exists(path) ) {
            Files.createDirectories(path);
        }
    }
    
    private final Path getWorkDir() {
        if ( workDir==null ) {
            workDir = Files.createTempDirectory("fcli").toAbsolutePath()
        }
        return workDir;
    }
    
    private Path extractResource(String resourceFile) {
        Path outputFilePath = resolve(resourceFile, PathType.FILE);
        if ( !Files.exists(outputFilePath) ) {
            getResourceInputStream(resourceFile).withCloseable {
                outputFilePath.parent.toFile().mkdirs()
                Files.copy(it, outputFilePath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return outputFilePath
    }
    
    private InputStream getResourceInputStream(String resourceFile) {
        def cl = this.class.classLoader
        def stream = cl.getResourceAsStream(resourceFile)
        if ( stream==null ) {
            stream = cl.getResourceAsStream(resourceFile+"-no-shadow")
        }
        if ( stream==null ) {
            throw new IllegalStateException("${resourceFile} (or ${resourceFile}-no-shadow) referenced in @TestResource not found")
        }
        return stream
    }
    
    private void removeWorkDir() {
        if ( workDir && !System.getProperty("ft.keep-temp-dirs") ) {
            try {
                Files.walk(workDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path.&toFile)
                    .forEach(File.&delete); // For some reason this throws an exception on the
                                            // top-level directory, but the full directory tree
                                            // is being deleted anyway. As such, we just swallow
                                            // any exceptions, and print an error if the directory
                                            // still exists afterwards.
            } catch ( IOException e ) {}
            if ( workDir.toFile().exists() ) {
                println "Error deleting directory "+workDir+", please clean up manually";
            }
        }
    }
}
