package com.fortify.cli.ssc.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.request.IHttpRequestUpdater;
import com.fortify.cli.common.output.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins.SSCProductHelper;
import com.fortify.cli.ssc.rest.helper.SSCInputTransformer;
import com.fortify.cli.ssc.rest.helper.SSCPagingHelper;
import com.fortify.cli.ssc.rest.query.ISSCQParamGeneratorSupplier;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Command;

/**
 * <p>This class provides standard, SSC-specific {@link IUnirestOutputHelper} implementations,
 * replicating the product-agnostic {@link IUnirestOutputHelper} implementations provided in 
 * {@link UnirestOutputHelperMixins}, adding product-specific functionality through the
 * {@link ProductHelperClass} annotation on this enclosing class. In addition to the
 * {@link IUnirestOutputHelper} implementations provided by the common {@link UnirestOutputHelperMixins},
 * this class may define some additional implementations specific for SSC.</p>
 * 
 * @author rsenden
 */
@ProductHelperClass(SSCProductHelper.class)
public class SSCOutputHelperMixins {
    @ReflectiveAccess
    public static class SSCProductHelper implements IProductHelper, IInputTransformerSupplier, INextPageUrlProducerSupplier, IHttpRequestUpdater {
        @Getter @Setter private IUnirestOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = SSCInputTransformer::getDataOrSelf;
        
        @Override
        public INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
            return SSCPagingHelper.nextPageUrlProducer();
        }
        
        @Override
        public final HttpRequest<?> updateRequest(UnirestInstance unirest, HttpRequest<?> request) {
            ISSCQParamGeneratorSupplier qParamGeneratorSupplier = outputHelper.getCommandAs(ISSCQParamGeneratorSupplier.class);
            if ( qParamGeneratorSupplier!=null ) {
                request = qParamGeneratorSupplier.getQParamGenerator().addQParam(outputHelper, request);
            }
            return request;
        }
    }
    
     public static class Add 
               extends UnirestOutputHelperMixins.Add {}
    
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
    
     public static class Download 
               extends UnirestOutputHelperMixins.Download {}
    
     public static class Upload 
               extends UnirestOutputHelperMixins.Upload {}
    
     public static class Install 
               extends UnirestOutputHelperMixins.Install {}
    
     public static class Uninstall 
               extends UnirestOutputHelperMixins.Uninstall {}
    
     public static class ArtifactApprove extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "approve";
    }
    
    @Command(aliases = "download-by-id")
     public static class ArtifactDownloadById 
                extends UnirestOutputHelperMixins.Download {}
    
     public static class ArtifactDownloadState extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "download-state"; 
    }
    
    @Command(aliases = "purge-by-id")
     public static class ArtifactPurgeById extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "purge";
    }
    
     public static class ArtifactPurgeOlderThan extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "purge-older-than";
    }
    
     public static class ImportDebricked extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-debricked";
    }
    
     public static class VulnCount extends UnirestOutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "count";
    }
    
    @Command(aliases = "gen-answer")
     public static class ReportTemplateGenerateAnswerFile extends BasicOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "generate-answerfile";
    }
}
