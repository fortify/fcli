package com.fortify.cli.fod.output.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.output.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IInputTransformerSupplier;
import com.fortify.cli.common.output.cli.mixin.spi.product.IProductHelper;
import com.fortify.cli.common.output.cli.mixin.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.cli.mixin.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins.FoDProductHelper;
import com.fortify.cli.fod.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod.rest.helper.FoDPagingHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>This class provides standard, FoD-specific {@link IOutputHelper} implementations,
 * replicating the product-agnostic {@link IOutputHelper} implementations provided in 
 * {@link OutputHelperMixins}, adding product-specific functionality through the
 * {@link ProductHelperClass} annotation on this enclosing class. In addition to the
 * {@link IOutputHelper} implementations provided by the common {@link OutputHelperMixins},
 * this class may define some additional implementations specific for FoD.</p>
 * 
 * @author rsenden
 */
@ReflectiveAccess
@ProductHelperClass(FoDProductHelper.class)
public class FoDOutputHelperMixins {
    public static class FoDProductHelper implements IProductHelper, IInputTransformerSupplier, INextPageUrlProducerSupplier {
        @Getter @Setter private IOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = FoDInputTransformer::getItems;
        
        @Override
        public INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
            return FoDPagingHelper.nextPageUrlProducer(originalRequest);
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
}
