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
package com.fortify.cli.ssc.appversion.cli.mixin;

import javax.validation.ValidationException;

import com.fortify.cli.common.variable.AbstractMinusVariableResolverMixin;
import com.fortify.cli.ssc.appversion.cli.cmd.SSCAppVersionCommands;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class SSCAppVersionResolverMixin {
    @ReflectiveAccess
    public static final class SSCDelimiterMixin {
        @Option(names = {"--delim"},
                description = "Change the default delimiter character when using options that accepts " +
                "\"application:version\" as an argument or parameter.", defaultValue = ":")
        @Getter private String delimiter;
    }
    
    @ReflectiveAccess
    public static final class SSCAppAndVersionNameResolverMixin {
        @Mixin private SSCDelimiterMixin delimiterMixin;
        
        public final SSCAppAndVersionNameDescriptor getAppAndVersionNameDescriptor(String appAndVersionName) {
            if ( appAndVersionName==null ) { return null; }
            String delimiter = delimiterMixin.getDelimiter();
            String[] appAndVersionNameArray = appAndVersionName.split(delimiter);
            if ( appAndVersionNameArray.length != 2 ) { 
                throw new ValidationException("Application and version name must be specified in the format <application name>"+delimiter+"<version name>"); 
            }
            return new SSCAppAndVersionNameDescriptor(appAndVersionNameArray[0], appAndVersionNameArray[1]);
        }
        
        @Data @ReflectiveAccess
        public static final class SSCAppAndVersionNameDescriptor {
            private final String appName, versionName;
        }
    }
    
    @ReflectiveAccess
    public static abstract class AbstractSSCAppVersionResolverMixin extends AbstractMinusVariableResolverMixin {
        @Mixin private SSCDelimiterMixin delimiterMixin;
        public abstract String getAppVersionNameOrId();

        public SSCAppVersionDescriptor getAppVersionDescriptor(UnirestInstance unirest, String... fields){
            return SSCAppVersionHelper.getAppVersion(unirest, resolveMinusVariable(getAppVersionNameOrId()), delimiterMixin.getDelimiter(), fields);
        }
        
        public String getAppVersionId(UnirestInstance unirest) {
            return getAppVersionDescriptor(unirest, "id").getVersionId();
        }
        
        @Override
        protected Class<?> getMVDClass() {
            return SSCAppVersionCommands.class;
        }
    }
    
    @ReflectiveAccess
    public static class RequiredOption extends AbstractSSCAppVersionResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Option(names = {"--appversion"}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
    }
    
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractSSCAppVersionResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationVersionMixin")
        @Getter private String appVersionNameOrId;
    }
}
