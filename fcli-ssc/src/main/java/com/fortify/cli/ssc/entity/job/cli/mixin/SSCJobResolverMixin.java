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
package com.fortify.cli.ssc.entity.job.cli.mixin;

import com.fortify.cli.ssc.entity.job.helper.SSCJobDescriptor;
import com.fortify.cli.ssc.entity.job.helper.SSCJobHelper;

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
