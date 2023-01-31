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

import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionResolverMixin {
    @ReflectiveAccess
    public static abstract class AbstractSSCAppVersionResolverMixin {
        @Mixin private SSCDelimiterMixin delimiterMixin;
        public abstract String getAppVersionNameOrId();

        public SSCAppVersionDescriptor getAppVersionDescriptor(UnirestInstance unirest, String... fields){
            return SSCAppVersionHelper.getRequiredAppVersion(unirest, getAppVersionNameOrId(), delimiterMixin.getDelimiter(), fields);
        }
        
        public String getAppVersionId(UnirestInstance unirest) {
            return getAppVersionDescriptor(unirest, "id").getVersionId();
        }
    }
    
    @ReflectiveAccess
    public static class RequiredOption extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--appversion"}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
    }
    
    @ReflectiveAccess
    public static class OptionalOption extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--appversion"}, required = false, descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
        public final boolean hasValue() { return StringUtils.isNotBlank(appVersionNameOrId); }
    }
    
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractSSCAppVersionResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
    }
}
