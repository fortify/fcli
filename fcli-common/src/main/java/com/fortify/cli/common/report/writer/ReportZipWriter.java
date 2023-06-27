/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.report.writer;

import java.io.BufferedWriter;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import com.fortify.cli.common.output.writer.IMessageResolver;

import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * Utility class for writing report files to a zip file.
 * @author rsenden
 *
 */
@Accessors(fluent = true)
public class ReportZipWriter extends AbstractReportWriter {
    private FileSystem fileSystem;
    
    public ReportZipWriter(String zipFileName, IMessageResolver messageResolver) {
        super(zipFileName, messageResolver);
    }
    
    @Override @SneakyThrows
    protected Path entryPath(String fileName) {
        var parentPath = Path.of(fileName).getParent();
        if (parentPath!=null ) {
            Files.createDirectories(fileSystem.getPath(parentPath.toString()));
        }
        return fileSystem().getPath(fileName);
    }
    
    @Override @SneakyThrows
    protected BufferedWriter newBufferedWriter(String fileName) {
        return Files.newBufferedWriter(entryPath(fileName));
    }
    
    @Override @SneakyThrows
    protected void closeReport() {
        fileSystem().close();
    }
    
    protected FileSystem fileSystem() {
        if ( fileSystem==null ) {
            fileSystem = createFileSystem();
        }
        return fileSystem;
    }
    
    @SneakyThrows
    protected FileSystem createFileSystem() {
        var env = Collections.singletonMap("create", "true");
        var uri = URI.create("jar:"+absoluteOutputPath().toUri().toString());
        return FileSystems.newFileSystem(uri, env);
    }
}
