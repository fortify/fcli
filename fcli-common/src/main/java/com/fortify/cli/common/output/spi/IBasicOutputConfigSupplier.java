package com.fortify.cli.common.output.spi;

import com.fortify.cli.common.output.cli.mixin.spi.basic.AbstractBasicOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.AbstractUnirestOutputHelper;
import com.fortify.cli.common.output.writer.output.standard.IOutputConfigSupplier;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

/**
 * Interface for supplying a basic output configuration which may not have
 * been fully configured yet. For example, {@link AbstractBasicOutputHelper}
 * and {@link AbstractUnirestOutputHelper} will update the basic output
 * configuration, like adding transformers from various sources.
 * 
 * Note that this interface is very similar to {@link IOutputConfigSupplier}, 
 * but has a different purpose.
 * 
 * @author rsenden
 *
 */
public interface IBasicOutputConfigSupplier {
    StandardOutputConfig getBasicOutputConfig();
}
