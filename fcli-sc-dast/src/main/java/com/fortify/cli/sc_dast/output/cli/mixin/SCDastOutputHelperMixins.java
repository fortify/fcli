package com.fortify.cli.sc_dast.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.sc_dast.output.cli.mixin.SCDastOutputHelperMixins.SCDastProductHelper;
import com.fortify.cli.sc_dast.rest.helper.SCDastInputTransformer;
import com.fortify.cli.sc_dast.rest.helper.SCDastPagingHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>This class provides standard, SC-DAST specific {@link IUnirestOutputHelper} implementations,
 * replicating the product-agnostic {@link IUnirestOutputHelper} implementations provided in 
 * {@link UnirestOutputHelperMixins}, adding product-specific functionality through the
 * {@link ProductHelperClass} annotation on this enclosing class. In addition to the
 * {@link IUnirestOutputHelper} implementations provided by the common {@link UnirestOutputHelperMixins},
 * this class defines some additional implementations specific for SC DAST.</p>
 * 
 * @author rsenden
 */

@ProductHelperClass(SCDastProductHelper.class)
public class SCDastOutputHelperMixins {
    @ReflectiveAccess
    public static class SCDastProductHelper implements IProductHelper, IInputTransformerSupplier, INextPageUrlProducerSupplier {
        @Getter @Setter private IUnirestOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = SCDastInputTransformer::getItems;
        
        @Override
        public INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
            return SCDastPagingHelper.nextPageUrlProducer(originalRequest);
        }
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
    
     public static class ScanActionComplete extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "complete";
    }
    
     public static class ScanActionImportFindings extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-findings";
    }
    
     public static class ScanActionPause extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "pause";
    }
    
     public static class ScanActionPublish extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "publish";
    }
    
     public static class ScanActionResume extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "resume";
    }
}
