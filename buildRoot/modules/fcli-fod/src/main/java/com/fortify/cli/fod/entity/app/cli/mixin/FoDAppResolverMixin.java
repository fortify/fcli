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

package com.fortify.cli.fod.entity.app.cli.mixin;

import com.fortify.cli.fod.entity.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.entity.app.helper.FoDAppHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAppResolverMixin {
    public static abstract class AbstractFoDAppResolverMixin {
        public abstract String getAppNameOrId();

        public FoDAppDescriptor getAppDescriptor(UnirestInstance unirest, String... fields){
            return FoDAppHelper.getAppDescriptor(unirest, getAppNameOrId(), true);
        }

        public String getAppId(UnirestInstance unirest) {
            return getAppDescriptor(unirest, "applicationId").getApplicationId().toString();
        }
    }

    public static class RequiredOption extends AbstractFoDAppResolverMixin {
        @Option(names = {"--app"}, required = true, descriptionKey = "fcli.fod.app.app-name-or-id")
        @Getter private String appNameOrId;
    }

    public static class OptionalOption extends AbstractFoDAppResolverMixin {
        @Option(names = {"--app"}, required = false, descriptionKey = "fcli.fod.app.app-name-or-id")
        @Getter private String appNameOrId;
    }

    public static class PositionalParameter extends AbstractFoDAppResolverMixin {
        @Parameters(index = "0", descriptionKey = "fcli.fod.app.app-name-or-id")
        @Getter private String appNameOrId;
    }
}
