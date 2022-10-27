package com.fortify.cli.sc_sast.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.basic.IBasicOutputHelper;
import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

/**
 * <p>This class provides standard SC-SAST Controller {@link IBasicOutputHelper} implementations,
 *    extending from the mixins in {@link BasicOutputHelperMixins}.</p>
 */
@ReflectiveAccess
public class SCSastControllerBasicOutputHelperMixins {
    @ReflectiveAccess public static class ScanWaitForArtifact extends BasicOutputHelperMixins.Other {
        public static final String CMD_NAME = "wait-for-artifact";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess public static class ScanWaitForScan extends BasicOutputHelperMixins.Other {
        public static final String CMD_NAME = "wait-for-scan";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess public static class ScanWaitForUpload extends BasicOutputHelperMixins.Other {
        public static final String CMD_NAME = "wait-for-upload";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
}
