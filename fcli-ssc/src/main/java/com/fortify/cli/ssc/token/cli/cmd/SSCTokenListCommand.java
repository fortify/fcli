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
package com.fortify.cli.ssc.token.cli.cmd;

import java.util.HashMap;
import java.util.Map;

import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.output.cli.mixin.filter.AddAsDefaultColumn;
import com.fortify.cli.common.output.cli.mixin.filter.OutputFilter;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentials;
import com.fortify.cli.ssc.rest.cli.mixin.filter.SSCFilterMixin;
import com.fortify.cli.ssc.rest.cli.mixin.filter.SSCFilterQParam;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;
import com.fortify.cli.ssc.util.SSCOutputHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "list")
public class SSCTokenListCommand extends AbstractSSCTokenCommand implements IOutputConfigSupplier {
    @Mixin private OutputMixin outputMixin;
    @Mixin private SSCFilterMixin filterMixin;
    
    // TODO Check whether SSC allows for q-based filtering on any of these fields
    @Option(names={"--id"}) @OutputFilter @AddAsDefaultColumn
    private Integer id;
    
    @Option(names={"--userName"}) @SSCFilterQParam @AddAsDefaultColumn
    private String username;
    
    @Option(names={"--type"}) @SSCFilterQParam @AddAsDefaultColumn
    private String type;
    
    @Option(names={"--creationDate"}) @OutputFilter @AddAsDefaultColumn
    private String creationDate;
    
    @Option(names={"--terminalDate"}) @OutputFilter @AddAsDefaultColumn
    private String terminalDate;
    
    @Option(names={"--remainingUsages"}) @OutputFilter @AddAsDefaultColumn
    private Integer remainingUsages;
    
    @Option(names={"--description"}) @OutputFilter @AddAsDefaultColumn
    private String description;
    
    @Override
    protected void run(SSCTokenHelper tokenHelper, IUrlConfig urlConfig, IUserCredentials userCredentials) {
        outputMixin.write(tokenHelper.listTokens(urlConfig, userCredentials, getQueryParams()));
    }
    
    private Map<String, Object> getQueryParams() {
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("limit", "-1");
        String qParamValue = filterMixin.getQParamValue();
        if ( qParamValue!=null && !qParamValue.isBlank() ) {
            queryParams.put("q", qParamValue);
        }
        return queryParams;
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputHelper.defaultTableOutputConfig()
                .defaultColumns(outputMixin.getDefaultColumns());
    }
}
