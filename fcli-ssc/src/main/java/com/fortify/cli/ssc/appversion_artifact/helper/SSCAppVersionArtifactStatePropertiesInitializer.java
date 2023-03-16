package com.fortify.cli.ssc.appversion_artifact.helper;

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
