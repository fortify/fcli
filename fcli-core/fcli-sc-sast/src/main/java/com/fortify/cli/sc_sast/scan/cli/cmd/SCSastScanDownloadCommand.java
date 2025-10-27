/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.sc_sast.scan.cli.cmd;

import java.io.File;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.sc_sast._common.output.cli.cmd.AbstractSCSastJsonNodeOutputCommand;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanJobResolverMixin;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Download.CMD_NAME)
public class SCSastScanDownloadCommand extends AbstractSCSastJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Download outputHelper;
    @Mixin private SCSastScanJobResolverMixin.PositionalParameter scanJobResolver;
    @Mixin private CommonOptionMixins.OptionalFile outputFileMixin;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmationMixin;
    @Option(names = {"--type", "-t"}, defaultValue = "fpr") private SCSastJobDownloadType type;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SCSastScanJobDescriptor descriptor = scanJobResolver.getScanJobDescriptor(unirest);
        String jobToken = descriptor.getJobToken();
        File outputFile = getOutputFile(jobToken);
        if ( outputFile.exists() ) {
            requireConfirmationMixin.checkConfirmed(outputFile.toString());
        }
        unirest.get("/rest/v2/job/{jobToken}/{type}")
            .routeParam("jobToken", jobToken)
            .routeParam("type", type.name())
            .asFile(outputFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
            .getBody();
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("jobToken", jobToken)
                .put("type", type.toString())
                .put("file", outputFile.toString());
    }

    @Override
    public String getActionCommandResult() {
        return "DOWNLOADED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private File getOutputFile(String jobToken) {
        var outputFile = outputFileMixin.getFile();
        return outputFile!=null ? outputFile : new File(type.getDefaultFileName(jobToken)); 
    }
    
    @RequiredArgsConstructor
    public static enum SCSastJobDownloadType {
        FPR("%s.fpr"), 
        LOG("%s.log"), 
        SENSOR_LOG("%s-sensor.log"), 
        JOB("%s-job.zip");
        
        private final String defaultFileNameFormat;
        
        public final String getDefaultFileName(String jobToken) {
            return String.format(defaultFileNameFormat, jobToken);
        }
        
        public final String toString() {
            return name().toLowerCase().replace('_', '-');
        }
    }
}
