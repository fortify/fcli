package com.fortify.cli.fod.rest.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.fod.rest.query.FoDOutputQueryFiltersParamGenerator;
import com.fortify.cli.fod.util.FoDOutputConfigHelper;
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
@ReflectiveAccess @FixInjection
public abstract class AbstractFoDHttpListCommand extends AbstractFoDUnirestRunnerCommand implements IOutputConfigSupplier {
    @Getter @Mixin private OutputMixinWithQuery outputMixin;
    
    @Override
    protected final Void run(UnirestInstance unirest) {
        System.out.println("in here");
        outputMixin.write(
                addFiltersParam(generateRequest(unirest)),
                //generateRequest(unirest),
                FoDOutputConfigHelper.pagingHandler(generateRequest(unirest).getUrl()));
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