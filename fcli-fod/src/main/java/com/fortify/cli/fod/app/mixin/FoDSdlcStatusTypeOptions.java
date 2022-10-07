package com.fortify.cli.fod.app.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FoDSdlcStatusTypeOptions {
    public enum FoDSdlcStatusType {Development, QA, Production}

    @ReflectiveAccess
    public static final class FoDSdlcStatusTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDSdlcStatusTypeIterable() {
            super(Stream.of(FoDSdlcStatusType.values()).map(FoDSdlcStatusType::name).collect(Collectors.toList()));
        }
    }
    @ReflectiveAccess
    public static abstract class AbstractFoDSdlcStatusType {
        public abstract FoDSdlcStatusType getSdlcStatusType();
    }

    @ReflectiveAccess
    public static class RequiredSdlcOption extends AbstractFoDSdlcStatusType {
        @Option(names = {"--status", "--sdlc-status"}, required = true, arity = "1", completionCandidates = FoDSdlcStatusTypeIterable.class, descriptionKey = "sdlcStatus")
        @Getter private FoDSdlcStatusType SdlcStatusType;
    }

    @ReflectiveAccess
    public static class OptionalSdlcOption extends AbstractFoDSdlcStatusType {
        @Option(names = {"--status", "--sdlc-status"}, required = true, arity = "1", completionCandidates = FoDSdlcStatusTypeIterable.class, descriptionKey = "sdlcStatus")
        @Getter private FoDSdlcStatusType SdlcStatusType;
    }

}
