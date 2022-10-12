package com.fortify.cli.common.output.cli.mixin.spi.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.spi.product.IProductHelper;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public abstract class AbstractJsonNodeOutputHelper extends AbstractOutputHelper implements IJsonNodeOutputHelper {
    /**
     * Write the given {@link JsonNode} using the output writer created by the
     * {@link #createOutputWriter()} method.
     */
    @Override
    public final void write(JsonNode jsonNode) {
        createOutputWriter().write(jsonNode);
    }

    /**
     * This method adds record transformers to the given {@link OutputConfig} by
     * calling the following methods, in this order:
     * <ul>
     * <li>{@link #addRecordTransformersFromObject(OutputConfig, Object)} with the command being invoked</li>
     * <li>{@link #addCommandActionResultRecordTransformer(OutputConfig, Object)} with the command being invoked</li>
     * <ul>
     * If a command needs to run any record transformations before the record transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those record transformations to the basic output configuration.
     * @param outputConfig
     * @param cmd
     */
    protected final void addRecordTransformersForCommand(OutputConfig outputConfig, Object cmd) {
        addRecordTransformersFromObject(outputConfig, cmd);
        addCommandActionResultRecordTransformer(outputConfig, cmd);
    }

    /**
     * This method adds input transformers to the given {@link OutputConfig} by
     * calling the following methods, in this order:
     * <ul>
     * <li>{@link #addInputTransformersFromObject(OutputConfig, Object)} with the command being invoked</li>
     * <ul>
     * If a command needs to run any input transformations before the input transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those input transformations to the basic output configuration.
     * @param outputConfig
     * @param cmd
     */
    protected final void addInputTransformersForCommand(OutputConfig outputConfig, Object cmd) {
        addInputTransformersFromObject(outputConfig, cmd);
    }
}
