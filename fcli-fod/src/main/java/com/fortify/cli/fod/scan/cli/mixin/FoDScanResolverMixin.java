package com.fortify.cli.fod.scan.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.variable.AbstractPredefinedVariableResolverMixin;
import com.fortify.cli.fod.scan.cli.cmd.FoDScanCommands;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class FoDScanResolverMixin {

    @ReflectiveAccess
    public static abstract class AbstractFoDScanResolverMixin extends AbstractPredefinedVariableResolverMixin {
        @Getter private Class<?> predefinedVariableClass = FoDScanCommands.class;
        public abstract String getScanId();

        public FoDScanDescriptor getScanDescriptor(UnirestInstance unirest) {
            return FoDScanHelper.getScanDescriptor(unirest, resolvePredefinedVariable(getScanId()));
        }

        public Integer getScanId(UnirestInstance unirest) {
            return getScanDescriptor(unirest).getScanId();
        }
    }

    @ReflectiveAccess
    public static abstract class AbstractFoDMultiScanResolverMixin extends AbstractPredefinedVariableResolverMixin {
        @Getter private Class<?> predefinedVariableClass = FoDScanCommands.class;
        public abstract String[] getScanIds();

        public FoDScanDescriptor[] getScanDescriptors(UnirestInstance unirest) {
            return Stream.of(getScanIds()).map(id->FoDScanHelper.getScanDescriptor(unirest, resolvePredefinedVariable(id))).toArray(FoDScanDescriptor[]::new);
        }

        public Collection<JsonNode> getScanDescriptorJsonNodes(UnirestInstance unirest) {
            return Stream.of(getScanDescriptors(unirest)).map(FoDScanDescriptor::asJsonNode).collect(Collectors.toList());
        }

        public String[] getScanIds(UnirestInstance unirest) {
            return Stream.of(getScanDescriptors(unirest)).map(FoDScanDescriptor::getScanId).toArray(String[]::new);
        }
    }

    @ReflectiveAccess
    public static class RequiredOption extends AbstractFoDScanResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Option(names = {"--scan"}, required = true)
        @Getter private String scanId;
    }

    @ReflectiveAccess
    public static class PositionalParameter extends AbstractFoDScanResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Parameters(index = "0", arity = "1", paramLabel="scan-id", descriptionKey = "ScanMixin")
        @Getter private String scanId;
    }

    @ReflectiveAccess
    public static class PositionalParameterMulti extends AbstractFoDMultiScanResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Parameters(index = "0", arity = "1..", paramLabel = "scan-id's", descriptionKey = "ScanMixin")
        @Getter private String[] scanIds;
    }

}
