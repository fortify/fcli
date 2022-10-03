package com.fortify.cli.sc_dast.rest.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_dast.util.SCDastOutputConfigHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * This base class for SSC list {@link Command} implementations provides capabilities for running 
 * SSC GET requests and outputting the result in table format. Filtering capabilities are provided
 * through {@link OutputMixinWithQuery}. Subclasses can optionally override the {@link #getQParamGenerator()}
 * to allow for server-side filtering based on SSC's 'q' request parameter. 
 *   
 * @author rsenden
 */
@ReflectiveAccess @FixInjection
public abstract class AbstractSCDastListCommand extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {
    @Getter @Mixin private OutputMixinWithQuery outputMixin;
    
    @Override
    protected final Void run(UnirestInstance unirest) {
        outputMixin.write(generateOutput(unirest));
        return null;
    }
    
    protected JsonNode generateOutput(UnirestInstance unirest) {
        return addQueryParam(generateRequest(unirest)).asObject(JsonNode.class).getBody();
    }

    protected HttpRequest<?> generateRequest(UnirestInstance unirest) {
        throw new IllegalStateException("Either generateRequest or generateOutput method must be implemented by subclass");
    }
    
    protected HttpRequest<?> addQueryParam(HttpRequest<?> request) {
        // TODO Implement server-side filtering if available with SC DAST
        return request;
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SCDastOutputConfigHelper.table().recordTransformer(this::transformRecord);
    }
    
    protected JsonNode transformRecord(JsonNode record) {
        return record;
    }
}