package com.fortify.cli.common.output.cli.mixin.spi.basic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.spi.AbstractOutputHelper;
import com.fortify.cli.common.output.spi.IBasicOutputConfigSupplier;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

public abstract class AbstractBasicOutputHelper extends AbstractOutputHelper implements IBasicOutputHelper {
    /**
     * Write the given {@link JsonNode} using the output writer created by the
     * {@link #createOutputWriter()} method.
     */
    @Override
    public final void write(JsonNode jsonNode) {
        createOutputWriter().write(jsonNode);
    }

    /**
     * This method adds record transformers to the given {@link StandardOutputConfig} by
     * calling the following methods, in this order:
     * <ul>
     * <li>{@link #addRecordTransformersFromObject(StandardOutputConfig, Object)} with the command being invoked</li>
     * <li>{@link #addCommandActionResultRecordTransformer(StandardOutputConfig, Object)} with the command being invoked</li>
     * <ul>
     * If a command needs to run any record transformations before the record transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those record transformations to the basic output configuration.
     * @param standardOutputConfig
     * @param cmd
     */
    protected final void addRecordTransformersForCommand(StandardOutputConfig standardOutputConfig, Object cmd) {
        addRecordTransformersFromObject(standardOutputConfig, cmd);
        addCommandActionResultRecordTransformer(standardOutputConfig, cmd);
    }

    /**
     * This method adds input transformers to the given {@link StandardOutputConfig} by
     * calling the following methods, in this order:
     * <ul>
     * <li>{@link #addInputTransformersFromObject(StandardOutputConfig, Object)} with the command being invoked</li>
     * <ul>
     * If a command needs to run any input transformations before the input transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those input transformations to the basic output configuration.
     * @param standardOutputConfig
     * @param cmd
     */
    protected final void addInputTransformersForCommand(StandardOutputConfig standardOutputConfig, Object cmd) {
        addInputTransformersFromObject(standardOutputConfig, cmd);
    }
}
