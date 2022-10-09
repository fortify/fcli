package com.fortify.cli.sc_dast.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.output.IUnirestOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IInputTransformerSupplier;
import com.fortify.cli.common.output.cli.mixin.spi.product.IProductHelper;
import com.fortify.cli.common.output.cli.mixin.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.cli.mixin.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.output.writer.output.StandardOutputWriterFactory;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.sc_dast.output.cli.mixin.SCDastOutputHelperMixins.SCDastProductHelper;
import com.fortify.cli.sc_dast.rest.helper.SCDastInputTransformer;
import com.fortify.cli.sc_dast.rest.helper.SCDastPagingHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Mixin;

/**
 * <p>This class provides standard, SC-DAST specific {@link IUnirestOutputHelper} implementations,
 * replicating the product-agnostic {@link IUnirestOutputHelper} implementations provided in 
 * {@link OutputHelperMixins}, adding product-specific functionality through the
 * {@link ProductHelperClass} annotation on this enclosing class. In addition to the
 * {@link IUnirestOutputHelper} implementations provided by the common {@link OutputHelperMixins},
 * this class defines some additional implementations specific for SC DAST.</p>
 * 
 * @author rsenden
 */
@ReflectiveAccess
@ProductHelperClass(SCDastProductHelper.class)
public class SCDastOutputHelperMixins {
    public static class SCDastProductHelper implements IProductHelper, IInputTransformerSupplier, INextPageUrlProducerSupplier {
        @Getter @Setter private IUnirestOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = SCDastInputTransformer::getItems;
        
        @Override
        public INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
            return SCDastPagingHelper.nextPageUrlProducer(originalRequest);
        }
    }
    
    @ReflectiveAccess public static class Create 
               extends OutputHelperMixins.Create {}
    
    @ReflectiveAccess public static class Delete 
               extends OutputHelperMixins.Delete {}
    
    @ReflectiveAccess public static class List 
               extends OutputHelperMixins.List {}
    
    @ReflectiveAccess public static class Get 
               extends OutputHelperMixins.Get {}
    
    @ReflectiveAccess public static class Set 
               extends OutputHelperMixins.Set {}
    
    @ReflectiveAccess public static class Update 
               extends OutputHelperMixins.Update {}
    
    @ReflectiveAccess public static class Enable 
               extends OutputHelperMixins.Enable {}
    
    @ReflectiveAccess public static class Disable 
               extends OutputHelperMixins.Disable {}
    
    @ReflectiveAccess public static class Start 
               extends OutputHelperMixins.Start {}
    
    @ReflectiveAccess public static class Pause 
               extends OutputHelperMixins.Pause {}
    
    @ReflectiveAccess public static class Resume 
               extends OutputHelperMixins.Resume {}
    
    @ReflectiveAccess public static class Cancel 
               extends OutputHelperMixins.Cancel {}
    
    @ReflectiveAccess public static class Upload 
               extends OutputHelperMixins.Upload {}
    
    @ReflectiveAccess public static class Download 
               extends OutputHelperMixins.Download {}
    
    @ReflectiveAccess public static class Install 
               extends OutputHelperMixins.Install {}
    
    @ReflectiveAccess public static class Uninstall 
               extends OutputHelperMixins.Uninstall {}
    
    @ReflectiveAccess public static class Other 
               extends OutputHelperMixins.Other {}
    
    @ReflectiveAccess public static class ScanAction extends OutputHelperMixins.Other {
        @Getter @Mixin private StandardOutputWriterFactory outputWriterFactory;
        @Getter private OutputConfig basicOutputConfig = OutputConfig.table(); 
    }
}
