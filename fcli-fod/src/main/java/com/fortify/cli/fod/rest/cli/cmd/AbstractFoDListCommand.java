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
package com.fortify.cli.fod.rest.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.fod.rest.query.FoDOutputQueryFiltersParamGenerator;
import com.fortify.cli.fod.util.FoDOutputConfigHelper;
import com.fortify.cli.fod.util.FoDOutputHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * This base class for FoD list {@link Command} implementations provides capabilities for running
 * FoD GET requests and outputting the result in table format. Filtering capabilities are provided
 * through {@link OutputMixinWithQuery}. Subclasses can optionally override the {@link #getFiltersParamGenerator()}
 * to allow for server-side filtering based on FoD's 'filters' request parameter.
 *   
 * @author kadraman
 */
@ReflectiveAccess
public abstract class AbstractFoDListCommand extends AbstractFoDUnirestRunnerCommand implements IOutputConfigSupplier {
    @Getter @Mixin private OutputMixinWithQuery outputMixin;
    
    @Override
    protected final Void run(UnirestInstance unirest) {
        outputMixin.write(
                addFiltersParam(generateRequest(unirest)),
                //generateRequest(unirest),
                FoDOutputHelper.pagingHandler(generateRequest(unirest).getUrl()));
        return null;
    }
    
    protected JsonNode generateOutput(UnirestInstance unirest) {
        return addFiltersParam(generateRequest(unirest)).asObject(JsonNode.class).getBody();
    }

    protected HttpRequest<?> generateRequest(UnirestInstance unirest) {
        throw new IllegalStateException("Either generateRequest or generateOutput method must be implemented by subclass");
    }
    
    protected HttpRequest<?> addFiltersParam(HttpRequest<?> request) {
        FoDOutputQueryFiltersParamGenerator filtersParamGenerator = getFiltersParamGenerator();
        HttpRequest<?> r = filtersParamGenerator==null ? request : filtersParamGenerator.addFiltersParam(request, outputMixin.getOutputQueries());
        return r;
    }

    protected FoDOutputQueryFiltersParamGenerator getFiltersParamGenerator() {
        return null;
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return FoDOutputConfigHelper.table().recordTransformer(this::transformRecord);
    }
    
    protected JsonNode transformRecord(JsonNode record) {
        return record;
    }
}