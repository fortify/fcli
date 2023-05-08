package com.fortify.cli.common.report.writer;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fortify.cli.common.output.writer.IMessageResolver;

import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * Utility class for writing report files to a directory.
 * @author rsenden
 */
@Accessors(fluent = true)
public class ReportDirWriter extends AbstractReportWriter {
    public ReportDirWriter(String dirName, IMessageResolver messageResolver) {
        super(dirName, messageResolver);
    }
    
    @Override @SneakyThrows
    protected Path entryPath(String fileName) {
        var path = absoluteOutputPath().resolve(fileName);
        Files.createDirectories(path.getParent());
        return path;
    }
    
    @Override @SneakyThrows
    protected BufferedWriter newBufferedWriter(String fileName) {
        Files.createDirectories(absoluteOutputPath());
        return Files.newBufferedWriter(entryPath(fileName));
    }
    
    @Override
    protected void closeReport() {
        // TODO Noting to do   
    }
}
