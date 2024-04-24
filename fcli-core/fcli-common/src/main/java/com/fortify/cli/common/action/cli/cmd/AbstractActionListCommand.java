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
import com.fortify.cli.common.action.helper.ActionHelper;
import com.fortify.cli.common.action.helper.ActionHelper.ActionSignatureHandler;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;

public abstract class AbstractActionListCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Override
    public final JsonNode getJsonNode() {
        return ActionHelper
                .streamAsJson(getType(), ActionSignatureHandler.EVALUATE)
                .collect(JsonHelper.arrayNodeCollector());
    }    
    @Override
    public final boolean isSingular() {
        return false;
    }
    protected abstract String getType();
    
    
}
