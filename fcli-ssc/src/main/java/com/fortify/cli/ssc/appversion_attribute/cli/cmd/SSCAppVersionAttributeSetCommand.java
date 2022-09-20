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
package com.fortify.cli.ssc.appversion_attribute.cli.cmd;

import java.util.Map;

import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAppVersionAttributeListHelper;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAppVersionAttributeUpdateHelper;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.util.SSCOutputHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = "set")
public class SSCAppVersionAttributeSetCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @Parameters(index = "0..*", arity = "1..*", paramLabel = "[CATEGORY:]ATTR=VALUE[,VALUE...]")
    private Map<String,String> attributes;
    @Mixin private SSCAppVersionResolverMixin.For parentResolver;
    @Mixin private OutputMixin outputMixin;
    
    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        SSCAttributeDefinitionHelper attrDefHelper = new SSCAttributeDefinitionHelper(unirest);
        SSCAppVersionAttributeUpdateHelper attrUpdateHelper = new SSCAppVersionAttributeUpdateHelper(attrDefHelper, attributes);
        String applicationVersionId = parentResolver.getAppVersionId(unirest);
        
        outputMixin.write(
            new SSCAppVersionAttributeListHelper()
                .attributeDefinitionHelper(attrDefHelper)
                .request("attrUpdate", attrUpdateHelper.getAttributeUpdateRequest(unirest, applicationVersionId))
                .attrIdsToInclude(attrUpdateHelper.getAttributeIds())
                .execute(unirest, applicationVersionId));
        
        return null;
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputHelper.defaultTableOutputConfig()
            .defaultColumns("id#category#guid#name#valueString");
    }
}
