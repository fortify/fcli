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
import com.fortify.cli.common.action.helper.ActionImportHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

public abstract class AbstractActionResetCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Override
    public final JsonNode getJsonNode() {
        return ActionImportHelper.reset(getType());
    }
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
    @Override
    public final boolean isSingular() {
        return false;
    }
    protected abstract String getType();
    
    
}
