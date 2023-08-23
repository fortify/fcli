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
package com.fortify.cli.fod.entitlement.cli.mixin;

import com.fortify.cli.fod.entitlement.helper.FoDEntitlementDescriptor;
import com.fortify.cli.fod.entitlement.helper.FoDEntitlementHelper;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDEntitlementResolverMixin {

    public static abstract class AbstractFoDEntitlementResolverMixin {
        public abstract String getEntitlementId();

        public FoDEntitlementDescriptor getEntitlementDescriptor(UnirestInstance unirest) {
            return FoDEntitlementHelper.getEntitlementDescriptor(unirest, getEntitlementId(), false);
        }

        public Integer getEntitlementId(UnirestInstance unirest) {
            return getEntitlementDescriptor(unirest).getEntitlementId();
        }
    }

    public static class RequiredOption extends AbstractFoDEntitlementResolverMixin {
        @Option(names = {"--entitlement"}, required = true)
        @Getter private String entitlementId;
    }

    public static class PositionalParameter extends AbstractFoDEntitlementResolverMixin {
        @Parameters(index = "0", arity = "1", paramLabel="entitlement-id", descriptionKey = "fcli.fod.entitlement.entitlement-id")
        @Getter private String entitlementId;
    }

}
