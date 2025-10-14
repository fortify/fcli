/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.sc_sast.scan.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobDescriptor;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobHelper;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobHelper.StatusEndpointVersion;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Parameters;

public class SCSastScanJobResolverMixin {
    public static abstract class AbstractSCSastScanJobResolverMixin {
        protected abstract String getScanJobToken();
        
        public SCSastScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest, StatusEndpointVersion minStatusEndpointVersion) {
            return SCSastScanJobHelper.getScanJobDescriptor(unirest, getScanJobToken(), minStatusEndpointVersion);
        }

        public SCSastScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest) {
            return getScanJobDescriptor(unirest, null);
        }
    }
    
    public static abstract class AbstractSCSastMultiScanJobResolverMixin {
        protected abstract String[] getScanJobTokens();
        
        public SCSastScanJobDescriptor[] getScanJobDescriptors(UnirestInstance unirest, StatusEndpointVersion minStatusEndpointVersion) {
            return Stream.of(getScanJobTokens()).map(id->SCSastScanJobHelper.getScanJobDescriptor(unirest, id, minStatusEndpointVersion)).toArray(SCSastScanJobDescriptor[]::new);
        }

        public SCSastScanJobDescriptor[] getScanJobDescriptors(UnirestInstance unirest) {
            return getScanJobDescriptors(unirest, null);
        }
        
        public Collection<JsonNode> getScanJobDescriptorJsonNodes(UnirestInstance unirest, StatusEndpointVersion minStatusEndpointVersion){
            return Stream.of(getScanJobDescriptors(unirest, minStatusEndpointVersion)).map(SCSastScanJobDescriptor::asJsonNode).collect(Collectors.toList());
        }
        
        public Collection<JsonNode> getScanJobDescriptorJsonNodes(UnirestInstance unirest){
            return getScanJobDescriptorJsonNodes(unirest, null);
        }
    }
    
    public static class PositionalParameter extends AbstractSCSastScanJobResolverMixin {
        @EnvSuffix("JOB_TOKEN") @Parameters(index = "0", arity = "1", paramLabel="scan-job-token", descriptionKey = "fcli.sc-sast.scan-job.resolver.jobToken")
        @Getter private String scanJobToken;
    }
    
    public static class PositionalParameterMulti extends AbstractSCSastMultiScanJobResolverMixin {
        @EnvSuffix("JOB_TOKENS") @Parameters(index = "0", arity = "1..", paramLabel = "scan-job-tokens", descriptionKey = "fcli.sc-sast.scan-job.resolver.jobToken")
        @Getter private String[] scanJobTokens;
    }
}
