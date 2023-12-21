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
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.report.cli.mixin.FoDReportResolverMixin;
import com.fortify.cli.fod.report.helper.FoDReportDescriptor;
import com.fortify.cli.fod.report.helper.FoDReportHelper;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class FoDReportDeleteCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Mixin private FoDReportResolverMixin.PositionalParameter reportResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDReportDescriptor reportDescriptor = FoDReportHelper.getReportDescriptor(unirest, reportResolver.getReportId());
        unirest.delete(FoDUrls.REPORT)
                .routeParam("reportId", reportResolver.getReportId())
                .asObject(JsonNode.class).getBody();
        return reportDescriptor.asObjectNode();
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDReportHelper.transformRecord(record);
    }

    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
