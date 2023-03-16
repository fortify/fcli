package com.fortify.cli.common.output.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.spi.basic.AbstractBasicOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.basic.IBasicOutputHelper;
import com.fortify.cli.common.output.cli.mixin.writer.OutputWriterWithQueryFactoryMixin;
import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

/**
 * <p>This class provides standard, product-agnostic {@link IBasicOutputHelper} implementations.
 *    The mixins provided in this class are equivalent to the mixins in {@link UnirestOutputHelperMixins},
 *    but allow for writing {@link JsonNode} instances to be written when no {@link UnirestInstance}
 *    and/or {@link IProductHelper} is available or required.</p>
 */
public class BasicOutputHelperMixins {
     @Command
    public static class Other extends AbstractBasicOutputHelper {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    }
    
    
    public static class TableWithQuery extends Other {
        @Getter @Mixin private OutputWriterWithQueryFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    
    public static class TableNoQuery extends Other {
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
	
	
    public static class Login extends TableNoQuery {
        public static final String CMD_NAME = "login";
    }
	
	
    public static class Logout extends TableNoQuery {
        public static final String CMD_NAME = "logout";
    }
	
    
    public static class Add extends TableNoQuery {
        public static final String CMD_NAME = "add";
    }
    
    
    public static class Create extends TableNoQuery {
        public static final String CMD_NAME = "create";
    }
    
     @Command(aliases = {"rm"})
    public static class Delete extends TableNoQuery {
        public static final String CMD_NAME = "delete";
    }
    
    
    public static class DeleteAll extends TableNoQuery {
        public static final String CMD_NAME = "delete-all";
    }
    
    
    public static class Revoke extends TableNoQuery {
        public static final String CMD_NAME = "revoke";
    }
    
    
    public static class Clear extends TableNoQuery {
        public static final String CMD_NAME = "clear";
    }
    
     @Command(name = "list", aliases = {"ls"})
    public static class List extends TableWithQuery {
        public static final String CMD_NAME = "list";
    }
    
    
    public static class Get extends TableNoQuery {
        public static final String CMD_NAME = "get";
    }
    
    
    public static class Set extends TableNoQuery {
        public static final String CMD_NAME = "set";
    }
    
    
    public static class Update extends TableNoQuery {
        public static final String CMD_NAME = "update";
    }
    
    
    public static class Enable extends TableNoQuery {
        public static final String CMD_NAME = "enable";
    }
    
    
    public static class Disable extends TableNoQuery {
        public static final String CMD_NAME = "disable";
    }
    
    
    public static class Start extends TableNoQuery {
        public static final String CMD_NAME = "start";
    }
    
    
    public static class Pause extends TableNoQuery {
        public static final String CMD_NAME = "pause";
    }
    
    
    public static class Resume extends TableNoQuery {
        public static final String CMD_NAME = "resume";
    }
    
    
    public static class Cancel extends TableNoQuery {
        public static final String CMD_NAME = "cancel";
    }
    
    
    public static class WaitFor extends TableNoQuery {
        public static final String CMD_NAME = "wait-for";
    }
    
    
    public static class Upload extends TableNoQuery {
        public static final String CMD_NAME = "upload";
    }
    
    
    public static class Download extends TableNoQuery {
        public static final String CMD_NAME = "download";
    }
    
    
    public static class Install extends TableNoQuery {
        public static final String CMD_NAME = "install";
    }
    
    
    public static class Uninstall extends TableNoQuery {
        public static final String CMD_NAME = "uninstall";
    }
}
