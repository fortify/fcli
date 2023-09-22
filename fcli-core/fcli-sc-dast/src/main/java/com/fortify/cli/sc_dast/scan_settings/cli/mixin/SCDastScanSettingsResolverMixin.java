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
package com.fortify.cli.sc_dast.scan_settings.cli.mixin;

import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.sc_dast.scan_settings.helper.SCDastScanSettingsDescriptor;
import com.fortify.cli.sc_dast.scan_settings.helper.SCDastScanSettingsHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SCDastScanSettingsResolverMixin {
    
    public static abstract class AbstractSSCDastScanSettingsResolverMixin {
        public abstract String getScanSettingsCicdTokenOrId();

        public SCDastScanSettingsDescriptor getScanSettingsDescriptor(UnirestInstance unirest){
            return SCDastScanSettingsHelper.getScanSettingsDescriptor(unirest, getScanSettingsCicdTokenOrId());
        }
        
        public String getScanSettingsId(UnirestInstance unirest) {
            return getScanSettingsDescriptor(unirest).getId();
        }
        
        public String getScanSettingsCicdToken(UnirestInstance unirest) {
            return getScanSettingsDescriptor(unirest).getCicdToken();
        }
    }
    
    public static class RequiredOption extends AbstractSSCDastScanSettingsResolverMixin {
        @Option(names = {"-s", "--settings"}, required = true, descriptionKey = "fcli.sc-dast.scan-settings.resolver.cicdTokenOrId")
        @Getter private String scanSettingsCicdTokenOrId;
    }
    
    public static class PositionalParameter extends AbstractSSCDastScanSettingsResolverMixin {
        @EnvSuffix("SETTINGS") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.sc-dast.scan-settings.resolver.cicdTokenOrId")
        @Getter private String scanSettingsCicdTokenOrId;
    }
}
