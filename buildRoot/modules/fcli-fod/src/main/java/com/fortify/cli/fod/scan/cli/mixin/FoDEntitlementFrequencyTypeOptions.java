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

package com.fortify.cli.fod.scan.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.fod._common.util.FoDEnums;

import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDEntitlementFrequencyTypeOptions {
    public static final class FoDEntitlementFrequencyTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public FoDEntitlementFrequencyTypeIterable() {
            super(Stream.of(FoDEnums.EntitlementFrequencyType.values()).map(FoDEnums.EntitlementFrequencyType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDEntitlementFrequencyType {
        public abstract FoDEnums.EntitlementFrequencyType getEntitlementFrequencyType();
    }

    public static class RequiredOption extends AbstractFoDEntitlementFrequencyType {
        @Option(names = {"--frequency", "--entitlement-frequency"}, required = true,
                completionCandidates = FoDEntitlementFrequencyTypeIterable.class, descriptionKey = "fcli.fod.scan.entitlement-frequency")
        @Getter
        private FoDEnums.EntitlementFrequencyType entitlementFrequencyType;
    }

    public static class OptionalOption extends AbstractFoDEntitlementFrequencyType {
        @Option(names = {"--frequency", "--entitlement-frequency"}, required = false,
                completionCandidates = FoDEntitlementFrequencyTypeIterable.class, descriptionKey = "fcli.fod.scan.entitlement-frequency")
        @Getter
        private FoDEnums.EntitlementFrequencyType entitlementFrequencyType;
    }

}
