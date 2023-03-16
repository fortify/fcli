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
package com.fortify.cli.ssc.appversion_filterset.cli.mixin;

import com.fortify.cli.ssc.appversion_filterset.helper.SSCAppVersionFilterSetDescriptor;
import com.fortify.cli.ssc.appversion_filterset.helper.SSCAppVersionFilterSetHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionFilterSetResolverMixin {
    private static abstract class AbstractSSCFilterSetResolverMixin {
        public abstract String getFilterSetTitleOrId();
        
        public SSCAppVersionFilterSetDescriptor getFilterSetDescriptor(UnirestInstance unirest, String appVersionId) {
            return new SSCAppVersionFilterSetHelper(unirest, appVersionId).getDescriptorByTitleOrId(getFilterSetTitleOrId(), true);
        }
    }
    
    public static class FilterSetOption extends AbstractSSCFilterSetResolverMixin {
        @Option(names="--filterset")
        @Getter private String filterSetTitleOrId;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCFilterSetResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "filterSetTitleOrId")
        @Getter private String filterSetTitleOrId;
    }
}
