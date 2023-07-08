/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_sast.entity.scan.helper;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;

public class SCSastControllerScanStatePropertiesInitializer implements IFortifyCLIInitializer {
    @Override
    public void initializeFortifyCLI(FortifyCLIInitializerCommand cmd) {
        System.setProperty("fcli.sc-sast.scan.jobStates", getValuesString(SCSastControllerScanJobState.values()));
        System.setProperty("fcli.sc-sast.scan.jobStates.complete", getValuesString(SCSastControllerScanJobState.getDefaultCompleteStates()));
        System.setProperty("fcli.sc-sast.scan.jobArtifactStates", getValuesString(SCSastControllerScanJobArtifactState.values()));
        System.setProperty("fcli.sc-sast.scan.jobArtifactStates.complete", getValuesString(SCSastControllerScanJobArtifactState.getDefaultCompleteStates()));
        
    }

    private String getValuesString(Enum<?>[] values) {
        return Stream.of(values).map(Enum::name).collect(Collectors.joining(", "));
    }
}
