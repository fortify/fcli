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
package com.fortify.cli.ssc.appversion.cli.mixin;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.JsonHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
public class SSCAppVersionResolverMixin {
    private static final String ALIAS = "--appVersion";
    
    public static abstract class AbstractSSCApplicationVersionMixin {
        public abstract String getVersionNameOrId();

        @Option(names = {"--delim"},
                description = "Change the default delimiter character when using options that accepts " +
                "\"application:version\" as an argument or parameter.", defaultValue = ":")
        private String delimiter;

        @SneakyThrows
        public SSCAppVersionDescriptor getApplicationAndVersion(UnirestInstance unirestInstance){
            String versionNameOrId = getVersionNameOrId();
            
            GetRequest request = unirestInstance.get("/api/v1/projectVersions?limit=2&fields=id,name,project");
            
            try {
                int versionId = Integer.parseInt(versionNameOrId);
                request = request.queryString("q", String.format("id:%d", versionId));
            } catch (NumberFormatException nfe) {
                String[] appAndVersionName = versionNameOrId.split(delimiter);
                if ( appAndVersionName.length != 2 ) { 
                    throw new ValidationException("Application version must be specified as either numeric version id, or in the format <application name>"+delimiter+"<version name>"); 
                }
                request = request.queryString("q", String.format("project.name:\"%s\",name:\"%s\"", appAndVersionName[0], appAndVersionName[1]));
            }
            JsonNode versions = request.asObject(ObjectNode.class).getBody().get("data");
            if ( versions.size()==0 ) {
                throw new ValidationException("No application version found for application version name or id: "+versionNameOrId);
            } else if ( versions.size()>1 ) {
                throw new ValidationException("Multiple application versions found for application version name or id: "+versionNameOrId);
            }
            return JsonHelper.getObjectMapper().treeToValue(versions.get(0), SSCAppVersionDescriptor.class);
        }
        public String getApplicationVersionId(UnirestInstance unirestInstance) {
            return getApplicationAndVersion(unirestInstance).getApplicationVersionId();
        }
    }
    
    // get/retrieve/delete/download version <entity> --from
    public static class From extends AbstractSSCApplicationVersionMixin {
        @Option(names = {"--from", ALIAS}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String versionNameOrId;
    }
    
    // create/update version <entity> --for <version>
    public static class For extends AbstractSSCApplicationVersionMixin {
        @Option(names = {"--for", ALIAS}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String versionNameOrId;
    }
    
    // upload version <entity> --to <version>
    public static class To extends AbstractSSCApplicationVersionMixin {
        @Option(names = {"--to", ALIAS}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String versionNameOrId;
    }
    
    // delete|update <versionNameOrId>
    public static class PositionalParameter extends AbstractSSCApplicationVersionMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationVersionMixin")
        @Getter private String versionNameOrId;
    }
}
