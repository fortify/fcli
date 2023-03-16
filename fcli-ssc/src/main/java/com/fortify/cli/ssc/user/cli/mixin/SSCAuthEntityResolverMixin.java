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
package com.fortify.cli.ssc.user.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.ssc.user.helper.SSCAuthEntitiesHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAuthEntityResolverMixin {
    private static abstract class AbstractSSCAuthEntityResolverMixin {
        public abstract String getAuthEntitySpec();
        
        public JsonNode getAuthEntityJsonNode(UnirestInstance unirest) {
            return new SSCAuthEntitiesHelper(unirest).getAuthEntities(false, true, getAuthEntitySpec());
        }
    }
    
    public static class RequiredOption extends AbstractSSCAuthEntityResolverMixin {
        @Option(names="--user", required = true)
        @Getter private String authEntitySpec;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCAuthEntityResolverMixin {
        @Parameters(index = "0", arity = "1")
        @Getter private String authEntitySpec;
    }
}
