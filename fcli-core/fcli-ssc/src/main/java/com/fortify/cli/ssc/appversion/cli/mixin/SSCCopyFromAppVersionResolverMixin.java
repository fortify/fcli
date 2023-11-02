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
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;

public class SSCCopyFromAppVersionResolverMixin {
    public static abstract class AbstractSSCAppVersionResolverMixin {
        public abstract String getAppVersionNameOrId();

        public SSCAppVersionDescriptor getAppVersionDescriptor(UnirestInstance unirest,String delimiter, String... fields){
            return SSCAppVersionHelper.getRequiredAppVersion(unirest, getAppVersionNameOrId(), delimiter, fields);
        }

        public String getAppVersionId(UnirestInstance unirest) {
            return getAppVersionId(unirest, ":");
        }

        public String getAppVersionId(UnirestInstance unirest, String delimiter) {
            return getAppVersionDescriptor(unirest, delimiter).getVersionId();
        }
    }


    public static class RequiredOption extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--copy-from", "--from"}, required = false, descriptionKey = "fcli.ssc.appversion.resolver.copy-from.nameOrId")
        @Getter private String appVersionNameOrId;

    }
}
