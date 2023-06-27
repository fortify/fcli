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
package com.fortify.cli.ssc.entity.appversion_artifact.helper;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;

import jakarta.inject.Singleton;

@Singleton
public class SSCAppVersionArtifactStatePropertiesInitializer implements IFortifyCLIInitializer {
    @Override
    public void initializeFortifyCLI(FortifyCLIInitializerCommand cmd) {
        System.setProperty("fcli.ssc.appversion-artifact.states", getValuesString(SSCAppVersionArtifactStatus.values()));
        System.setProperty("fcli.ssc.appversion-artifact.states.complete", getValuesString(SSCAppVersionArtifactStatus.getDefaultCompleteStates()));
    }

    private String getValuesString(Enum<?>[] values) {
        return Stream.of(values).map(Enum::name).collect(Collectors.joining(", "));
    }
}
