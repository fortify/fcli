package com.fortify.cli.sc_sast.entity.scan.cli.mixin;

import java.io.File;

import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerJobType;

public interface ISCSastScanStartOptions {
    String getBuildId();
    String getScaRuntimeArgs();
    boolean isDotNetRequired();
    String getDotNetVersion();
    File getPayloadFile();
    String getSensorVersion();
    SCSastControllerJobType getJobType();
}
