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
package com.fortify.cli.fod.app.cli.cmd;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fod._common.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-scans", aliases = "lss") @CommandGroup("scan")
public class FoDAppScanListCommand extends AbstractFoDBaseRequestOutputCommand {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    @Mixin private FoDAppResolverMixin.RequiredOption appResolver;
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return FoDScanHelper.addDefaultScanListParams(unirest.get(FoDUrls.APP_SCANS)
                .routeParam("appId", appResolver.getAppId(unirest)));
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
