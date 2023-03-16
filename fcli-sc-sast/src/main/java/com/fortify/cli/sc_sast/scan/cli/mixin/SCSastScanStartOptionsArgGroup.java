package com.fortify.cli.sc_sast.scan.cli.mixin;

import picocli.CommandLine.ArgGroup;

public class SCSastScanStartOptionsArgGroup {
    @ArgGroup(exclusive = false, headingKey = "fcli.sc-sast.scan.start.mbs.heading", multiplicity = "1") 
    private SCSastScanStartMbsOptions mbsOptions;
    @ArgGroup(exclusive = false, headingKey = "fcli.sc-sast.scan.start.package.heading", multiplicity = "1") 
    private SCSastScanStartPackageOptions packageOptions;
    
    public ISCSastScanStartOptions getScanStartOptions() {
        if ( mbsOptions!=null && mbsOptions.getPayloadFile()!=null ) {
            return mbsOptions;
        } else if ( packageOptions!=null && packageOptions.getPayloadFile()!=null ) {
            return packageOptions;
        } else {
            throw new IllegalArgumentException("Either package or mbs options need to be specified");
        }
    }
}
