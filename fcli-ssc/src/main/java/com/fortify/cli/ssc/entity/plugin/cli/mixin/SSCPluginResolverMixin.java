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
package com.fortify.cli.ssc.entity.plugin.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCPluginResolverMixin {
    private static abstract class AbstractSSCPluginResolverMixin {
        public abstract String getNumericPluginId();
    }
    
    public static class RequiredOption extends AbstractSSCPluginResolverMixin {
        @Option(names="--plugin", required = true)
        @Getter private String numericPluginId;
    }
    
    public static class PositionalParameter extends AbstractSSCPluginResolverMixin {
        @Parameters(index = "0", arity = "1")
        @Getter private String numericPluginId;
    }
}
