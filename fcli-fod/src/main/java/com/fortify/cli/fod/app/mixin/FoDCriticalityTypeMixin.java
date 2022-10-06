package com.fortify.cli.fod.app.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDCriticalityTypeMixin {
    private static final String PARAM_LABEL = "[High|Medium|Low]";

    public enum FoDCriticalityType {High, Medium, Low}

    @ReflectiveAccess
    public static abstract class AbstractFoDCriticalityTypeMixin {
        public abstract FoDCriticalityType getCriticalityType();
    }

    @ReflectiveAccess
    public static class CriticalityTypeOption extends AbstractFoDCriticalityTypeMixin {
        // TODO: required on add optional on update
        @Option(names = {"--criticality", "--business-criticality"}, required = false, arity = "1", paramLabel = PARAM_LABEL, descriptionKey = "critType")
        @Getter
        private FoDCriticalityType criticalityType;
    }

}
