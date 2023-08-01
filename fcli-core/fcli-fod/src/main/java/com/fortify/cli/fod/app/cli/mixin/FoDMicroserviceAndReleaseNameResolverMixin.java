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

package com.fortify.cli.fod.app.cli.mixin;

import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod.app.helper.FoDMicroserviceAndReleaseNameDescriptor;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class FoDMicroserviceAndReleaseNameResolverMixin {
    public static abstract class AbstractFoDMicroserviceAndReleaseNameResolverMixin {
        @Getter @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getMicroserviceAndReleaseName();

        public final FoDMicroserviceAndReleaseNameDescriptor getMicroserviceAndReleaseNameDescriptor() {
            if (getMicroserviceAndReleaseName() == null) { return null; }
            return FoDMicroserviceAndReleaseNameDescriptor.fromMicroserviceAndReleaseName(getMicroserviceAndReleaseName(), getDelimiter());
        }

        public final String getDelimiter() {
            return delimiterMixin.getDelimiter();
        }
    }

    public static class RequiredOption extends AbstractFoDMicroserviceAndReleaseNameResolverMixin {
        @Option(names = {"--release"}, required = true, paramLabel = "[ms:]rel", descriptionKey = "fcli.fod.app.release.microservice-and-release-name")
        @Getter private String microserviceAndReleaseName;
    }
}
