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
