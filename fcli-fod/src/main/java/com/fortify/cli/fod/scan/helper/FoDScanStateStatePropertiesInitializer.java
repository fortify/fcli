package com.fortify.cli.fod.scan.helper;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;

import jakarta.inject.Singleton;

@Singleton
public class FoDScanStateStatePropertiesInitializer implements IFortifyCLIInitializer {
    @Override
    public void initializeFortifyCLI(FortifyCLIInitializerCommand cmd) {
        System.setProperty("fcli.fod.scan.states", getValuesString(FoDScanStatus.values()));
        System.setProperty("fcli.fod.scan.states.complete", getValuesString(FoDScanStatus.getDefaultCompleteStates()));
    }

    private String getValuesString(Enum<?>[] values) {
        return Stream.of(values).map(Enum::name).collect(Collectors.joining(", "));
    }
}
