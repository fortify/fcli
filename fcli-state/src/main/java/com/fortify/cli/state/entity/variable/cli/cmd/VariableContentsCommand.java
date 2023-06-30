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
package com.fortify.cli.state.entity.variable.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.state.entity.variable.cli.mixin.VariableOutputHelperMixins;
import com.fortify.cli.state.entity.variable.cli.mixin.VariableResolverMixin;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = VariableOutputHelperMixins.Contents.CMD_NAME)
@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT) // Output columns depend on variable contents
public class VariableContentsCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private VariableOutputHelperMixins.Contents outputHelper;
    @Mixin private VariableResolverMixin.PositionalParameter variableResolver;

    @Override
    public JsonNode getJsonNode() {
        return variableResolver.getVariableContents();
    }
    
    @Override
    public boolean isSingular() {
        return variableResolver.getVariableDescriptor().isSingular();
    }
}
