package com.fortify.cli.scm.github.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.scm.github.cli.mixin.GitHubOutputHelperMixins.GitHubProductHelper;
import com.fortify.cli.scm.github.cli.util.GitHubPagingHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>This class provides standard, GitHub-specific {@link IUnirestOutputHelper} implementations,
 * replicating the product-agnostic {@link IUnirestOutputHelper} implementations provided in 
 * {@link UnirestOutputHelperMixins}, adding product-specific functionality through the
 * {@link ProductHelperClass} annotation on this enclosing class. In addition to the
 * {@link IUnirestOutputHelper} implementations provided by the common {@link UnirestOutputHelperMixins},
 * this class may define some additional implementations specific for GitHub.</p>
 * 
 * @author rsenden
 */
@ProductHelperClass(GitHubProductHelper.class)
public class GitHubOutputHelperMixins {
    @ReflectiveAccess
    public static class GitHubProductHelper implements IProductHelper, INextPageUrlProducerSupplier/*, IInputTransformerSupplier, INextPageUrlProducerSupplier, IHttpRequestUpdater*/ {
        @Getter @Setter private IUnirestOutputHelper outputHelper;
        
        @Override
        public INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
            return GitHubPagingHelper.nextPageUrlProducer();
        }
        
        /*
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
        */
    }
    
     public static class List 
               extends UnirestOutputHelperMixins.List {}
    
}
