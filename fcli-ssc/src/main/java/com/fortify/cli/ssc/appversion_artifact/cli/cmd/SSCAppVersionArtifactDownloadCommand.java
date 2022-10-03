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

import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "download")
public class SSCAppVersionArtifactDownloadCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @CommandLine.Option(names = {"-f", "--dest"}, descriptionKey = "download.destination")
    private String destination;
    @Mixin private SSCAppVersionResolverMixin.From parentResolver;
    
    @ArgGroup(exclusive=true) private SSCAppVersionArtifactDownloadOptions options = new SSCAppVersionArtifactDownloadOptions();
    
    private static final class SSCAppVersionArtifactDownloadOptions {
        // When downloading an artifact by id, the --no-include-sources option is not applicable, and vice versa 
        @Option(names = "--no-include-sources", negatable = true) private boolean includeSources = true;
        @Option(names="--id") private String artifactId; //TODO Should this be an option or optional positional parameter?
    }

    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        SSCAppVersionDescriptor av = parentResolver.getAppVersionDescriptor(unirest);
        destination = destination != null ? destination : String.format("./%s_%s.fpr", av.getApplicationName(), av.getVersionName());
        if ( StringUtils.isNotBlank(options.artifactId) ) {
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), options.includeSources),
                    destination,
                    ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);
        } else {
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_ARTIFACT(options.artifactId),
                    destination,
                    ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);
        }

        return null;
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputConfigHelper.table();
                //.defaultColumns("id#$[*].scans[*].type:type#lastScanDate#uploadDate#status");
    }
}
