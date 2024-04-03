/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.issue.cli.mixin;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.cli.mixin.AbstractFoDEmbedMixin;
import com.fortify.cli.fod._common.rest.embed.IFoDEntityEmbedder;
import com.fortify.cli.fod._common.rest.embed.IFoDEntityEmbedderSupplier;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;

public class FoDIssueEmbedMixin extends AbstractFoDEmbedMixin {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = "--embed", required = false, split = ",", descriptionKey = "fcli.fod.issue.embed" )
    @Getter private FoDIssueEmbedderSupplier[] embedSuppliers;
    
    @RequiredArgsConstructor
    public static enum FoDIssueEmbedderSupplier implements IFoDEntityEmbedderSupplier {
        allData(FoDIssueAllDataEmbedder::new),
        summary(FoDIssueSummaryEmbedder::new),
        details(FoDIssueDetailsEmbedder::new),
        recommendations(FoDIssueRecommendationsEmbedder::new),
        history(FoDIssueHistoryEmbedder::new),
        requestResponse(FoDIssueRequestResponseEmbedder::new),
        headers(FoDIssueHeadersEmbedder::new),
        parameters(FoDIssueParametersEmbedder::new),
        traces(FoDIssueTracesEmbedder::new),
        ;
        
        private final Supplier<IFoDEntityEmbedder> supplier;
        
        public IFoDEntityEmbedder createEntityEmbedder() {
            return supplier.get();
        }
        
        private static abstract class AbstractFoDIssueEmbedder implements IFoDEntityEmbedder {
            @Override
            public void embed(UnirestInstance unirest, ObjectNode record) {
                var releaseId = record.get("releaseId").asText();
                var vulnId = record.get("vulnId").asText();
                JsonNode response = getBaseRequest(unirest)
                    .routeParam("releaseId", releaseId)    
                    .routeParam("vulnId", vulnId)
                    .queryString("limit", "-1")
                    .asObject(JsonNode.class)
                    .getBody();
                process(record, response);
            }

            protected abstract GetRequest getBaseRequest(UnirestInstance unirest);
            protected abstract void process(ObjectNode record, JsonNode response);
        }
        
        private static final class FoDIssueAllDataEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/all-data");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("allData", response); 
            }
        }
        
        private static final class FoDIssueSummaryEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/summary");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("summary", response); 
            }
        }
        
        private static final class FoDIssueDetailsEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/details");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("details", response); 
            }
        }
        
        private static final class FoDIssueRecommendationsEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/recommendations");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("recommendations", response); 
            }
        }
        
        private static final class FoDIssueHistoryEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/history");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("history", response); 
            }
        }
        
        private static final class FoDIssueRequestResponseEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/request-response");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("requestResponse", response); 
            }
        }
        
        private static final class FoDIssueHeadersEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/headers");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("headers", response); 
            }
        }
        
        private static final class FoDIssueParametersEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/parameters");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("parameters", response); 
            }
        }
        
        private static final class FoDIssueTracesEmbedder extends AbstractFoDIssueEmbedder {
            @Override
            protected GetRequest getBaseRequest(UnirestInstance unirest) {
                return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/traces");
            }
            @Override
            protected void process(ObjectNode record, JsonNode response) {
                record.set("traces", response); 
            }
        }
        
    }
}
