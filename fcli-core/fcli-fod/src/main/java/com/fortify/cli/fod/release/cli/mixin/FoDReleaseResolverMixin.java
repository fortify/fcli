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
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDReleaseResolverMixin {
    public static abstract class AbstractFoDReleaseResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getReleaseNameOrId();

        public FoDReleaseDescriptor getReleaseDescriptor(UnirestInstance unirest, String... fields){
            return FoDReleaseHelper.getRequiredReleaseDescriptor(unirest, getReleaseNameOrId(), delimiterMixin.getDelimiter(), fields);
        }

        public String getReleaseId(UnirestInstance unirest) {
            return String.valueOf(getReleaseDescriptor(unirest, "id").getReleaseId());
        }
    }

    public static class RequiredOption extends AbstractFoDReleaseResolverMixin {
        @Option(names = {"--release"}, required = true, paramLabel = "id|app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.name-or-id")
        @Getter private String releaseNameOrId;
    }
    
    public static class PositionalParameter extends AbstractFoDReleaseResolverMixin {
        @Parameters(index = "0", paramLabel = "id|app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.name-or-id")
        @Getter private String releaseNameOrId;
    }
}
