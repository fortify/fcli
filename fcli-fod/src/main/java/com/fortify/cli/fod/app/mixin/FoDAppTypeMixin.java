package com.fortify.cli.fod.app.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;

public class FoDAppTypeMixin {
    public enum FoDAppType { Web, Mobile, Microservice }
    private static final String PARAM_LABEL = "[Meb|Mobile|Microservice]";
    @ReflectiveAccess
    public static abstract class AbstractFoDAppTypeMixin {
        public abstract FoDAppType getAppType();
    }
    
    @ReflectiveAccess
    public static class AppTypeOption extends AbstractFoDAppTypeMixin {
        @Option(names = {"--type", "--app-type"}, required = true, arity="1", paramLabel = PARAM_LABEL)
        @Getter private FoDAppType appType;
    }

}
