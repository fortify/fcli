package com.fortify.cli.ssc.rest.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * This base class for {@link Command} implementations provides capabilities for running 
 * SSC GET requests and outputting the result in table format.
 *   
 * @author rsenden
 */
@ReflectiveAccess
public abstract class AbstractSSCTableOutputCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @Getter @Mixin private OutputMixin outputMixin;
    
    @Override
    protected final Void run(UnirestInstance unirest) {
        outputMixin.write(generateOutput(unirest));
        return null;
    }
    
    protected JsonNode generateOutput(UnirestInstance unirest) {
        return generateRequest(unirest).asObject(JsonNode.class).getBody();
    }
    
    protected GetRequest generateRequest(UnirestInstance unirest) {
        throw new IllegalStateException("Either generateRequest or generateOutput method must be implemented by subclass");
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputConfigHelper.table();
    }
}