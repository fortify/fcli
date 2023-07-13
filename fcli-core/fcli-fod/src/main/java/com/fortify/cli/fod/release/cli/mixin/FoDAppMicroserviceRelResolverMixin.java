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

package com.fortify.cli.fod.release.cli.mixin;

import com.fortify.cli.fod.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAppMicroserviceRelResolverMixin {
    public static abstract class AbstractFoDAppMicroserviceRelResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getAppMicroserviceRelNameOrId();

        public FoDAppRelDescriptor getAppMicroserviceRelDescriptor(UnirestInstance unirest, String... fields){
            return FoDAppRelHelper.getRequiredAppMicroserviceRel(unirest, getAppMicroserviceRelNameOrId(), delimiterMixin.getDelimiter(), fields);
        }

        public String getAppMicroserviceRelId(UnirestInstance unirest) {
            return String.valueOf(getAppMicroserviceRelDescriptor(unirest, "id").getReleaseId());
        }
    }

    public static class RequiredOption extends AbstractFoDAppMicroserviceRelResolverMixin {
        @Option(names = {"--rel", "--release"}, required = true, descriptionKey = "fcli.fod.microservice.release-name-or-id")
        @Getter private String appMicroserviceRelNameOrId;
    }

    public static class PositionalParameter extends AbstractFoDAppMicroserviceRelResolverMixin {
        @Parameters(index = "0", descriptionKey = "fcli.fod.microservice.release-name-or-id")
        @Getter private String appMicroserviceRelNameOrId;
    }
}
