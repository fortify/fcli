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

import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionResolverMixin {
    private static final String ALIAS = "--appversion";
    
    @ReflectiveAccess
    public static class SSCAppVersionDelimiterMixin {
        @Option(names = {"--delim"},
                description = "Change the default delimiter character when using options that accepts " +
                "\"application:version\" as an argument or parameter.", defaultValue = ":")
        @Getter private String delimiter;
        
        public final SSCAppAndVersionNameDescriptor getAppAndVersionNameDescriptor(String appAndVersionName) {
            if ( appAndVersionName==null ) { return null; }
            String[] appAndVersionNameArray = appAndVersionName.split(delimiter);
            if ( appAndVersionNameArray.length != 2 ) { 
                throw new ValidationException("Application and version name must be specified in the format <application name>"+delimiter+"<version name>"); 
            }
            return new SSCAppAndVersionNameDescriptor(appAndVersionNameArray[0], appAndVersionNameArray[1]);
        }
        
        @Data
        public static final class SSCAppAndVersionNameDescriptor {
            private final String appName, versionName;
        }
    }
    
    @ReflectiveAccess
    public static abstract class AbstractSSCAppVersionResolverMixin extends SSCAppVersionDelimiterMixin {
        public abstract String getAppVersionNameOrId();

        public SSCAppVersionDescriptor getAppVersionDescriptor(UnirestInstance unirest, String... fields){
            return SSCAppVersionHelper.getAppVersion(unirest, getAppVersionNameOrId(), getDelimiter(), fields);
        }
        
        public String getAppVersionId(UnirestInstance unirest) {
            return getAppVersionDescriptor(unirest, "id").getVersionId();
        }
    }
    
    // get/retrieve/delete/download version <entity> --from
    @ReflectiveAccess
    public static class From extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--from", ALIAS}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
    }
    
    // create/update version <entity> --for <version>
    @ReflectiveAccess
    public static class For extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--for", ALIAS}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
    }
    
    // upload version <entity> --to <version>
    @ReflectiveAccess
    public static class To extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--to", ALIAS}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
    }
    
    // delete|update <versionNameOrId>
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractSSCAppVersionResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
    }
}
