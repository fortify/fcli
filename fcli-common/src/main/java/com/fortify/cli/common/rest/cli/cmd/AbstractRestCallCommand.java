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
package com.fortify.cli.common.rest.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.session.manager.api.ISessionData;

import io.micronaut.core.util.StringUtils;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class AbstractRestCallCommand<D extends ISessionData> extends AbstractUnirestRunnerCommand<D> {
    public static final String CMD_NAME = "call"; 
    @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
    @Parameters(index = "0", arity = "1..1", descriptionKey = "api.uri") String uri;
    
    @Option(names = {"--request", "-X"}, required = false, defaultValue = "GET")
    @Getter private String httpMethod;
    
    @Option(names = {"--data", "-d"}, required = false)
    @Getter private String data; // TODO Add ability to read data from file
    
    // TODO Add options for content-type, arbitrary headers, ...?
    
    @Override
    protected final Void run(UnirestInstance unirest) {
        outputWriterFactory.createOutputWriter(StandardOutputConfig.json())
            .write(prepareRequest(unirest));
        return null;
    }
    
    protected final HttpRequest<?> prepareRequest(UnirestInstance unirest) {
        if ( StringUtils.isEmpty(uri) ) {
            throw new IllegalArgumentException("Uri must be specified");
        }
        var request = unirest.request(httpMethod, uri);
        // TODO Add Content-Type & accept headers
        return data==null ? request : request.body(data);
    }
    
}
