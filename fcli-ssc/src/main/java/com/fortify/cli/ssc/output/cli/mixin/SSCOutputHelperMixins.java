package com.fortify.cli.ssc.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.cli.mixin.writer.OutputWriterWithQueryFactoryMixin;
import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.request.IHttpRequestUpdater;
import com.fortify.cli.common.output.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
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
import picocli.CommandLine.Mixin;

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
@ReflectiveAccess
@ProductHelperClass(SSCProductHelper.class)
public class SSCOutputHelperMixins {
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
    
    @ReflectiveAccess public static class Add 
               extends UnirestOutputHelperMixins.Add {}
    
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
    
    @ReflectiveAccess public static class Download 
               extends UnirestOutputHelperMixins.Download {}
    
    @ReflectiveAccess public static class Upload 
               extends UnirestOutputHelperMixins.Upload {}
    
    @ReflectiveAccess public static class Install 
               extends UnirestOutputHelperMixins.Install {}
    
    @ReflectiveAccess public static class Uninstall 
               extends UnirestOutputHelperMixins.Uninstall {}
    
    @ReflectiveAccess public static class Other 
               extends UnirestOutputHelperMixins.Other {}
    
    @ReflectiveAccess public static class ArtifactApprove extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "approve";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @Command(aliases = "download-by-id")
    @ReflectiveAccess public static class ArtifactDownloadById 
                extends UnirestOutputHelperMixins.Download {}
    
    @ReflectiveAccess public static class ArtifactDownloadState extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "download-state";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @Command(aliases = "purge-by-id")
    @ReflectiveAccess public static class ArtifactPurgeById extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "purge";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess public static class ArtifactPurgeOlderThan extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "purge-older-than";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess public static class ImportDebricked extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "import-debricked";
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
    
    @ReflectiveAccess public static class VulnCount extends UnirestOutputHelperMixins.Other {
        public static final String CMD_NAME = "count";
        @Getter @Mixin private OutputWriterWithQueryFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table(); 
    }
}
