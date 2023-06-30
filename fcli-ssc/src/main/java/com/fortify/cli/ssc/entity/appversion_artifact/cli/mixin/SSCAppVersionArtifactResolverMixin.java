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
package com.fortify.cli.ssc.entity.appversion_artifact.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.ssc.entity.appversion_artifact.helper.SSCAppVersionArtifactDescriptor;
import com.fortify.cli.ssc.entity.appversion_artifact.helper.SSCAppVersionArtifactHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionArtifactResolverMixin {
    public static abstract class AbstractSSCAppVersionArtifactResolverMixin {
        public abstract String getArtifactId();

        public SSCAppVersionArtifactDescriptor getArtifactDescriptor(UnirestInstance unirest){
            return SSCAppVersionArtifactHelper.getArtifactDescriptor(unirest, getArtifactId());
        }
        
        public String getArtifactId(UnirestInstance unirest) {
            return getArtifactDescriptor(unirest).getId();
        }
    }
    
    public static abstract class AbstractSSCAppVersionMultiArtifactResolverMixin {
        public abstract String[] getArtifactIds();

        public SSCAppVersionArtifactDescriptor[] getArtifactDescriptors(UnirestInstance unirest){
            return Stream.of(getArtifactIds()).map(id->SSCAppVersionArtifactHelper.getArtifactDescriptor(unirest, id)).toArray(SSCAppVersionArtifactDescriptor[]::new);
        }
        
        public Collection<JsonNode> getArtifactDescriptorJsonNodes(UnirestInstance unirest){
            return Stream.of(getArtifactDescriptors(unirest)).map(SSCAppVersionArtifactDescriptor::asJsonNode).collect(Collectors.toList());
        }
        
        public String[] getArtifactIds(UnirestInstance unirest) {
            return Stream.of(getArtifactDescriptors(unirest)).map(SSCAppVersionArtifactDescriptor::getId).toArray(String[]::new);
        }
    }
    
    public static class RequiredOption extends AbstractSSCAppVersionArtifactResolverMixin {
        @Option(names = {"--artifact"}, required = true)
        @Getter private String artifactId;
    }
    
    public static class PositionalParameter extends AbstractSSCAppVersionArtifactResolverMixin {
        @Parameters(index = "0", arity = "1", paramLabel="artifact-id")
        @Getter private String artifactId;
    }
    
    public static class PositionalParameterMulti extends AbstractSSCAppVersionMultiArtifactResolverMixin {
        @Parameters(index = "0", arity = "1..", paramLabel = "artifact-id's")
        @Getter private String[] artifactIds;
    }
}
