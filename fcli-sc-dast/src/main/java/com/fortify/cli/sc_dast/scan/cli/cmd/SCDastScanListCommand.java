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
package com.fortify.cli.sc_dast.scan.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.common.output.query.OutputQueryHelper;
import com.fortify.cli.common.output.query.OutputQueryOperator;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.sc_dast.output.cli.mixin.SCDastOutputHelperMixins;
import com.fortify.cli.sc_dast.rest.cli.mixin.SCDastSearchTextMixin;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SCDastOutputHelperMixins.List.CMD_NAME)
public class SCDastScanListCommand extends AbstractSCDastScanOutputCommand implements IUnirestBaseRequestSupplier {
    @Getter @Mixin private SCDastOutputHelperMixins.List outputHelper;
    @Mixin SCDastSearchTextMixin searchTextMixin;
    // TODO Once we support date-based less-than/greater-than operators for -q,
    //      deprecate these options and update #updateRequest() to get the appropriate
    //      query values.
    @Option(names = {"-a", "--started-after"}) private String startedAfter;
    @Option(names = {"-b", "--started-before"}) private String startedBefore;
    
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return updateRequest(
             unirest.get("/api/v2/scans/scan-summary-list")
        );
    };
    
    @Override
    public boolean isSingular() {
        return false;
    }

    private HttpRequest<?> updateRequest(HttpRequest<?> request) {
        OutputQueryHelper queryHelper = new OutputQueryHelper(outputHelper);
        request = searchTextMixin.updateRequest(request);
        request = StringUtils.isBlank(startedAfter) 
                ? request
                : request.queryString("startedOnStartDate", startedAfter);
        request = StringUtils.isBlank(startedBefore) 
                ? request
                : request.queryString("startedOnEndDate", startedBefore);
        String scanStatus = queryHelper.getQueryValue("scanStatus", OutputQueryOperator.EQUALS);
        request = StringUtils.isBlank(scanStatus) 
                ? request
                : request.queryString("scanStatusType", SCDastScanStatus.valueOf(scanStatus).getScanStatusType());
        return request;
    }
}
