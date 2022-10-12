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

// TODO Review whether we need all the from/for/to mixins
public class FoDAppResolverMixin {
    private static final String ALIAS = "--app";
    @ReflectiveAccess
    public static abstract class AbstractFoDAppResolverMixin {
        public abstract String getAppNameOrId();

        public FoDAppDescriptor getAppDescriptor(UnirestInstance unirest, String... fields){
            //return FoDAppHelper.getApp(unirest, getAppNameOrId(), true, fields);
            return FoDAppHelper.getAppDescriptor(unirest, getAppNameOrId(), true);
        }

        public String getAppId(UnirestInstance unirest) {
            return getAppDescriptor(unirest, "applicationId").getApplicationId().toString();
        }
    }

    // get/retrieve/delete/download app <entity> --from <app>
    @ReflectiveAccess
    public static class From extends AbstractFoDAppResolverMixin {
        @Option(names = {"--from", ALIAS}, required = true)
        @Getter private String appNameOrId;
    }

    // create/update app <entity> --for <app>
    @ReflectiveAccess
    public static class For extends AbstractFoDAppResolverMixin {
        @Option(names = {"--for", ALIAS}, required = true)
        @Getter private String appNameOrId;
    }

    // upload app <entity> --to <app>
    @ReflectiveAccess
    public static class To extends AbstractFoDAppResolverMixin {
        @Option(names = {"--to", ALIAS}, required = true)
        @Getter private String appNameOrId;
    }

    // delete|update <app>
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractFoDAppResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "app")
        @Getter private String appNameOrId;
    }
}