package com.fortify.cli.sc_sast.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerOutputHelperMixins.SCSastProductHelper;
import com.fortify.cli.sc_sast.rest.helper.SCSastInputTransformer;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Mixin;

/**
 * <p>This class provides standard, SC-SAST specific {@link IUnirestOutputHelper} implementations,
 * replicating the product-agnostic {@link IUnirestOutputHelper} implementations provided in 
 * {@link UnirestOutputHelperMixins}, adding product-specific functionality through the
 * {@link ProductHelperClass} annotation on this enclosing class. In addition to the
 * {@link IUnirestOutputHelper} implementations provided by the common {@link UnirestOutputHelperMixins},
 * this class defines some additional implementations specific for SC SAST.</p>
 * 
 * @author rsenden
 */
@ReflectiveAccess
@ProductHelperClass(SCSastProductHelper.class)
public class SCSastControllerOutputHelperMixins {
    public static class SCSastProductHelper implements IProductHelper, IInputTransformerSupplier {
        @Getter @Setter private IUnirestOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = SCSastInputTransformer::getItems;
    }
    
    @ReflectiveAccess public static class Create 
               extends UnirestOutputHelperMixins.Create {}
    
    @ReflectiveAccess public static class Delete 
               extends UnirestOutputHelperMixins.Delete {}
    
    @ReflectiveAccess public static class List 
               extends UnirestOutputHelperMixins.List {}
    
    @ReflectiveAccess public static class Get 
               extends UnirestOutputHelperMixins.Get {}
    
    @ReflectiveAccess public static class Set 
               extends UnirestOutputHelperMixins.Set {}
    
    @ReflectiveAccess public static class Update 
               extends UnirestOutputHelperMixins.Update {}
    
    @ReflectiveAccess public static class Enable 
               extends UnirestOutputHelperMixins.Enable {}
    
    @ReflectiveAccess public static class Disable 
               extends UnirestOutputHelperMixins.Disable {}
    
    @ReflectiveAccess public static class Start 
               extends UnirestOutputHelperMixins.Start {}
    
    @ReflectiveAccess public static class Pause 
               extends UnirestOutputHelperMixins.Pause {}
    
    @ReflectiveAccess public static class Resume 
               extends UnirestOutputHelperMixins.Resume {}
    
    @ReflectiveAccess public static class Cancel 
               extends UnirestOutputHelperMixins.Cancel {}
    
    @ReflectiveAccess public static class Upload 
               extends UnirestOutputHelperMixins.Upload {}
    
    @ReflectiveAccess public static class Download 
               extends UnirestOutputHelperMixins.Download {}
    
    @ReflectiveAccess public static class Install 
               extends UnirestOutputHelperMixins.Install {}
    
    @ReflectiveAccess public static class Uninstall 
               extends UnirestOutputHelperMixins.Uninstall {}
    
    @ReflectiveAccess public static class Other 
               extends UnirestOutputHelperMixins.Other {}
    
    @ReflectiveAccess public static class Ping extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "ping";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.details(); 
    }
    
    @ReflectiveAccess public static class StartPackageScan extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "package";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess public static class StartMbsScan extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "mbs";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }

}
