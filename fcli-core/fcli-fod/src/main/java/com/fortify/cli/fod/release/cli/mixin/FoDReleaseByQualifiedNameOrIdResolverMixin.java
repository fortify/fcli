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

import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.cli.mixin.IFoDDelimiterMixinAware;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDReleaseByQualifiedNameOrIdResolverMixin {
    public static abstract class AbstractFoDQualifiedReleaseNameOrIdResolverMixin implements IFoDDelimiterMixinAware {
        @Setter private FoDDelimiterMixin delimiterMixin;
        public abstract String getQualifiedReleaseNameOrId();

        public FoDReleaseDescriptor getReleaseDescriptor(UnirestInstance unirest, String... fields) {
            var qualifiedReleaseNameOrId = getQualifiedReleaseNameOrId();
            return StringUtils.isBlank(qualifiedReleaseNameOrId)
                    ? null
                    : FoDReleaseHelper.getReleaseDescriptor(unirest, qualifiedReleaseNameOrId, delimiterMixin.getDelimiter(), true, fields);
        }

        public String getReleaseId(UnirestInstance unirest) {
            var descriptor = getReleaseDescriptor(unirest, "releaseId");
            return descriptor==null ? null : descriptor.getReleaseId();
        }
    }

    public static class RequiredOption extends AbstractFoDQualifiedReleaseNameOrIdResolverMixin {
        @Option(names = {"--release"}, required = true, paramLabel = "id|app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.name-or-id")
        @Getter private String qualifiedReleaseNameOrId;
    }

    public static class OptionalOption extends AbstractFoDQualifiedReleaseNameOrIdResolverMixin {
        @Option(names = {"--release"}, required = false, paramLabel = "id|app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.name-or-id")
        @Getter private String qualifiedReleaseNameOrId;
    }

    public static class PositionalParameter extends AbstractFoDQualifiedReleaseNameOrIdResolverMixin {
        @EnvSuffix("RELEASE") @Parameters(index = "0", paramLabel = "id|app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.name-or-id")
        @Getter private String qualifiedReleaseNameOrId;
    }

    public static class OptionalCopyFromOption extends AbstractFoDQualifiedReleaseNameOrIdResolverMixin {
        @Option(names = {"--copy-from"}, required = false, paramLabel = "id|app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.copy-from.nameOrId")
        @Getter private String qualifiedReleaseNameOrId;
    }
}
