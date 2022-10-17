/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.sc_dast.scan_settings.cli.mixin;

import com.fortify.cli.common.variable.AbstractPredefinedVariableResolverMixin;
import com.fortify.cli.sc_dast.scan_settings.cli.cmd.SCDastScanSettingsCommands;
import com.fortify.cli.sc_dast.scan_settings.helper.SCDastScanSettingsDescriptor;
import com.fortify.cli.sc_dast.scan_settings.helper.SCDastScanSettingsHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec.Target;

public class SCDastScanSettingsResolverMixin {
    
    @ReflectiveAccess
    public static abstract class AbstractSSCDastScanSettingsResolverMixin extends AbstractPredefinedVariableResolverMixin {
        public abstract String getScanSettingsCicdTokenOrId();

        public SCDastScanSettingsDescriptor getScanSettingsDescriptor(UnirestInstance unirest){
            return SCDastScanSettingsHelper.getScanSettingsDescriptor(unirest, resolvePredefinedVariable(getScanSettingsCicdTokenOrId()));
        }
        
        public String getScanSettingsId(UnirestInstance unirest) {
            return getScanSettingsDescriptor(unirest).getId();
        }
        
        public String getScanSettingsCicdToken(UnirestInstance unirest) {
            return getScanSettingsDescriptor(unirest).getCicdToken();
        }
        
        @Override
        protected Class<?> getPredefinedVariableClass() {
            return SCDastScanSettingsCommands.class;        
        }
    }
    
    @ReflectiveAccess
    public static class RequiredOption extends AbstractSSCDastScanSettingsResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Option(names = {"-S", "--settings"}, required = true)
        @Getter private String scanSettingsCicdTokenOrId;
    }
    
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractSSCDastScanSettingsResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Parameters(index = "0", arity = "1")
        @Getter private String scanSettingsCicdTokenOrId;
    }
}
