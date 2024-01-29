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
package com.fortify.cli.fod.report.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.report.cli.mixin.FoDReportResolverMixin;
import com.fortify.cli.fod.report.helper.FoDReportDescriptor;
import com.fortify.cli.fod.report.helper.FoDReportHelper;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.nio.file.StandardCopyOption;

@Command(name = OutputHelperMixins.Download.CMD_NAME)
public class FoDReportDownloadCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Download outputHelper;
    @Mixin private FoDReportResolverMixin.PositionalParameter reportResolver;

    @Mixin private CommonOptionMixins.RequiredFile outputFileMixin;

    @Override @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDReportDescriptor reportDescriptor = FoDReportHelper.getReportDescriptor(unirest, reportResolver.getReportId());
        var file = outputFileMixin.getFile().getAbsolutePath();
        GetRequest request = unirest.get(FoDUrls.REPORT + "/download")
                .routeParam("reportId", reportResolver.getReportId())
                .accept("application/octet-stream");
        int status = 202;
        while ( status==202 ) {
            status = request
                    .asFile(file, StandardCopyOption.REPLACE_EXISTING)
                    .getStatus();
            if ( status==202 ) { Thread.sleep(30000L); }
        }
        return reportDescriptor.asObjectNode().put("file", file);
    }

    @Override
    public String getActionCommandResult() {
        return "REPORT_DOWNLOADED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
