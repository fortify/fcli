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
package com.fortify.cli.sc_dast.entity.scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastBaseRequestOutputCommand;
import com.fortify.cli.sc_dast.rest.query.cli.mixin.SCDastQueryParamsMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SCDastScanListCommand extends AbstractSCDastBaseRequestOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    // TODO Re-implement support for querying on scan type name based on 
    //      SCDastScanStatus.valueOf(scanStatus).getScanStatusType()
    // TODO Re-implement auto-generated queries based on -q / --query option
    // TODO Should we perform validation (whitelist/blacklist) on query parameter
    //      names and values, for example disallowing users from specifying paging
    //      parameters?
    @Mixin private SCDastQueryParamsMixin queryParamsMixin;
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v2/scans/scan-summary-list");
    };
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
