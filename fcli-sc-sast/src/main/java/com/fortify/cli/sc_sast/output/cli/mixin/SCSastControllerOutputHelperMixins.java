package com.fortify.cli.sc_sast.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerOutputHelperMixins.SCSastProductHelper;
import com.fortify.cli.sc_sast.rest.helper.SCSastInputTransformer;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;

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
@ProductHelperClass(SCSastProductHelper.class)
public class SCSastControllerOutputHelperMixins {
    @ReflectiveAccess
    public static class SCSastProductHelper implements IProductHelper, IInputTransformerSupplier {
        @Getter @Setter private IUnirestOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = SCSastInputTransformer::getItems;
    }
    
     public static class Create 
               extends UnirestOutputHelperMixins.Create {}
    
     public static class Delete 
               extends UnirestOutputHelperMixins.Delete {}
    
     public static class List 
               extends UnirestOutputHelperMixins.List {}
    
     public static class Get 
               extends UnirestOutputHelperMixins.Get {}
    
     public static class Set 
               extends UnirestOutputHelperMixins.Set {}
    
     public static class Update 
               extends UnirestOutputHelperMixins.Update {}
    
     public static class Enable 
               extends UnirestOutputHelperMixins.Enable {}
    
     public static class Disable 
               extends UnirestOutputHelperMixins.Disable {}
    
     public static class Start 
               extends UnirestOutputHelperMixins.Start {}
    
     public static class Pause 
               extends UnirestOutputHelperMixins.Pause {}
    
     public static class Resume 
               extends UnirestOutputHelperMixins.Resume {}
    
     public static class Cancel 
               extends UnirestOutputHelperMixins.Cancel {}
    
     public static class Upload 
               extends UnirestOutputHelperMixins.Upload {}
    
     public static class Download 
               extends UnirestOutputHelperMixins.Download {}
    
     public static class Install 
               extends UnirestOutputHelperMixins.Install {}
    
     public static class Uninstall 
               extends UnirestOutputHelperMixins.Uninstall {}
}
