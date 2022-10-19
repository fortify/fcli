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

import com.fortify.cli.ssc.appversion.helper.SSCAppAndVersionNameDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppAndVersionNameResolverMixin {
    
    @ReflectiveAccess
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
    
    @ReflectiveAccess
    public static class RequiredOption extends AbstractSSCAppAndVersionNameResolverMixin {
        @Option(names = {"--appversion"}, required = true, descriptionKey = "ApplicationVersionMixin")
        @Getter private String appAndVersionName;
    }
    
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractSSCAppAndVersionNameResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationVersionMixin")
        @Getter private String appAndVersionName;
    }
}
