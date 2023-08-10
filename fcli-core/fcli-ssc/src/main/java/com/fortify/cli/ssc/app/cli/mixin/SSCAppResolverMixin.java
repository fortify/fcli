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
package com.fortify.cli.ssc.app.cli.mixin;

import com.fortify.cli.ssc.app.helper.SSCAppDescriptor;
import com.fortify.cli.ssc.app.helper.SSCAppHelper;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppResolverMixin {
    public static abstract class AbstractSSCAppResolverMixin {
        public abstract String getAppNameOrId();

        public SSCAppDescriptor getAppDescriptor(UnirestInstance unirest, String... fields){
            return SSCAppHelper.getApp(unirest, getAppNameOrId(), true, fields);
        }
        
        public String getAppId(UnirestInstance unirest) {
            return getAppDescriptor(unirest, "id").getApplicationId();
        }
    }
    
    public static class RequiredOption extends AbstractSSCAppResolverMixin {
        @Option(names = {"--app"}, required = true, descriptionKey = "fcli.ssc.app.resolver.nameOrId")
        @Getter private String appNameOrId;
    }
    
    // delete|update <app>
    public static class PositionalParameter extends AbstractSSCAppResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.app.resolver.nameOrId")
        @Getter private String appNameOrId;
    }
}
