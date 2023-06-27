/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod.entity.scan.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.fod.util.FoDEnums;

import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDEntitlementPreferenceTypeOptions {
    public static final class FoDEntitlementPreferenceTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public FoDEntitlementPreferenceTypeIterable() {
            super(Stream.of(FoDEnums.EntitlementPreferenceType.values()).map(FoDEnums.EntitlementPreferenceType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDEntitlementPreferenceType {
        public abstract FoDEnums.EntitlementPreferenceType getEntitlementPreferenceType();
    }

    public static class RequiredOption extends AbstractFoDEntitlementPreferenceType {
        @Option(names = {"--entitlement", "--entitlement-preference"}, required = true,
                completionCandidates = FoDEntitlementPreferenceTypeIterable.class, descriptionKey = "fcli.fod.scan.entitlement-preference")
        @Getter
        private FoDEnums.EntitlementPreferenceType entitlementPreferenceType;
    }

    public static class OptionalOption extends AbstractFoDEntitlementPreferenceType {
        @Option(names = {"--entitlement", "--entitlement-preference"}, required = false,
                completionCandidates = FoDEntitlementPreferenceTypeIterable.class, descriptionKey = "fcli.fod.scan.entitlement-preference")
        @Getter
        private FoDEnums.EntitlementPreferenceType entitlementPreferenceType;
    }

}
