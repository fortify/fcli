package com.fortify.cli.fod.app.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FoDCriticalityTypeOptions {
    public enum FoDCriticalityType {High, Medium, Low}

    @ReflectiveAccess
    public static final class FoDCriticalityTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDCriticalityTypeIterable() {
            super(Stream.of(FoDCriticalityType.values()).map(FoDCriticalityType::name).collect(Collectors.toList()));
        }
    }

    @ReflectiveAccess
    public static abstract class AbstractFoDCriticalityType {
        public abstract FoDCriticalityType getCriticalityType();
    }

    @ReflectiveAccess
    public static class RequiredCritOption extends AbstractFoDCriticalityType {
        @Option(names = {"--criticality", "--business-criticality"}, required = true, arity = "1", completionCandidates = FoDCriticalityTypeIterable.class, descriptionKey = "critType")
        @Getter private FoDCriticalityType criticalityType;
    }

    @ReflectiveAccess
    public static class OptionalCritOption extends AbstractFoDCriticalityType {
        @Option(names = {"--criticality", "--business-criticality"}, required = false, arity = "1", completionCandidates = FoDCriticalityTypeIterable.class, descriptionKey = "critType")
        @Getter private FoDCriticalityType criticalityType;
    }

}
