package com.fortify.cli.ssc.rest.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.output.cli.mixin.filter.AddAsDefaultColumn;
import com.fortify.cli.common.output.cli.mixin.filter.OutputFilter;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.ssc.rest.cli.mixin.filter.SSCFilterMixin;
import com.fortify.cli.ssc.rest.cli.mixin.filter.SSCFilterQParam;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * This base class for {@link Command} implementations provides capabilities for running 
 * SSC GET requests and outputting the result in table format. Filtering {@link Option}s 
 * are supported through the {@link OutputFilter} and {@link SSCFilterQParam} annotations,
 * and table columns are by default generated based on corresponding {@link AddAsDefaultColumn} 
 * annotations.
 *   
 * @author rsenden
 */
@ReflectiveAccess @FixInjection
public abstract class AbstractSSCTableOutputCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @Getter @Mixin private OutputMixin outputMixin;
    @Mixin private SSCFilterMixin sscFilterMixin;
    
    @Override
    protected final Void run(UnirestInstance unirest) {
        outputMixin.write(generateOutput(unirest));
        return null;
    }
    
    protected JsonNode generateOutput(UnirestInstance unirest) {
        return sscFilterMixin.addFilterParams(generateRequest(unirest)).asObject(JsonNode.class).getBody();
    }
    
    protected GetRequest generateRequest(UnirestInstance unirest) {
        throw new IllegalStateException("Either generateRequest or generateOutput method must be implemented by subclass");
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return (isOutputWrappedInDataObject() 
                ? SSCOutputConfigHelper.tableFromData() 
                : SSCOutputConfigHelper.tableFromObjects()) 
            .defaultColumns(getOutputMixin().getDefaultColumns());
    }

    protected boolean isOutputWrappedInDataObject() { return true; }
}