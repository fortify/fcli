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

package com.fortify.cli.fod.entity.release.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAppAndRelNameResolverMixin {

    public static abstract class AbstractFoDAppAndRelNameResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getAppAndRelName();

        public final FoDAppAndRelNameDescriptor getAppAndRelNameDescriptor() {
            if (getAppAndRelName() == null) { return null; }
            return FoDAppAndRelNameDescriptor.fromCombinedAppAndRelName(getAppAndRelName(), getDelimiter());
        }

        public final String getDelimiter() {
            return delimiterMixin.getDelimiter();
        }
    }

    public static class RequiredOption extends AbstractFoDAppAndRelNameResolverMixin {
        @Option(names = {"--rel", "--release"}, required = true, descriptionKey = "fcli.fod.release.release-name-or-id")
        @Getter private String appAndRelName;
    }

    public static class PositionalParameter extends AbstractFoDAppAndRelNameResolverMixin {
        @Parameters(index = "0", descriptionKey = "fcli.fod.release.release-name-or-id")
        @Getter private String appAndRelName;
    }
}
