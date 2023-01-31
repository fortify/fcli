/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.appversion_artifact.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactDescriptor;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionArtifactResolverMixin {
    @ReflectiveAccess
    public static abstract class AbstractSSCAppVersionArtifactResolverMixin {
        public abstract String getArtifactId();

        public SSCAppVersionArtifactDescriptor getArtifactDescriptor(UnirestInstance unirest){
            return SSCAppVersionArtifactHelper.getArtifactDescriptor(unirest, getArtifactId());
        }
        
        public String getArtifactId(UnirestInstance unirest) {
            return getArtifactDescriptor(unirest).getId();
        }
    }
    
    @ReflectiveAccess
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
    
    @ReflectiveAccess
    public static class RequiredOption extends AbstractSSCAppVersionArtifactResolverMixin {
        @Option(names = {"--artifact"}, required = true)
        @Getter private String artifactId;
    }
    
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractSSCAppVersionArtifactResolverMixin {
        @Parameters(index = "0", arity = "1", paramLabel="artifact-id")
        @Getter private String artifactId;
    }
    
    @ReflectiveAccess
    public static class PositionalParameterMulti extends AbstractSSCAppVersionMultiArtifactResolverMixin {
        @Parameters(index = "0", arity = "1..", paramLabel = "artifact-id's")
        @Getter private String[] artifactIds;
    }
}
