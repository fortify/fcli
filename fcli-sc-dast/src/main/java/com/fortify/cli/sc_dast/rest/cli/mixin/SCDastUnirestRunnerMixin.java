package com.fortify.cli.sc_dast.rest.cli.mixin;

import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.ssc.rest.cli.mixin.SSCUnirestRunnerMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public class SCDastUnirestRunnerMixin {
    @Inject private UnirestRunner runner;
    @Mixin private SSCUnirestRunnerMixin sscUnirestRunnerMixin;
    
    public <R> R run(Function<UnirestInstance, R> f) {
        return sscUnirestRunnerMixin.run(sscUnirest->runner.run(scDastUnirest->run(sscUnirest, scDastUnirest, f)));
    }
    
    private <R> R run(UnirestInstance sscUnirest, UnirestInstance scDastUnirest, Function<UnirestInstance, R> f) {
        configure(sscUnirest, scDastUnirest);
        try {
            return f.apply(scDastUnirest);
        } finally {
            cleanup(scDastUnirest);
        }
    }

    private void configure(UnirestInstance sscUnirest, UnirestInstance scDastUnirest) {
        String scDastApiUrl = getSCDastApiUrlFromSSC(sscUnirest);
        String authHeader = sscUnirest.config().getDefaultHeaders().get("Authorization").stream().filter(h->h.startsWith("FortifyToken")).findFirst().orElseThrow();
        scDastUnirest.config()
            .defaultBaseUrl(scDastApiUrl)
            .setDefaultHeader("Authorization", authHeader)
            .verifySsl(sscUnirest.config().isVerifySsl());
        UnirestUnexpectedHttpResponseConfigurer.configure(scDastUnirest);
    }
    
    private void cleanup(UnirestInstance unirest) {
        // TODO With current implementation, we just reuse the token from SSCUnirestRunnerMixin,
        //      so we don't need to do any token cleanup. However, eventually we may have dedicated
        //      SC DAST sessions, in which case we may need to do some cleanup here.
    }
    
    private String getSCDastApiUrlFromSSC(UnirestInstance sscUnirest) {
        ArrayNode properties = getSCDastConfigurationProperties(sscUnirest);
        checkSCDastIsEnabled(properties);
        String scDastUrl = getSCDastUrlFromProperties(properties);
        return normalizeSCDastUrl(scDastUrl);
    }

    private final ArrayNode getSCDastConfigurationProperties(UnirestInstance sscUnirest) {
        ObjectNode configData = sscUnirest.get("/api/v1/configuration?group=edast")
                .asObject(ObjectNode.class)
                .getBody(); 
        
        return JsonHelper.evaluateJsonPath(configData, "$.data.properties", ArrayNode.class);
    }
    
    private void checkSCDastIsEnabled(ArrayNode properties) {
        boolean scDastEnabled = JsonHelper.evaluateJsonPath(properties, "$.[?(@.name=='edast.enabled')].value", Boolean.class);
        if (!scDastEnabled) {
            throw new IllegalStateException("ScanCentral DAST must be enabled in SSC");
        }
    }
    
    private String getSCDastUrlFromProperties(ArrayNode properties) {
        String scDastUrl = JsonHelper.evaluateJsonPath(properties, "$.[?(@.name=='edast.server.url')].value", String.class);
        if ( scDastUrl.isEmpty() ) {
            throw new IllegalStateException("SSC returns an empty ScanCentral DAST URL");
        }
        return scDastUrl;
    }
    
    private String normalizeSCDastUrl(String scDastUrl) {
        // We remove '/api' and any trailing slashes from the URL as most users will specify relative URL's starting with /api/v2/...
        return scDastUrl.replaceAll("/api/?$","").replaceAll("/+$", "");
    }
    
}
