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
package com.fortify.cli.common.action.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.action.cli.mixin.ActionResolverMixin;
import com.fortify.cli.common.action.helper.ActionImportHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import picocli.CommandLine.Mixin;

public abstract class AbstractActionImportCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Mixin private ActionResolverMixin.OptionalParameter actionResolver;
    
    @Override
    public final JsonNode getJsonNode() {
        var source = actionResolver.getActionSourceResolver().getSource();
        var action = actionResolver.getAction();
        if ( action!=null) {
            return ActionImportHelper.importAction(getType(), source, action);
        } else {
            var zip = actionResolver.getActionSourceResolver().getSource();
            if ( zip!=null ) {
                return ActionImportHelper.importZip(getType(), zip);
            } else {
                throw new IllegalArgumentException("Either action and/or --from-zip option must be specified");
            }
        }
    }
    @Override
    public String getActionCommandResult() {
        return "IMPORTED";
    }
    @Override
    public final boolean isSingular() {
        return false;
    }
    protected abstract String getType();
    
    
}
