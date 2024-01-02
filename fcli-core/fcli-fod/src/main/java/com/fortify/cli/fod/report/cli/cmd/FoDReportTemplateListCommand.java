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
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.report.helper.FoDReportTemplateGroupType;
import com.fortify.cli.fod.report.helper.FoDReportTemplateHelper;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-templates", aliases = "lst") @CommandGroup("report-template")
public final class FoDReportTemplateListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer {
    @Getter @Mixin private FoDOutputHelperMixins.Lookup outputHelper;

    @Option(names = {"--group"}, defaultValue = "All")
    FoDReportTemplateGroupType groupType;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(FoDUrls.LOOKUP_ITEMS).queryString("type", FoDReportTemplateHelper.LOOKUP_TYPE);
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDReportTemplateHelper.filterTemplatesOnGroup(record, groupType);
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
