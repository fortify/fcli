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
package com.fortify.cli.sc_sast.entity.scan.cli.mixin;

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
