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

import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.cli.mixin.IFoDDelimiterMixinAware;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceHelper;
import com.fortify.cli.fod.release.helper.FoDQualifiedReleaseNameDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDQualifiedReleaseNameResolverMixin {
    public static abstract class AbstractFoDQualifiedReleaseNameResolverMixin implements IFoDDelimiterMixinAware {
        @Setter private FoDDelimiterMixin delimiterMixin;
        public abstract String getQualifiedReleaseName();

        public final FoDQualifiedReleaseNameDescriptor getQualifiedReleaseNameDescriptor() {
            if (getQualifiedReleaseName() == null) { return null; }
            return FoDQualifiedReleaseNameDescriptor.fromQualifiedReleaseName(getQualifiedReleaseName(), getDelimiter());
        }
        
        public String getSimpleReleaseName() {
            var desc = getQualifiedReleaseNameDescriptor();
            return desc==null ? null : desc.getReleaseName();
        }
        
        public FoDAppDescriptor getAppDescriptor(UnirestInstance unirest, boolean failIfNotFound) {
            var desc = getQualifiedReleaseNameDescriptor();
            if (desc == null || desc.getAppName()==null) { return null; }
            return FoDAppHelper.getAppDescriptor(unirest, desc.getAppName(), failIfNotFound);
        }
        
        public FoDMicroserviceDescriptor getMicroServiceDescriptor(UnirestInstance unirest, boolean failIfNotFound) {
            var desc = getQualifiedReleaseNameDescriptor();
            if (desc == null || desc.getAppName()==null || desc.getMicroserviceName()==null) { return null; }
            return FoDMicroserviceHelper.getAppMicroserviceDescriptor(unirest, desc.getAppName(),
                    desc.getMicroserviceName(), failIfNotFound);
        }
        
        public FoDReleaseDescriptor getReleaseDescriptor(UnirestInstance unirest, String... fields){
            return FoDReleaseHelper.getRequiredReleaseDescriptor(unirest, getQualifiedReleaseName(), delimiterMixin.getDelimiter(), fields);
        }

        public final String getDelimiter() {
            return delimiterMixin.getDelimiter();
        }
    }

    public static class RequiredOption extends AbstractFoDQualifiedReleaseNameResolverMixin {
        @Option(names = {"--release"}, required = true, paramLabel = "app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.name")
        @Getter private String qualifiedReleaseName;
    }

    public static class PositionalParameter extends AbstractFoDQualifiedReleaseNameResolverMixin {
        @Parameters(index = "0", paramLabel = "app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.name")
        @Getter private String qualifiedReleaseName;
    }
}
