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
package com.fortify.cli.state.entity.variable.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.variable.FcliVariableHelper;
import com.fortify.cli.common.variable.FcliVariableHelper.VariableDescriptor;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class VariableResolverMixin {
    
    public static abstract class AbstractVariableResolverMixin  {
        public abstract String getVariableName();
        public abstract boolean isRequired();

        public VariableDescriptor getVariableDescriptor(){
            return FcliVariableHelper.getVariableDescriptor(getVariableName(), isRequired());
        }
        
        public JsonNode getVariableContents(){
            return FcliVariableHelper.getVariableContents(getVariableName(), isRequired());
        }
    }
    
    public static abstract class AbstractRequiredVariableResolverMixin extends AbstractVariableResolverMixin {
        @Getter private boolean required = true; 
    }
    
    public static class RequiredOption extends AbstractRequiredVariableResolverMixin {
        @Option(names = {"--variable"}, required = true)
        @Getter private String variableName;
    }
    
    public static class PositionalParameter extends AbstractRequiredVariableResolverMixin {
        @Parameters(index = "0", arity = "1")
        @Getter private String variableName;
    }
}
