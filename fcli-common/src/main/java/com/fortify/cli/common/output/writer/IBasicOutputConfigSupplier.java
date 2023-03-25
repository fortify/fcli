package com.fortify.cli.common.output.writer;

import com.fortify.cli.common.output.cli.mixin.AbstractOutputHelperMixin;
import com.fortify.cli.common.output.writer.output.standard.IOutputConfigSupplier;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

/**
 * Interface for supplying a basic output configuration which may not have
 * been fully configured yet. For example, {@link AbstractOutputHelperMixin} will 
 * update the basic output configuration, like adding transformers from 
 * various sources.
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
