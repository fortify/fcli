package com.fortify.cli.sc_dast.output.cli.mixin;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.spi.IProductHelper;
import com.fortify.cli.sc_dast.rest.paging.SCDastPagingHelper;
import com.fortify.cli.sc_dast.util.SCDastInputTransformer;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;

public class SCDastOutputHelperMixins {
    public static class SCDastProductHelper implements IProductHelper {
        @Getter @Setter private IOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = SCDastInputTransformer::getItems;
        
        @Override
        public Function<HttpResponse<JsonNode>, String> getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
            return SCDastPagingHelper.nextPageUrlProducer(originalRequest);
        }
    }
    
    @ReflectiveAccess
    public static class List extends OutputHelperMixins.List {
        public List() { setProductHelper(new SCDastProductHelper()); }
    }
    
    @ReflectiveAccess
    public static class Get extends OutputHelperMixins.Get {
        public Get() { setProductHelper(new SCDastProductHelper()); }
    }
    
    @ReflectiveAccess
    public static class Enable extends OutputHelperMixins.Enable {
        public Enable() { setProductHelper(new SCDastProductHelper()); }
    }
    
    @ReflectiveAccess
    public static class Disable extends OutputHelperMixins.Disable {
        public Disable() { setProductHelper(new SCDastProductHelper()); }
    }
    
    @ReflectiveAccess
    public static class Other extends OutputHelperMixins.Other {
        public Other() { setProductHelper(new SCDastProductHelper()); }
    }
}
