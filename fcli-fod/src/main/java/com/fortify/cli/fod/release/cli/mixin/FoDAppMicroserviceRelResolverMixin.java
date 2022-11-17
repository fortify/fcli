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

package com.fortify.cli.fod.release.cli.mixin;

import com.fortify.cli.common.variable.AbstractPredefinedVariableResolverMixin;
import com.fortify.cli.fod.release.cli.cmd.FoDAppRelCommands;
import com.fortify.cli.fod.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class FoDAppMicroserviceRelResolverMixin {
    @ReflectiveAccess
    public static abstract class AbstractFoDAppMicroserviceRelResolverMixin extends AbstractPredefinedVariableResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getAppMicroserviceRelNameOrId();

        public FoDAppRelDescriptor getAppMicroserviceRelDescriptor(UnirestInstance unirest, String... fields){
            return FoDAppRelHelper.getRequiredAppMicroserviceRel(unirest, resolvePredefinedVariable(getAppMicroserviceRelNameOrId()), delimiterMixin.getDelimiter(), fields);
        }
        
        public String getAppMicroserviceRelId(UnirestInstance unirest) {
            return String.valueOf(getAppMicroserviceRelDescriptor(unirest, "id").getReleaseId());
        }
        
        @Override
        protected Class<?> getPredefinedVariableClass() {
            return FoDAppRelCommands.class;
        }
    }
    
    @ReflectiveAccess
    public static class RequiredOption extends AbstractFoDAppMicroserviceRelResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Option(names = {"--rel", "--release"}, required = true, descriptionKey = "ApplicationMicroserviceReleaseMixin")
        @Getter private String appMicroserviceRelNameOrId;
    }
    
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractFoDAppMicroserviceRelResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationMicroserviceReleaseMixin")
        @Getter private String appMicroserviceRelNameOrId;
    }
}
