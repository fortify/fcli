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
package com.fortify.cli.ssc.alert_definition.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCAlertDefinitionGetCommand extends AbstractSSCBaseRequestOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper; 
    // TODO Add support for resolving alert definitions by name (if unique) using a ResolverMixin
    @Parameters(arity="1", descriptionKey="fcli.ssc.alert-definition.get.id")
    private String alertDefinitionId;
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(SSCUrls.ALERT_DEFINITION(alertDefinitionId));
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
