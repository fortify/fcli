package com.fortify.cli.fod.app.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDSdlcStatusTypeMixin {
    public enum FoDSdlcStatusType { Development, QA, Production }
    private static final String PARAM_LABEL = "[Development|QA|Production]";
    @ReflectiveAccess
    public static abstract class AbstractFoDSdlcStatusTypeMixin {
        public abstract FoDSdlcStatusType getSdlcStatusType();
    }
    
    @ReflectiveAccess
    public static class SdlcStatusTypeOption extends AbstractFoDSdlcStatusTypeMixin {
        @Option(names = {"--status", "--sdlc-status"}, required = true, arity="1", paramLabel = PARAM_LABEL)
        @Getter private FoDSdlcStatusType SdlcStatusType;
    }

}
