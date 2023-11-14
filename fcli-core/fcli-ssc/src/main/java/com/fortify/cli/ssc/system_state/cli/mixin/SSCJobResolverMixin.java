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
package com.fortify.cli.ssc.system_state.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.ssc.system_state.helper.SSCJobDescriptor;
import com.fortify.cli.ssc.system_state.helper.SSCJobHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static abstract class AbstractSSCAppVersionMultiJobResolverMixin {
        public abstract String[] getJobNames();

        public SSCJobDescriptor[] getJobDescriptors(UnirestInstance unirest){
            return Stream.of(getJobNames()).map(id-> SSCJobHelper.getJobDescriptor(unirest, id)).toArray(SSCJobDescriptor[]::new);
        }

        public Collection<JsonNode> getJobDescriptorJsonNodes(UnirestInstance unirest){
            return Stream.of(getJobDescriptors(unirest)).map(SSCJobDescriptor::asJsonNode).collect(Collectors.toList());
        }

        public String[] getJobNames(UnirestInstance unirest) {
            return Stream.of(getJobDescriptors(unirest)).map(SSCJobDescriptor::getJobName).toArray(String[]::new);
        }
    }
    
    public static class RequiredOption extends AbstractSSCJobResolverMixin {
        @Option(names="--job", required = true, descriptionKey = "fcli.ssc.system-state.job.resolver.name")
        @Getter private String jobName;
    }
    
    public static class PositionalParameter extends AbstractSSCJobResolverMixin {
        @EnvSuffix("JOB") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.system-state.job.resolver.name")
        @Getter private String jobName;
    }

    public static class PositionalParameterMulti extends  SSCJobResolverMixin.AbstractSSCAppVersionMultiJobResolverMixin {
        @EnvSuffix("JOB") @Parameters(index = "0", arity = "1..", paramLabel = "job-names", descriptionKey = "fcli.ssc.job.resolver.names")
        @Getter private String[] jobNames;
    }
}
