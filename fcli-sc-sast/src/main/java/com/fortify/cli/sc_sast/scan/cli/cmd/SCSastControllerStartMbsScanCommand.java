package com.fortify.cli.sc_sast.scan.cli.cmd;

import java.io.File;

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
    @Getter private final boolean dotNetRequired = false;
    @Getter private final String dotNetVersion = null;
    @Getter private final String scaRuntimeArgs = ""; // TODO Provide options
    @Getter private SCSastControllerJobType jobType = SCSastControllerJobType.SCAN_JOB;
    
    @Parameters(arity = "1", index = "0", paramLabel="PACKAGE-FILE")
    public void setPackageFile(File mbsFile) {
        this.payloadFile = mbsFile;
        setMbsProperties(mbsFile);
    }

    private void setMbsProperties(File mbsPath) {
        // TODO Retrieve SCA version from MBS
        // TODO Retrieve build id from MBS?
    }
}
