package com.fortify.cli.fod.app.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FoDAppTypeOptions {
    public enum FoDAppType {
        Web("Web Application"),
        Mobile("Mobile Application"),
        Microservice("Microservice");

        public final String name;

        FoDAppType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public boolean isMicroservice() {
            return (name.equals("Microservice"));
        }
    }

    @ReflectiveAccess
    public static final class FoDAppTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDAppTypeIterable() {
            super(Stream.of(FoDAppType.values()).map(FoDAppType::name).collect(Collectors.toList()));
        }
    }

    @ReflectiveAccess
    public static abstract class AbstractFoDAppType {
        public abstract FoDAppType getAppType();
    }

    @ReflectiveAccess
    public static class RequiredAppTypeOption extends AbstractFoDAppType {
        @Option(names = {"--type", "--app-type"}, required = true, arity = "1", completionCandidates = FoDAppTypeIterable.class, descriptionKey = "appType")
        @Getter private FoDAppType appType;
    }

    @ReflectiveAccess
    public static class OptionalAppTypeOption extends AbstractFoDAppType {
        @Option(names = {"--type", "--app-type"}, required = false, arity = "1", completionCandidates = FoDAppTypeIterable.class, descriptionKey = "appType")
        @Getter private FoDAppType appType;
    }

}
