/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper.SSCAppVersionArtifactPurgeByDateRequest;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = "purge")
public class SSCAppVersionArtifactPurgeCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.DAYS, Period.YEARS);
    @Mixin private OutputMixin outputMixin;
    @ArgGroup(exclusive=true, multiplicity="1") private SSCAppVersionArtifactPurgeOptions purgeOptions = new SSCAppVersionArtifactPurgeOptions();
    
    private static final class SSCAppVersionArtifactPurgeOptions {
        @Parameters(arity="1", description = "Id of the artifact to be purged")
        private String artifactId;
        @ArgGroup(exclusive=false) private SSCAppVersionArtifactPurgeByDateOptions purgeByDateOptions;
        
        private static final class SSCAppVersionArtifactPurgeByDateOptions extends SSCAppVersionResolverMixin.From {
            @Option(names = {"--older-than"}, required=true) private String olderThan;            
        }
    }
    
    @Override
    protected Void run(UnirestInstance unirest) {
        JsonNode result;
        if ( purgeOptions.artifactId!=null && !purgeOptions.artifactId.isBlank() ) {
            result = SSCAppVersionArtifactHelper.purge(unirest, purgeOptions.artifactId);
        } else {
            String[] versionIds = {purgeOptions.purgeByDateOptions.getAppVersionDescriptor(unirest).getVersionId()};
            OffsetDateTime dateTime = PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod(purgeOptions.purgeByDateOptions.olderThan);
            SSCAppVersionArtifactPurgeByDateRequest request = SSCAppVersionArtifactPurgeByDateRequest.builder()
                    .projectVersionIds(versionIds)
                    .purgeBefore(dateTime).build();
            result = SSCAppVersionArtifactHelper.purge(unirest, request);
        }
        outputMixin.write(result);
        return null;
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return OutputConfig.table();
    }
}
