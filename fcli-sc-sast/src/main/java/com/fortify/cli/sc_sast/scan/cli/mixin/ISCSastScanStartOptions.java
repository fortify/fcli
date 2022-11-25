package com.fortify.cli.sc_sast.scan.cli.mixin;

import java.io.File;

import com.fortify.cli.sc_sast.scan.helper.SCSastControllerJobType;

public interface ISCSastScanStartOptions {
    String getBuildId();
    String getScaRuntimeArgs();
    boolean isDotNetRequired();
    String getDotNetVersion();
    File getPayloadFile();
    String getSensorVersion();
    SCSastControllerJobType getJobType();
}
