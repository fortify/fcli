package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.mixin.spi.AbstractOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.IProductHelper;
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

/**
 * <p>This class provides standard, product-agnostic {@link IOutputHelper} implementations.
 * Each product module should provide a {@code <Product>OutputHelperMixins} class that
 * provides similarly named inner classes that extend from the corresponding 
 * {@link OutputHelperMixins} inner class, with a no-argument constructor that injects 
 * the product-specific {@link IProductHelper} by calling the 
 * {@link AbstractOutputHelper#setProductHelper(IProductHelper)} method. For example:</p>
 * 
 * <pre>
 * public class MyProductOutputHelperMixins {
 *     ...
 *     
 *     &#64;ReflectiveAccess
 *     public static class List extends OutputHelperMixins.List {
 *         public List() { setProductHelper(new MyProductProductHelper()); }
 *     }
 *     
 *     ...
 * }
 * </pre>
 * 
 * <p>Note that the {@link IOutputHelper} instance is injected back into the provided
 * {@link IProductHelper}, so you should always create a new {@link IProductHelper}
 * instance, rather than reusing the same {@link IProductHelper} instance for all 
 * {@link IOutputHelper} implementations.</p> 
 * 
 * <p>Each {@link Command} implementation can then use the appropriate product-specific 
 * {@link IOutputHelper} implementation through the {@link Mixin} annotation, i.e.:</p>
 * 
 * <pre>
 * &#64;ReflectiveAccess
 * &#64;Command(name = MyProductOutputHelperMixins.List.CMD_NAME)
 * public class SomeListCommand extends AbstractMyProductOutputCommand implements IBaseHttpRequestSupplier {
 *     &#64;Getter &#64;Mixin private MyProductOutputHelperMixins.List outputHelper;
 *     ...
 * }
 * </pre>
 * 
 * <p>Here, {@code AbstractMyProductOutputCommand} would extend from {@link AbstractOutputCommand},
 * which takes care of displaying the output generated by the command, based on the configured
 * {@link IOutputHelper} {@link Mixin}.</p>
 * 
 * <p>For consistency, command implementations should use the {@link IOutputHelper} implementation
 * that exactly matches the command. For example, you should only be using the {@link List}
 * implementation if you are actually implementing a 'list' command; {@link List} shouldn't be used
 * by commands that just generate some list of output but are not actually called 'list'. In other
 * words, the command name should match the {@code CMD_NAME} provided by the {@link IOutputHelper}
 * implementation (and you should always use that constant to define the {@link Command} name.</p>
 * 
 * If there is no matching standard {@link IOutputHelper} implementation, there are two options:
 * <ul>
 * <li>The {@code MyProductOutputHelperMixins} class can define additional, product-specific
 *     {@link IOutputHelper} implementations, that extend from {@link Other} and provide 
 *     appropriate {@code CMD_NAME} constants and other output attributes</li>
 * <li>The {@link Command} implementation can use the product-specific {@link Other} implementation</li>
 * </ul>
 *  
 * @author rsenden
 */
public class OutputHelperMixins {
    @ReflectiveAccess
    public static class Create extends AbstractOutputHelper {
        public static final String CMD_NAME = "create";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command(aliases = {"rm"})
    public static class Delete extends AbstractOutputHelper {
        public static final String CMD_NAME = "delete";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command(name = "list", aliases = {"ls"})
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
    public static class Set extends AbstractOutputHelper {
        public static final String CMD_NAME = "set";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Update extends AbstractOutputHelper {
        public static final String CMD_NAME = "update";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
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
    
    @ReflectiveAccess
    public static class Start extends AbstractOutputHelper {
        public static final String CMD_NAME = "start";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Pause extends AbstractOutputHelper {
        public static final String CMD_NAME = "pause";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Resume extends AbstractOutputHelper {
        public static final String CMD_NAME = "resume";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Cancel extends AbstractOutputHelper {
        public static final String CMD_NAME = "cancel";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Upload extends AbstractOutputHelper {
        public static final String CMD_NAME = "upload";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Download extends AbstractOutputHelper {
        public static final String CMD_NAME = "download";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Install extends AbstractOutputHelper {
        public static final String CMD_NAME = "install";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Uninstall extends AbstractOutputHelper {
        public static final String CMD_NAME = "uninstall";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command
    public static class Other extends AbstractOutputHelper {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    }
}
