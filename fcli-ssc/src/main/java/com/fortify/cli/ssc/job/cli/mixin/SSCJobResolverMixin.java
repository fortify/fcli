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
package com.fortify.cli.ssc.job.cli.mixin;

import com.fortify.cli.ssc.job.helper.SSCJobDescriptor;
import com.fortify.cli.ssc.job.helper.SSCJobHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCJobResolverMixin {
    private static abstract class AbstractSSCJobResolverMixin {
        public abstract String getJobName();
        
        public SSCJobDescriptor getJobDescriptor(UnirestInstance unirest, String... fields) {
            return SSCJobHelper.getJobDescriptor(unirest, getJobName(), fields);
        }
        
        public String getJobName(UnirestInstance unirest) {
            return getJobDescriptor(unirest, "jobName").getJobName();
        }
    }
    
    public static class RequiredOption extends AbstractSSCJobResolverMixin {
        @Option(names="--job", required = true)
        @Getter private String jobName;
    }
    
    public static class PositionalParameter extends AbstractSSCJobResolverMixin {
        @Parameters(index = "0", arity = "1")
        @Getter private String jobName;
    }
}
