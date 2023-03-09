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

package com.fortify.cli.fod.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.cli.mixin.FoDTimePeriodOptions;
import com.fortify.cli.fod.rest.helper.FoDFilterResultsTransformer;
import com.fortify.cli.fod.rest.query.FoDFilterParamGenerator;
import com.fortify.cli.fod.rest.query.FoDFiltersParamValueGenerators;
import com.fortify.cli.fod.rest.query.IFoDFilterParamGeneratorSupplier;
import com.fortify.cli.fod.scan.cli.mixin.FoDAnalysisStatusTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanFormatOptions;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.List.CMD_NAME)
public class FoDScanListCommand extends AbstractFoDOutputCommand implements IUnirestBaseRequestSupplier, IRecordTransformer, IFoDFilterParamGeneratorSupplier {

    @Getter @Mixin private FoDOutputHelperMixins.List outputHelper;

    @Getter private final FoDFilterParamGenerator filterParamGenerator = new FoDFilterParamGenerator()
            .add("id", "scanId", FoDFiltersParamValueGenerators::plain)
            .add("type", "scanType", FoDFiltersParamValueGenerators::plain);

    @Option(names = {"--latest-first"})
    private Boolean latestFirst;
    @Option(names = {"--started-on-start-date"})
    private String startedOnStartDate;
    @Option(names = {"--started-on-end-date"})
    private String startedOnEndDate;
    @Option(names = {"--completed-on-start-date"})
    private String completedOnStartDate;
    @Option(names = {"--completed-on-end-date"})
    private String completedOnEndDate;
    @Option(names = {"--modified-start-date"})
    private String modifiedStartDate;
    @Mixin private FoDAnalysisStatusTypeOptions.OptionalOption analysisStatus;
    @Mixin private FoDScanFormatOptions.OptionalOption scanType;
    @Mixin private FoDTimePeriodOptions.OptionalOption timePeriod;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return updateRequest(unirest.get(FoDUrls.SCANS));
    }

    private HttpRequest<?> updateRequest(HttpRequest<?> request) {
        request = StringUtils.isBlank(startedOnStartDate)
                ? request
                : request.queryString("startedOnStartDate", startedOnStartDate);
        request = StringUtils.isBlank(startedOnEndDate)
                ? request
                : request.queryString("startedOnEndDate", startedOnEndDate);
        request = StringUtils.isBlank(completedOnStartDate)
                ? request
                : request.queryString("completedOnStartDate", completedOnStartDate);
        request = StringUtils.isBlank(completedOnEndDate)
                ? request
                : request.queryString("completedOnEndDate", completedOnEndDate);
        if (!StringUtils.isBlank(modifiedStartDate)) {
            request = request.queryString("modifiedStartDate", modifiedStartDate);
        } else {
            String modifiedTimePeriod = timePeriod.getTimePeriodType().getDateTime();
            request = request.queryString("modifiedStartDate", modifiedTimePeriod);
        }
        request.queryString("orderBy", "startedDateTime");
        request.queryString("orderByDirection", (latestFirst != null && latestFirst ? "DESC" : "ASC"));
        return request;
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        String aStatusStr = (analysisStatus != null && analysisStatus.getAnalysisStatusType() != null ? String.valueOf(analysisStatus.getAnalysisStatusType()) : "*");
        String sTypeStr = (scanType != null && scanType.getScanType() != null ? String.valueOf(scanType.getScanType()) : "*");
        return new FoDFilterResultsTransformer(new String[]{
                "scanType:" + sTypeStr, "analysisStatusType:" + aStatusStr
        }).transform(FoDScanHelper.renameFields(record));
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
