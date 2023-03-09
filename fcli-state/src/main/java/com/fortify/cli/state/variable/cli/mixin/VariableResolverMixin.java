/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.state.variable.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.variable.FcliVariableHelper;
import com.fortify.cli.common.variable.FcliVariableHelper.VariableDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class VariableResolverMixin {
    
    @ReflectiveAccess
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
    
    @ReflectiveAccess
    public static abstract class AbstractRequiredVariableResolverMixin extends AbstractVariableResolverMixin {
        @Getter private boolean required = true; 
    }
    
    @ReflectiveAccess
    public static class RequiredOption extends AbstractRequiredVariableResolverMixin {
        @Option(names = {"--variable"}, required = true)
        @Getter private String variableName;
    }
    
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractRequiredVariableResolverMixin {
        @Parameters(index = "0", arity = "1")
        @Getter private String variableName;
    }
}
