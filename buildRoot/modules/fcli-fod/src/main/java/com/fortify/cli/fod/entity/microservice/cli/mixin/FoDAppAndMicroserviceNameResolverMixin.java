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

import com.fortify.cli.fod.entity.release.cli.mixin.FoDDelimiterMixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAppAndMicroserviceNameResolverMixin {

    public static abstract class AbstractFoDAppAndMicroserviceNameResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getAppAndMicroserviceName();

        public final FoDAppAndMicroserviceNameDescriptor getAppAndMicroserviceNameDescriptor() {
            if (getAppAndMicroserviceName() == null) { return null; }
            return FoDAppAndMicroserviceNameDescriptor.fromCombinedAppAndMicroserviceName(getAppAndMicroserviceName(), getDelimiter());
        }

        public final String getDelimiter() {
            return delimiterMixin.getDelimiter();
        }
    }

    public static class RequiredOption extends AbstractFoDAppAndMicroserviceNameResolverMixin {
        @Option(names = {"--microservice"}, required = true, descriptionKey = "fcli.fod.microservice.microservice-name-or-id")
        @Getter private String appAndMicroserviceName;
    }

    public static class PositionalParameter extends AbstractFoDAppAndMicroserviceNameResolverMixin {
        @Parameters(index = "0", descriptionKey = "fcli.fod.microservice.microservice-name-or-id")
        @Getter private String appAndMicroserviceName;
    }
}
