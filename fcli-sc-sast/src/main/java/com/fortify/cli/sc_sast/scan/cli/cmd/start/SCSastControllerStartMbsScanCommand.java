package com.fortify.cli.sc_sast.scan.cli.cmd.start;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerOutputHelperMixins;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerJobType;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = SCSastControllerOutputHelperMixins.StartMbsScan.CMD_NAME)
public class SCSastControllerStartMbsScanCommand extends AbstractSCSastControllerScanStartCommand {
    @Getter @Mixin private SCSastControllerOutputHelperMixins.StartMbsScan outputHelper;
    @Getter private File payloadFile;
    @Getter private String sensorVersion;
    @Getter private String buildId;
    @Getter private final boolean dotNetRequired = false;
    @Getter private final String dotNetVersion = null;
    @Getter private final String scaRuntimeArgs = ""; // TODO Provide options
    @Getter private SCSastControllerJobType jobType = SCSastControllerJobType.SCAN_JOB;
    
    @Parameters(arity = "1", index = "0", paramLabel="PACKAGE-FILE")
    public void setPackageFile(File mbsFile) {
        this.payloadFile = mbsFile;
        setMbsProperties(mbsFile);
    }

    private void setMbsProperties(File mbsFile) {
        try ( FileSystem fs = FileSystems.newFileSystem(mbsFile.toPath(), null) ) {
            Path mbsManifest = fs.getPath("MobileBuildSession.manifest");
            try ( InputStream is = Files.newInputStream(mbsManifest) ) {
                Properties p = new Properties();
                p.load(is);
                this.sensorVersion = p.getProperty("SCAVersion");
                this.buildId = p.getProperty("BuildID");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to determine .NET version (if applicable) from package file");
        }
    }
}
