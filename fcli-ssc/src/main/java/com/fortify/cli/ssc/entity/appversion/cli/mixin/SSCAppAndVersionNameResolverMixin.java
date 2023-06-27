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
package com.fortify.cli.ssc.entity.appversion.cli.mixin;

import com.fortify.cli.ssc.entity.appversion.helper.SSCAppAndVersionNameDescriptor;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppAndVersionNameResolverMixin {
    
    public static abstract class AbstractSSCAppAndVersionNameResolverMixin {
        @Mixin private SSCDelimiterMixin delimiterMixin;
        public abstract String getAppAndVersionName();
        
        public final SSCAppAndVersionNameDescriptor getAppAndVersionNameDescriptor() {
            if ( getAppAndVersionName()==null ) { return null; }
            return SSCAppAndVersionNameDescriptor.fromCombinedAppAndVersionName(getAppAndVersionName(), getDelimiter());
        }
        
        public final String getDelimiter() {
            return delimiterMixin.getDelimiter();
        }
    }
    
    public static class RequiredOption extends AbstractSSCAppAndVersionNameResolverMixin {
        @Option(names = {"--appversion"}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String appAndVersionName;
    }
    
    public static class PositionalParameter extends AbstractSSCAppAndVersionNameResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationVersionMixin")
        @Getter private String appAndVersionName;
    }
}
