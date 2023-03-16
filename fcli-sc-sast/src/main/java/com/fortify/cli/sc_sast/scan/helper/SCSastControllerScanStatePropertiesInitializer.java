package com.fortify.cli.sc_sast.scan.helper;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;

import jakarta.inject.Singleton;

@Singleton
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
