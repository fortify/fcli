package com.fortify.cli.sc_sast.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.basic.IBasicOutputHelper;
import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import lombok.Getter;
import picocli.CommandLine.Mixin;

/**
 * <p>This class provides standard SC-SAST Controller {@link IBasicOutputHelper} implementations,
 *    extending from the mixins in {@link BasicOutputHelperMixins}.</p>
 */
public class SCSastControllerBasicOutputHelperMixins {
    public static class WaitFor extends BasicOutputHelperMixins.WaitFor {
        public static final String CMD_NAME = "wait-for";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
}
