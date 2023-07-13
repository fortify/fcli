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

public class FoDRemediationScanPreferenceTypeOptions {
    public static final class FoDRemediationScanPreferenceTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public FoDRemediationScanPreferenceTypeIterable() {
            super(Stream.of(FoDEnums.RemediationScanPreferenceType.values()).map(FoDEnums.RemediationScanPreferenceType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDEntitlementType {
        public abstract FoDEnums.RemediationScanPreferenceType getRemediationScanPreferenceType();
    }

    public static class RequiredOption extends AbstractFoDEntitlementType {
        @Option(names = {"--remediation", "--remediation-preference"}, required = true,
                completionCandidates = FoDRemediationScanPreferenceTypeIterable.class, descriptionKey = "fcli.fod.scan.remediation-preference")
        @Getter
        private FoDEnums.RemediationScanPreferenceType remediationScanPreferenceType;
    }

    public static class OptionalOption extends AbstractFoDEntitlementType {
        @Option(names = {"--remediation", "--remediation-preference"}, required = false,
                completionCandidates = FoDRemediationScanPreferenceTypeIterable.class, descriptionKey = "fcli.fod.scan.remediation-preference")
        @Getter
        private FoDEnums.RemediationScanPreferenceType remediationScanPreferenceType;
    }

}
