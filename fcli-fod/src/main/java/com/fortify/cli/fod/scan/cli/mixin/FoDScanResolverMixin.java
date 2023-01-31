package com.fortify.cli.fod.scan.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDScanResolverMixin {

    @ReflectiveAccess
    public static abstract class AbstractFoDScanResolverMixin {
        public abstract String getScanId();

        public FoDScanDescriptor getScanDescriptor(UnirestInstance unirest) {
            return FoDScanHelper.getScanDescriptor(unirest, getScanId());
        }

        public Integer getScanId(UnirestInstance unirest) {
            return getScanDescriptor(unirest).getScanId();
        }
    }

    @ReflectiveAccess
    public static abstract class AbstractFoDMultiScanResolverMixin {
        public abstract String[] getScanIds();

        public FoDScanDescriptor[] getScanDescriptors(UnirestInstance unirest) {
            return Stream.of(getScanIds()).map(id->FoDScanHelper.getScanDescriptor(unirest, id)).toArray(FoDScanDescriptor[]::new);
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
        @Option(names = {"--scan"}, required = true)
        @Getter private String scanId;
    }

    @ReflectiveAccess
    public static class PositionalParameter extends AbstractFoDScanResolverMixin {
        @Parameters(index = "0", arity = "1", paramLabel="scan-id", descriptionKey = "ScanMixin")
        @Getter private String scanId;
    }

    @ReflectiveAccess
    public static class PositionalParameterMulti extends AbstractFoDMultiScanResolverMixin {
        @Parameters(index = "0", arity = "1..", paramLabel = "scan-id's", descriptionKey = "ScanMixin")
        @Getter private String[] scanIds;
    }

}
