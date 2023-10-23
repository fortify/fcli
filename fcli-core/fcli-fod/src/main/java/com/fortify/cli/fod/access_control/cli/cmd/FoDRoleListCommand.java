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
package com.fortify.cli.fod.access_control.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.fod._common.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupType;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-roles", aliases = "lsr") @CommandGroup("role")
@DefaultVariablePropertyName("id")
public class FoDRoleListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer {
    private static final RenameFieldsTransformer RECORD_TRANSFORMER = new RenameFieldsTransformer(
            new String[] {"value:id", "text:name"});
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(FoDUrls.LOOKUP_ITEMS).queryString("type", FoDLookupType.Roles.name());
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        ((ObjectNode)record).remove("group");
        return RECORD_TRANSFORMER.transform(record);
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
