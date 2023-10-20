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
package com.fortify.cli.ssc.access_control.cli.cmd;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-appversion-users", aliases = "lsavu") @CommandGroup("appversion-user")
public class SSCAppVersionUserListCommand extends AbstractSSCBaseRequestOutputCommand {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    // TODO Can we do any server-side filtering?
    // TODO Add boolean options to set extractusersfromgroups and includeuniversalaccessentities request parameters
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(parentResolver.getAppVersionId(unirest)))
                .queryString("embed", "roles(name,allApplicationRole)");
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
