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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.access_control.cli.mixin.SSCRoleResolverMixin;
import com.fortify.cli.ssc.access_control.helper.SSCRoleDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "delete-role", aliases="rm-role") @CommandGroup("role")
public class SSCRoleDeleteCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private SSCRoleResolverMixin.PositionalParameter roleResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCRoleDescriptor descriptor = roleResolver.getRoleDescriptor(unirest);
        unirest.delete(SSCUrls.ROLE(descriptor.getRoleId())).asString().getBody();
        return descriptor.asJsonNode();
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
