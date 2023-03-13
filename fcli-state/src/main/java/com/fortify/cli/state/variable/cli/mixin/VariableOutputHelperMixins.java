package com.fortify.cli.state.variable.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.writer.OutputWriterWithQueryFactoryMixin;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public class VariableOutputHelperMixins {
    @ReflectiveAccess public static class Contents extends BasicOutputHelperMixins.Other {
        public static final String CMD_NAME = "contents";
        @Getter @Mixin private OutputWriterWithQueryFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess public static class Delete 
        extends BasicOutputHelperMixins.Delete {}
    
    @ReflectiveAccess public static class DeleteAll 
    extends BasicOutputHelperMixins.DeleteAll {}

    @ReflectiveAccess public static class List 
        extends BasicOutputHelperMixins.List {}

    @ReflectiveAccess public static class Get 
        extends BasicOutputHelperMixins.Get {}
}
