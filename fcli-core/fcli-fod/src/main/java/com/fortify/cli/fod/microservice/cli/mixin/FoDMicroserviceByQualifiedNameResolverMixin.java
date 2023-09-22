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

package com.fortify.cli.fod.microservice.cli.mixin;

import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.cli.mixin.IFoDDelimiterMixinAware;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceHelper;
import com.fortify.cli.fod.microservice.helper.FoDQualifiedMicroserviceNameDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// Note that FoD doesn't provide endpoints for resolving microservices by id only
// (you can only resolve microservice by id if you know the app id as well), hence
// there is no corresponding FoDMicroserviceByQualifiedNameOrIdResolverMixin.
public class FoDMicroserviceByQualifiedNameResolverMixin {

    public static abstract class AbstractFoDAppAndMicroserviceNameResolverMixin implements IFoDDelimiterMixinAware {
        @Setter private FoDDelimiterMixin delimiterMixin;
        public abstract String getQualifiedMicroserviceName();

        public final FoDQualifiedMicroserviceNameDescriptor getQualifiedMicroserviceNameDescriptor() {
            if (getQualifiedMicroserviceName() == null) { return null; }
            return FoDQualifiedMicroserviceNameDescriptor.fromCombinedAppAndMicroserviceName(getQualifiedMicroserviceName(), delimiterMixin.getDelimiter());
        }
        
        public String getSimpleMicroserviceName() {
            var desc = getQualifiedMicroserviceNameDescriptor();
            return desc==null ? null : desc.getMicroserviceName();
        }
        
        public FoDAppDescriptor getAppDescriptor(UnirestInstance unirest, boolean failIfNotFound) {
            var desc = getQualifiedMicroserviceNameDescriptor();
            if (desc == null || desc.getAppName()==null) { return null; }
            return FoDAppHelper.getAppDescriptor(unirest, desc.getAppName(), failIfNotFound);
        }
        
        public FoDMicroserviceDescriptor getMicroserviceDescriptor(UnirestInstance unirest, boolean failIfNotFound) {
            var desc = getQualifiedMicroserviceNameDescriptor();
            if (desc == null || desc.getAppName()==null || desc.getMicroserviceName()==null) { return null; }
            return FoDMicroserviceHelper.getMicroserviceDescriptor(unirest, desc, failIfNotFound);
        }
    }

    public static class RequiredOption extends AbstractFoDAppAndMicroserviceNameResolverMixin {
        @Option(names = {"--microservice"}, required = true, paramLabel="app:ms", descriptionKey = "fcli.fod.microservice.resolver.name")
        @Getter private String qualifiedMicroserviceName;
    }

    public static class PositionalParameter extends AbstractFoDAppAndMicroserviceNameResolverMixin {
        @EnvSuffix("MICROSERVICE") @Parameters(index = "0", paramLabel="app:ms", descriptionKey = "fcli.fod.microservice.resolver.name")
        @Getter private String qualifiedMicroserviceName;
    }
}
