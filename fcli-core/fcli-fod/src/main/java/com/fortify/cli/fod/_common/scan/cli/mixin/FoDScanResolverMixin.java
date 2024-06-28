/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod._common.scan.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.cli.mixin.IFoDDelimiterMixinAware;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FoDScanResolverMixin {

    public static abstract class AbstractFoDScanResolverMixin implements IFoDDelimiterMixinAware  {
        @Setter private FoDDelimiterMixin delimiterMixin;
        public abstract String getReleaseQualifiedScanOrId();

        public FoDScanDescriptor getScanDescriptor(UnirestInstance unirest) {
            var releaseQualifiedScanOrId = getReleaseQualifiedScanOrId();
            return StringUtils.isBlank(releaseQualifiedScanOrId)
                    ? null
                    : FoDScanHelper.getScanDescriptor(unirest, releaseQualifiedScanOrId, getDelimiter());
        }
        
        public FoDScanDescriptor getScanDescriptor(UnirestInstance unirest, FoDScanType scanType) {
            var result = getScanDescriptor(unirest);
            if ( scanType!=null && !scanType.name().equals(result.getScanType()) ) {
                throw new IllegalArgumentException(String.format("Scan id %s (%s) doesn't match expected scan type %s", result.getScanId(), result.getScanType(), scanType.name()));
            }
            return result;
        }

        public String getScanId(UnirestInstance unirest) {
            return getScanDescriptor(unirest).getScanId();
        }

        public final String getDelimiter() {
            if( delimiterMixin!=null ) {
                return delimiterMixin.getDelimiter();
            }
            return null;
        }

    }

    public static abstract class AbstractFoDMultiScanResolverMixin implements IFoDDelimiterMixinAware {
        @Setter private FoDDelimiterMixin delimiterMixin;

        public abstract String[] getReleaseQualifiedScanOrIds();

        public boolean containsReleaseId() {
            return Arrays.stream(getReleaseQualifiedScanOrIds()).anyMatch((e) -> e.contains(getDelimiter()));
        }

        public FoDScanDescriptor[] getScanDescriptors(UnirestInstance unirest) {
            return Stream.of(getReleaseQualifiedScanOrIds()).map(id -> FoDScanHelper.getScanDescriptor(unirest, id, delimiterMixin.getDelimiter())).toArray(FoDScanDescriptor[]::new);
        }

        public Collection<JsonNode> getScanDescriptorJsonNodes(UnirestInstance unirest) {
            return Stream.of(getScanDescriptors(unirest)).map(FoDScanDescriptor::asJsonNode).collect(Collectors.toList());
        }

        public String[] getScanIds(UnirestInstance unirest) {
            return Stream.of(getScanDescriptors(unirest)).map(FoDScanDescriptor::getScanId).toArray(String[]::new);
        }

        public final String getDelimiter() {
            return delimiterMixin.getDelimiter();
        }

    }

    public static class RequiredOption extends AbstractFoDScanResolverMixin {
        @EnvSuffix("SCAN") @Option(names = {"--scan"}, required = true)
        @Getter private String releaseQualifiedScanOrId;
    }

    public static class RequiredOptionMulti extends AbstractFoDMultiScanResolverMixin {
        @EnvSuffix("SCANS") @Option(names = {"--scans"}, required=true, split=",", descriptionKey = "fcli.fod.scan.scan-ids")
        @Getter private String[] releaseQualifiedScanOrIds;
    }

    public static class PositionalParameter extends AbstractFoDScanResolverMixin {
        @EnvSuffix("SCAN") @Parameters(index = "0", arity = "1", paramLabel="scan-id", descriptionKey = "fcli.fod.scan.scan-id")
        @Getter private String releaseQualifiedScanOrId;
    }

    public static class PositionalParameterMulti extends AbstractFoDMultiScanResolverMixin {
        @EnvSuffix("SCANS") @Parameters(index = "0", arity = "1..", paramLabel = "scan-id's", descriptionKey = "fcli.fod.scan.scan-ids")
        @Getter private String[] releaseQualifiedScanOrIds;
    }

}
