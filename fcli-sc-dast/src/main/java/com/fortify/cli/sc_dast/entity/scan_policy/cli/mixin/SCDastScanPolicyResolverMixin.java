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
package com.fortify.cli.sc_dast.entity.scan_policy.cli.mixin;

import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.sc_dast.entity.scan_policy.helper.SCDastScanPolicyDescriptor;
import com.fortify.cli.sc_dast.entity.scan_policy.helper.SCDastScanPolicyHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SCDastScanPolicyResolverMixin {
    
    public static abstract class AbstractSSCDastScanPolicyResolverMixin {
        public abstract String getScanPolicyNameOrId();

        public SCDastScanPolicyDescriptor getScanPolicyDescriptor(UnirestInstance unirest){
            String scanPolicyNameOrId = getScanPolicyNameOrId();
            return StringUtils.isBlank(scanPolicyNameOrId) 
                    ? null
                    : SCDastScanPolicyHelper.getScanPolicyDescriptor(unirest, scanPolicyNameOrId);
        }
        
        public String getScanPolicyId(UnirestInstance unirest) {
            SCDastScanPolicyDescriptor descriptor = getScanPolicyDescriptor(unirest);
            return descriptor==null ? null : descriptor.getId();
        }
    }
    
    public static class RequiredOption extends AbstractSSCDastScanPolicyResolverMixin {
        @Option(names = {"-p", "--policy"}, required = true)
        @Getter private String scanPolicyNameOrId;
    }
    
    public static class OptionalOption extends AbstractSSCDastScanPolicyResolverMixin {
        @Option(names = {"-p", "--policy"}, required = false)
        @Getter private String scanPolicyNameOrId;
    }
    
    public static class PositionalParameter extends AbstractSSCDastScanPolicyResolverMixin {
        @Parameters(index = "0", arity = "1")
        @Getter private String scanPolicyNameOrId;
    }
}
