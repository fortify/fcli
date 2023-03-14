package com.fortify.cli.fod.output.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
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
 * <p>This class provides standard, FoD-specific {@link IUnirestOutputHelper} implementations,
 * replicating the product-agnostic {@link IUnirestOutputHelper} implementations provided in 
 * {@link UnirestOutputHelperMixins}, adding product-specific functionality through the
 * {@link ProductHelperClass} annotation on this enclosing class. In addition to the
 * {@link IUnirestOutputHelper} implementations provided by the common {@link UnirestOutputHelperMixins},
 * this class may define some additional implementations specific for FoD.</p>
 * 
 * @author rsenden
 */
@ReflectiveAccess
@ProductHelperClass(FoDProductHelper.class)
public class FoDOutputHelperMixins {
    public static class FoDProductHelper implements IProductHelper, IInputTransformerSupplier, INextPageUrlProducerSupplier {
        @Getter @Setter private IUnirestOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = FoDInputTransformer::getItems;
        
        @Override
        public INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
            return FoDPagingHelper.nextPageUrlProducer(originalRequest);
        }
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

    @ReflectiveAccess public static class WaitFor
            extends BasicOutputHelperMixins.WaitFor {}

    @ReflectiveAccess public static class Upload 
               extends UnirestOutputHelperMixins.Upload {}
    
    @ReflectiveAccess public static class Download 
               extends UnirestOutputHelperMixins.Download {}
    
    @ReflectiveAccess public static class Install 
               extends UnirestOutputHelperMixins.Install {}
    
    @ReflectiveAccess public static class Uninstall 
               extends UnirestOutputHelperMixins.Uninstall {}

    @ReflectiveAccess public static class Import
            extends UnirestOutputHelperMixins.Import {}

    @ReflectiveAccess public static class Export
            extends UnirestOutputHelperMixins.Export {}

    @ReflectiveAccess public static class Setup
            extends UnirestOutputHelperMixins.Setup {}

    @ReflectiveAccess public static class Other 
               extends UnirestOutputHelperMixins.Other {}
}
