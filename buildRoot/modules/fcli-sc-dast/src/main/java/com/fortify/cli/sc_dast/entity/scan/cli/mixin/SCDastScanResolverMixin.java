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
package com.fortify.cli.sc_dast.entity.scan.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.sc_dast.entity.scan.helper.SCDastScanDescriptor;
import com.fortify.cli.sc_dast.entity.scan.helper.SCDastScanHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Parameters;

public class SCDastScanResolverMixin {
    public static abstract class AbstractSSCDastScanResolverMixin {
        public abstract String getScanId();

        public SCDastScanDescriptor getScanDescriptor(UnirestInstance unirest){
            return SCDastScanHelper.getScanDescriptor(unirest, getScanId());
        }
        
        public String getScanId(UnirestInstance unirest) {
            return getScanDescriptor(unirest).getId();
        }
    }
    
    public static abstract class AbstractSSCDastMultiScanResolverMixin {
        public abstract String[] getScanIds();

        public SCDastScanDescriptor[] getScanDescriptors(UnirestInstance unirest){
            return Stream.of(getScanIds()).map(id->SCDastScanHelper.getScanDescriptor(unirest, id)).toArray(SCDastScanDescriptor[]::new);
        }
        
        public Collection<JsonNode> getScanDescriptorJsonNodes(UnirestInstance unirest){
            return Stream.of(getScanDescriptors(unirest)).map(SCDastScanDescriptor::asJsonNode).collect(Collectors.toList());
        }
        
        public String[] getScanIds(UnirestInstance unirest) {
            return Stream.of(getScanDescriptors(unirest)).map(SCDastScanDescriptor::getId).toArray(String[]::new);
        }
    }
    
    public static class PositionalParameter extends AbstractSSCDastScanResolverMixin {
        @Parameters(index = "0", arity = "1", paramLabel="scan-id")
        @Getter private String scanId;
    }
    
    public static class PositionalParameterMulti extends AbstractSSCDastMultiScanResolverMixin {
        @Parameters(index = "0", arity = "1..", paramLabel = "scan-id's")
        @Getter private String[] scanIds;
    }
}
