package com.fortify.cli.sc_sast.scan.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerOutputHelperMixins;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerJobType;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = SCSastControllerOutputHelperMixins.StartPackageScan.CMD_NAME)
public class SCSastControllerStartPackageScanCommand extends AbstractSCSastControllerScanStartCommand {
    private static final Pattern dotnetFlagFilePattern = Pattern.compile("^dotnet(-(?<version>\\d+\\.\\d+(\\.\\d+)?))?$");
    @Getter @Mixin private SCSastControllerOutputHelperMixins.StartPackageScan outputHelper;
    @Getter private File payloadFile;
    @Getter @Option(names = {"--sensor-version", "-v"}, required = true) private String sensorVersion;
    @Getter private boolean dotNetRequired;
    @Getter private String dotNetVersion;
    @Getter private final String scaRuntimeArgs = "-scan";
    @Getter private SCSastControllerJobType jobType = SCSastControllerJobType.TRANSLATION_AND_SCAN_JOB;
    
    @Parameters(arity = "1", index = "0", paramLabel="PACKAGE-FILE")
    public void setPackagePath(File packageFile) {
        this.payloadFile = packageFile;
        setDotNetProperties(packageFile);
    }
    
    // TODO Clean this up
    private void setDotNetProperties(File packageFile) {
        try (ZipFile zf = new ZipFile(packageFile);) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                Matcher matcher = dotnetFlagFilePattern.matcher(name);
                if (matcher.matches()) {
                    this.dotNetRequired = true;
                    try {
                        this.dotNetVersion = matcher.group("version");
                    } catch (IllegalArgumentException e) {
                        //it is ok, not version is specified
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to determine .NET version (if applicable) from package file");
        }
    }

    
}
