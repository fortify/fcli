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
package com.fortify.cli.sc_sast.entity.scan.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobDescriptor;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobHelper;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobHelper.StatusEndpointVersion;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SCSastScanJobResolverMixin {
    public static abstract class AbstractSCSastScanJobResolverMixin {
        protected abstract String getNonResolvedScanJobToken();
        
        public SCSastControllerScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest, StatusEndpointVersion minStatusEndpointVersion) {
            return SCSastControllerScanJobHelper.getScanJobDescriptor(unirest, getNonResolvedScanJobToken(), minStatusEndpointVersion);
        }

        public SCSastControllerScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest) {
            return getScanJobDescriptor(unirest, null);
        }
    }
    
    public static abstract class AbstractSCSastMultiScanJobResolverMixin {
        protected abstract String[] getNonResolvedScanJobTokens();
        
        public SCSastControllerScanJobDescriptor[] getScanJobDescriptors(UnirestInstance unirest, StatusEndpointVersion minStatusEndpointVersion) {
            return Stream.of(getNonResolvedScanJobTokens()).map(id->SCSastControllerScanJobHelper.getScanJobDescriptor(unirest, id, minStatusEndpointVersion)).toArray(SCSastControllerScanJobDescriptor[]::new);
        }

        public SCSastControllerScanJobDescriptor[] getScanJobDescriptors(UnirestInstance unirest) {
            return getScanJobDescriptors(unirest, null);
        }
        
        public Collection<JsonNode> getScanJobDescriptorJsonNodes(UnirestInstance unirest, StatusEndpointVersion minStatusEndpointVersion){
            return Stream.of(getScanJobDescriptors(unirest, minStatusEndpointVersion)).map(SCSastControllerScanJobDescriptor::asJsonNode).collect(Collectors.toList());
        }
        
        public Collection<JsonNode> getScanJobDescriptorJsonNodes(UnirestInstance unirest){
            return getScanJobDescriptorJsonNodes(unirest, null);
        }
    }
    
    public static class RequiredOption extends AbstractSCSastScanJobResolverMixin {
        @Option(names = {"--job", "--job-token"}, required = true)
        @Getter private String nonResolvedScanJobToken;
    }
    
    public static class PositionalParameter extends AbstractSCSastScanJobResolverMixin {
        @Parameters(index = "0", arity = "1", paramLabel="scan-job-token")
        @Getter private String nonResolvedScanJobToken;
    }
    
    public static class PositionalParameterMulti extends AbstractSCSastMultiScanJobResolverMixin {
        @Parameters(index = "0", arity = "1..", paramLabel = "scan-job-tokens")
        @Getter private String[] nonResolvedScanJobTokens;
    }
}
