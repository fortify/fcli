package com.fortify.cli.sc_sast.scan.cli.mixin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import com.fortify.cli.sc_sast.scan.helper.SCSastControllerJobType;

import lombok.Getter;
import picocli.CommandLine.Option;

public class SCSastScanStartPackageOptions implements ISCSastScanStartOptions {
    private static final Pattern dotnetFlagFilePattern = Pattern.compile("^dotnet(-(?<version>\\d+\\.\\d+(\\.\\d+)?))?$");
    @Getter @Option(names = {"--sensor-version", "-v"}, required = true) private String sensorVersion;
    @Getter private File payloadFile;
    @Getter private final String buildId = null; // TODO ScanCentral Client doesn't allow for specifying build id; should we provide a CLI option for this?
    @Getter private boolean dotNetRequired;
    @Getter private String dotNetVersion;
    @Getter private final String scaRuntimeArgs = "";
    @Getter private SCSastControllerJobType jobType = SCSastControllerJobType.TRANSLATION_AND_SCAN_JOB;
    
    @Option(names = {"-p", "--package-file"}, required = true)
    public void setPackageFile(File packageFile) {
        this.payloadFile = packageFile;
        setDotNetProperties(packageFile);
    }
    
    // TODO Clean this up
    private void setDotNetProperties(File packageFile) {
        try ( FileSystem fs = FileSystems.newFileSystem(packageFile.toPath(), null) ) {
            StreamSupport.stream(fs.getFileStores().spliterator(), false)
            .map(FileStore::name)
            .map(dotnetFlagFilePattern::matcher)
            .filter(Matcher::matches)
            .findFirst()
            .ifPresent(this::setDotNetProperties);  
        } catch (IOException e) {
            throw new IllegalStateException("Unable to determine .NET version (if applicable) from package file");
        }
    }
    
    private void setDotNetProperties(Matcher matcher) {
        this.dotNetRequired = true;
        this.dotNetVersion = matcher.group("version");
    }
}
