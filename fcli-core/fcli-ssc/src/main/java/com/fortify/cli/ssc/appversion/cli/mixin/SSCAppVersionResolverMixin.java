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
package com.fortify.cli.ssc.appversion.cli.mixin;

import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionResolverMixin {
    public static abstract class AbstractSSCAppVersionResolverMixin {
        @Getter @Mixin private SSCDelimiterMixin delimiterMixin;
        public abstract String getAppVersionNameOrId();

        public SSCAppVersionDescriptor getAppVersionDescriptor(UnirestInstance unirest, String... fields){
            return SSCAppVersionHelper.getRequiredAppVersion(unirest, getAppVersionNameOrId(), delimiterMixin.getDelimiter(), fields);
        }
        
        public String getAppVersionId(UnirestInstance unirest) {
            return getAppVersionDescriptor(unirest, "id").getVersionId();
        }
    }
    
    public static class RequiredOption extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--appversion"}, required = true, descriptionKey = "fcli.ssc.appversion.resolver.nameOrId")
        @Getter private String appVersionNameOrId;
    }
    
    public static class PositionalParameter extends AbstractSSCAppVersionResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.appversion.resolver.nameOrId")
        @Getter private String appVersionNameOrId;
    }
}
