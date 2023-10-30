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
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class SSCSourceAndTargetAppVersionResolverMixin {
    public static abstract class AbstractSSCSourceAndTargetAppVersionResolverMixin {
        @Getter @Mixin private SSCDelimiterMixin delimiterMixin;
        public abstract String getSourceAppVersionNameOrId();
        public abstract String getTargetAppVersionNameOrId();

        public SSCAppVersionDescriptor getSourceAppVersionDescriptor(UnirestInstance unirest, String... fields){
            return SSCAppVersionHelper.getRequiredAppVersion(unirest, getSourceAppVersionNameOrId(), delimiterMixin.getDelimiter(), fields);
        }

        public SSCAppVersionDescriptor getTargetAppVersionDescriptor(UnirestInstance unirest, String... fields){
            return SSCAppVersionHelper.getRequiredAppVersion(unirest, getTargetAppVersionNameOrId(), delimiterMixin.getDelimiter(), fields);
        }

        public String getSourceAppVersionId(UnirestInstance unirest) {
            return getSourceAppVersionDescriptor(unirest, "id").getVersionId();
        }

        public String getTargetAppVersionId(UnirestInstance unirest) {
            return getTargetAppVersionDescriptor(unirest, "id").getVersionId();
        }
    }

    public static class RequiredOption extends AbstractSSCSourceAndTargetAppVersionResolverMixin {
        @Option(names = {"--target-appversion", "--tav"}, required = true, descriptionKey = "fcli.ssc.appversion.resolver.target.nameOrId")
        @Getter private String targetAppVersionNameOrId;

        @Option(names = {"--source-appversion", "--sav"}, required = true, descriptionKey = "fcli.ssc.appversion.resolver.source.nameOrId")
        @Getter private String sourceAppVersionNameOrId;
    }
}
