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

package com.fortify.cli.fod.app.cli.mixin;

import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// TODO Change description keys to be more like picocli convention, 
//      for example fcli.fod.app.app-name-or-id or fcli.for.app.resolver 
//      (note that we should do the same in other modules like SSC) 
public class FoDAppResolverMixin {    
    @ReflectiveAccess
    public static abstract class AbstractFoDAppResolverMixin {
        public abstract String getAppNameOrId();

        public FoDAppDescriptor getAppDescriptor(UnirestInstance unirest, String... fields){
            return FoDAppHelper.getAppDescriptor(unirest, getAppNameOrId(), true);
        }

        public String getAppId(UnirestInstance unirest) {
            return getAppDescriptor(unirest, "applicationId").getApplicationId().toString();
        }
    }

    @ReflectiveAccess
    public static class RequiredOption extends AbstractFoDAppResolverMixin {
        @Option(names = {"--app"}, required = true, descriptionKey = "ApplicationMixin")
        @Getter private String appNameOrId;
    }

    @ReflectiveAccess
    public static class OptionalOption extends AbstractFoDAppResolverMixin {
        @Option(names = {"--app"}, required = false, descriptionKey = "ApplicationMixin")
        @Getter private String appNameOrId;
    }

    @ReflectiveAccess
    public static class PositionalParameter extends AbstractFoDAppResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationMixin")
        @Getter private String appNameOrId;
    }
}