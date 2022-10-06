package com.fortify.cli.fod.app.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDAppTypeMixin {
    private static final String PARAM_LABEL = "[Meb|Mobile|Microservice]";

    public enum FoDAppType {Web, Mobile, Microservice}

    @ReflectiveAccess
    public static abstract class AbstractFoDAppTypeMixin {
        public abstract FoDAppType getAppType();
    }

    @ReflectiveAccess
    public static class AppTypeOption extends AbstractFoDAppTypeMixin {
        @Option(names = {"--type", "--app-type"}, required = true, arity = "1", paramLabel = PARAM_LABEL, descriptionKey = "appType")
        @Getter
        private FoDAppType appType;
    }

}
