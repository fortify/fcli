package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.spi.AbstractOutputHelper;
import com.fortify.cli.common.output.writer.output.StandardOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQueryFactory;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class OutputHelperMixins {
    @ReflectiveAccess
    public static class List extends AbstractOutputHelper {
        public static final String CMD_NAME = "list";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private OutputWriterWithQueryFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Get extends AbstractOutputHelper {
        public static final String CMD_NAME = "get";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.details(); 
    }
    
    @ReflectiveAccess
    public static class Enable extends AbstractOutputHelper {
        public static final String CMD_NAME = "enable";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Disable extends AbstractOutputHelper {
        public static final String CMD_NAME = "disable";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command
    public static class Other extends AbstractOutputHelper {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    }
}
