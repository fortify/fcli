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
package com.fortify.cli.ssc.artifact.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Parameters;

public class SSCArtifactResolverMixin {
    public static abstract class AbstractSSCAppVersionArtifactResolverMixin {
        public abstract String getArtifactId();

        public SSCArtifactDescriptor getArtifactDescriptor(UnirestInstance unirest){
            return SSCArtifactHelper.getArtifactDescriptor(unirest, getArtifactId());
        }
        
        public String getArtifactId(UnirestInstance unirest) {
            return getArtifactDescriptor(unirest).getId();
        }
    }
    
    public static abstract class AbstractSSCAppVersionMultiArtifactResolverMixin {
        public abstract String[] getArtifactIds();

        public SSCArtifactDescriptor[] getArtifactDescriptors(UnirestInstance unirest){
            return Stream.of(getArtifactIds()).map(id->SSCArtifactHelper.getArtifactDescriptor(unirest, id)).toArray(SSCArtifactDescriptor[]::new);
        }
        
        public Collection<JsonNode> getArtifactDescriptorJsonNodes(UnirestInstance unirest){
            return Stream.of(getArtifactDescriptors(unirest)).map(SSCArtifactDescriptor::asJsonNode).collect(Collectors.toList());
        }
        
        public String[] getArtifactIds(UnirestInstance unirest) {
            return Stream.of(getArtifactDescriptors(unirest)).map(SSCArtifactDescriptor::getId).toArray(String[]::new);
        }
    }
    
    public static class PositionalParameter extends AbstractSSCAppVersionArtifactResolverMixin {
        @EnvSuffix("ARTIFACT") @Parameters(index = "0", arity = "1", paramLabel="artifact-id", descriptionKey = "fcli.ssc.artifact.resolver.id")
        @Getter private String artifactId;
    }
    
    public static class PositionalParameterMulti extends AbstractSSCAppVersionMultiArtifactResolverMixin {
        @EnvSuffix("ARTIFACTS") @Parameters(index = "0", arity = "1..", paramLabel = "artifact-id's", descriptionKey = "fcli.ssc.artifact.resolver.ids")
        @Getter private String[] artifactIds;
    }
}
