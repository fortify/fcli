package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.output.cli.cmd.unirest.AbstractUnirestOutputCommand;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.AbstractUnirestOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.cli.mixin.writer.OutputWriterWithQueryFactoryMixin;
import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

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
 * <p>This class provides standard, product-agnostic {@link IUnirestOutputHelper} implementations.
 * Each product module should provide a {@code <Product>OutputHelperMixins} class that
 * provides similarly named inner classes that extend from the corresponding 
 * {@link UnirestOutputHelperMixins} inner class, with a no-argument constructor that injects 
 * the product-specific {@link IProductHelper} by calling the 
 * {@link AbstractUnirestOutputHelper#setProductHelper(IProductHelper)} method. For example:</p>
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
 * <p>Note that the {@link IUnirestOutputHelper} instance is injected back into the provided
 * {@link IProductHelper}, so you should always create a new {@link IProductHelper}
 * instance, rather than reusing the same {@link IProductHelper} instance for all 
 * {@link IUnirestOutputHelper} implementations.</p> 
 * 
 * <p>Each {@link Command} implementation can then use the appropriate product-specific 
 * {@link IUnirestOutputHelper} implementation through the {@link Mixin} annotation, i.e.:</p>
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
 * <p>Here, {@code AbstractMyProductOutputCommand} would extend from {@link AbstractUnirestOutputCommand},
 * which takes care of displaying the output generated by the command, based on the configured
 * {@link IUnirestOutputHelper} {@link Mixin}.</p>
 * 
 * <p>For consistency, command implementations should use the {@link IUnirestOutputHelper} implementation
 * that exactly matches the command. For example, you should only be using the {@link List}
 * implementation if you are actually implementing a 'list' command; {@link List} shouldn't be used
 * by commands that just generate some list of output but are not actually called 'list'. In other
 * words, the command name should match the {@code CMD_NAME} provided by the {@link IUnirestOutputHelper}
 * implementation (and you should always use that constant to define the {@link Command} name.</p>
 * 
 * If there is no matching standard {@link IUnirestOutputHelper} implementation, there are two options:
 * <ul>
 * <li>The {@code MyProductOutputHelperMixins} class can define additional, product-specific
 *     {@link IUnirestOutputHelper} implementations, that extend from {@link Other} and provide 
 *     appropriate {@code CMD_NAME} constants and other output attributes</li>
 * <li>The {@link Command} implementation can use the product-specific {@link Other} implementation</li>
 * </ul>
 *  
 * @author rsenden
 */
public class UnirestOutputHelperMixins {
    @ReflectiveAccess
    public static class Add extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "add";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Create extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "create";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command(aliases = {"rm"})
    public static class Delete extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "delete";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command(aliases = {"clear"})
    public static class DeleteAll extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "delete-all";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command(name = "list", aliases = {"ls"})
    public static class List extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "list";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private OutputWriterWithQueryFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Get extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "get";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.details(); 
    }
    
    @ReflectiveAccess
    public static class Set extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "set";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Update extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "update";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Enable extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "enable";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Disable extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "disable";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Start extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "start";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Pause extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "pause";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Resume extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "resume";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Cancel extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "cancel";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class WaitFor extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "wait-for";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private OutputWriterWithQueryFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Upload extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "upload";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Download extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "download";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Install extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "install";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess
    public static class Uninstall extends AbstractUnirestOutputHelper {
        public static final String CMD_NAME = "uninstall";
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess @Command
    public static class Other extends AbstractUnirestOutputHelper {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    }
}
