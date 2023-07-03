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

package com.fortify.cli.fod.entity.microservice.cli.mixin;

import com.fortify.cli.fod.entity.microservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.entity.microservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.entity.release.cli.mixin.FoDDelimiterMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAppMicroserviceResolverMixin {
    public static abstract class AbstractFoDAppMicroserviceResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getAppMicroserviceNameOrId();

        public FoDAppMicroserviceDescriptor getAppMicroserviceDescriptor(UnirestInstance unirest, String... fields){
            return FoDAppMicroserviceHelper.getRequiredAppMicroservice(unirest, getAppMicroserviceNameOrId(), delimiterMixin.getDelimiter(), fields);
        }

        public String getAppMicroserviceId(UnirestInstance unirest) {
            return String.valueOf(getAppMicroserviceDescriptor(unirest, "id").getReleaseId());
        }
    }

    public static class RequiredOption extends AbstractFoDAppMicroserviceResolverMixin {
        @Option(names = {"--microservice"}, required = true, descriptionKey = "fcli.fod.microservice.microservice-name-or-id")
        @Getter private String appMicroserviceNameOrId;
    }

    public static class PositionalParameter extends AbstractFoDAppMicroserviceResolverMixin {
        @Parameters(index = "0", descriptionKey = "fcli.fod.microservice.microservice-name-or-id")
        @Getter private String AppMicroserviceNameOrId;
    }
}