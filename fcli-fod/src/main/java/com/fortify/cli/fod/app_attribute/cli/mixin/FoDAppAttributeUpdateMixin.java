package com.fortify.cli.fod.app_attribute.cli.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;

public class FoDAppAttributeUpdateMixin {
    private static final String PARAM_LABEL = "[ATTR=VALUE]";
    @ReflectiveAccess
    public static abstract class AbstractFoDAppAttributeUpdateMixin {
        public abstract Map<String,String> getAttributes();

    }
    
    @ReflectiveAccess
    public static class OptionalAttrOption extends AbstractFoDAppAttributeUpdateMixin {
        @Option(names = {"--attr", "--attribute"}, required = false, arity="0..", paramLabel = PARAM_LABEL)
        @Getter private Map<String,String> attributes;
    }
    
    @ReflectiveAccess
    public static class RequiredPositionalParameter extends AbstractFoDAppAttributeUpdateMixin {
        @Parameters(index = "0..*", arity = "1..*", paramLabel = PARAM_LABEL)
        @Getter private Map<String,String> attributes;
    }
    
    
}
