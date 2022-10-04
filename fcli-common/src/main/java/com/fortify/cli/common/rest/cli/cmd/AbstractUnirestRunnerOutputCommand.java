package com.fortify.cli.common.rest.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;

/**
 * This abstract base class provides generic functionality to write output to
 * the {@link OutputMixin} provided by the {@link #getOutputMixin()}, providing
 * a standardized approach for configuring the {@link OutputConfig} instance
 * used to write the output. Subclasses implement or override the various
 * protected methods to define actual behavior.
 *   
 * @author rsenden
 */
@ReflectiveAccess
public abstract class AbstractUnirestRunnerOutputCommand extends AbstractUnirestRunnerCommand implements IOutputConfigSupplier {
    /**
     * This method gets the {@link OutputMixin} using the {@link #getOutputMixin()} method,
     * and provides it together with the provided {@link UnirestInstance} to the 
     * {@link #generateAndWriteOutput(OutputMixin, UnirestInstance)} method to allow
     * subclasses to generate data and write it to the given {@link OutputMixin}.
     */
    @Override
    protected final Void run(UnirestInstance unirest) {
        generateAndWriteOutput(getOutputMixin(), unirest);
        return null;
    }

    /**
     * This implementation for {@link IOutputConfigSupplier#getOutputOptionsWriterConfig()}
     * calls {@link #getBasicOutputConfig()} to get a basic output configuration,
     * then adds input and record transformers using the {@link #addTransformers(OutputConfig)}
     * method.
     */
    @Override
    public final OutputConfig getOutputOptionsWriterConfig() {
        return addTransformers(getBasicOutputConfig());
    }
    
    /**
     * Subclasses need to implement this method to generate data using the provided
     * {@link UnirestInstance}, and write this data to the provided {@link OutputMixin}. 
     * @param outputMixin {@link OutputMixin} used to output the generated data
     * @param unirest {@link UnirestInstance} that may be used to generate data
     */
    protected abstract void generateAndWriteOutput(OutputMixin outputMixin, UnirestInstance unirest);

    /**
     * Subclasses need to implement this method to return the {@link OutputMixin}
     * implementation used to write the output. Subclasses would usually implement
     * this method using one of the following approaches:
     * </ul>
     * <li>{@code @Getter @Mixin private OutputMixin outputMixin;}</li>
     * <li>{@code @Getter @Mixin private OutputMixinWithQuery outputMixin;}</li>
     * </ul>
     * @return {@link OutputMixin} or any of its subclasses.
     */
    protected abstract OutputMixin getOutputMixin();
    
    /**
     * Subclasses need to implement this method to return a basic output
     * configuration, specifying for example the default output format.
     * Note that input and record transformation should usually not be 
     * configured in the {@link OutputConfig} returned by this method;
     * subclasses would usually override {@link #getInputTransformer()}
     * or {@link #transformInput(JsonNode)}, and {@link #getRecordTransformer()}
     * or {@link #transformRecord(JsonNode)}.
     * @return
     */
    protected abstract OutputConfig getBasicOutputConfig();
    
    /**
     * This method adds input and record transformers as returned by the
     * {@link #getInputTransformer()} and {@link #getRecordTransformer()}
     * methods to the {@link OutputConfig} instance returned by the 
     * {@link #getBasicOutputConfig()} method. Unless a command 
     * implementation has very specific transformation needs, this method 
     * should not be overridden.
     * 
     * @param outputConfig {@link OutputConfig} instance as returned by {@link #getBasicOutputConfig()}
     * @return {@link OutputConfig} instance with input and record transformers added
     */
    protected OutputConfig addTransformers(OutputConfig outputConfig) {
        return outputConfig.inputTransformer(getInputTransformer()).recordTransformer(getRecordTransformer());
    }

    /**
     * This method may be overridden by subclasses to provide an input transformer 
     * to be supplied to {@link OutputConfig#inputTransformer(UnaryOperator)}. The
     * default implementation uses {@link #transformInput(JsonNode)} to transform
     * the input. Either {@link #getInputTransformer()} or {@link #transformInput(JsonNode)}
     * should be overridden, not both.
     * @return Input transformer
     */
    protected UnaryOperator<JsonNode> getInputTransformer() {
        return this::transformInput;
    }
    
    /**
     * This method may be overridden by subclasses to transform the input, for
     * example for retrieving a nested {@link JsonNode} from the HTTP response.
     * By default, this method returns the provided input as-is, without doing 
     * any transformation. As this method is used for the default implementation 
     * of {@link #getInputTransformer()}, either {@link #getInputTransformer()} 
     * or {@link #transformInput(JsonNode)} should be overridden, not both.
     * @param input Input to be transformed
     * @return Transformed input
     */
    protected JsonNode transformInput(JsonNode input) {
        return input;
    }
    
    /**
     * This method may be overridden by subclasses to provide a record transformer 
     * to be supplied to {@link OutputConfig#recordTransformer(UnaryOperator)}. The
     * default implementation uses {@link #transformRecord(JsonNode)} to transform
     * the record. Either {@link #getRecordTransformer()} or {@link #transformRecord(JsonNode)}
     * should be overridden, not both.
     * @return Record transformer
     */
    protected UnaryOperator<JsonNode> getRecordTransformer() {
        return this::transformRecord;
    }
    
    /**
     * This method may be overridden by subclasses to transform the given record, 
     * for example for adding additional properties. By default, this method returns 
     * the provided record as-is, without doing any transformation. As this method 
     * is used for the default implementation of {@link #getRecordTransformer()}, 
     * either {@link #getRecordTransformer()} or {@link #transformRecord(JsonNode)} 
     * should be overridden, not both.
     * @param record Record to be transformed
     * @return Transformed record
     */
    protected JsonNode transformRecord(JsonNode record) {
        return record;
    }
}