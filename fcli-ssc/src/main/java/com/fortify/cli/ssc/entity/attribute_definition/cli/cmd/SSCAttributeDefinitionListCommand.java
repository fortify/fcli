/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.entity.attribute_definition.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc.rest.query.SSCQParamGenerator;
import com.fortify.cli.ssc.rest.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc.rest.query.cli.mixin.SSCQParamMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCAttributeDefinitionListCommand extends AbstractSSCBaseRequestOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    @Mixin private SSCQParamMixin qParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
                .add("id", SSCQParamValueGenerators::plain)
                .add("category", SSCQParamValueGenerators::wrapInQuotes)
                .add("guid", SSCQParamValueGenerators::wrapInQuotes)
                .add("name", SSCQParamValueGenerators::wrapInQuotes)
                .add("type", SSCQParamValueGenerators::wrapInQuotes)
                .add("required", SSCQParamValueGenerators::plain);

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/attributeDefinitions?limit=-1&orderby=category,name");
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}