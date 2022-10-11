package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.spi.output.AbstractJsonNodeOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.output.IJsonNodeOutputHelper;
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

// TODO Rename this class to UnirestOutputHelperMixins
/**
 * <p>This class provides standard, product-agnostic {@link IJsonNodeOutputHelper} implementations.</p>
 */
public class JsonNodeOutputHelperMixins {
    @ReflectiveAccess
    public static class Create extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "create";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command(aliases = {"rm"})
    public static class Delete extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "delete";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command(aliases = {"clear"})
    public static class DeleteAll extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "delete-all";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command(name = "list", aliases = {"ls"})
    public static class List extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "list";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private OutputWriterWithQueryFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Get extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "get";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.details(); 
    }
    
    @ReflectiveAccess
    public static class Set extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "set";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Update extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "update";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Enable extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "enable";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Disable extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "disable";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Start extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "start";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Pause extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "pause";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Resume extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "resume";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Cancel extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "cancel";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Upload extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "upload";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Download extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "download";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Install extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "install";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Uninstall extends AbstractJsonNodeOutputHelper {
        public static final String CMD_NAME = "uninstall";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command
    public static class Other extends AbstractJsonNodeOutputHelper {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    }
}
