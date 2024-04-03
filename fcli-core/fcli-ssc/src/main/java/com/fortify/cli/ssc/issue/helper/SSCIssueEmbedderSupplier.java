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
package com.fortify.cli.ssc.issue.helper;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.ssc._common.rest.bulk.ISSCEntityEmbedder;
import com.fortify.cli.ssc._common.rest.bulk.ISSCEntityEmbedderSupplier;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.helper.SSCInputTransformer;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SSCIssueEmbedderSupplier implements ISSCEntityEmbedderSupplier {
    details(SSCIssueDetailsEmbedder::new),
    comments(SSCIssueCommentsEmbedder::new),
    auditHistory(SSCIssueAuditHistoryEmbedder::new),
    ;
    
    private final Supplier<ISSCEntityEmbedder> supplier;
    
    public ISSCEntityEmbedder createEntityEmbedder() {
        return supplier.get();
    }
    
    private static abstract class AbstractSSCIssueEmbedder implements ISSCEntityEmbedder {
        @Override
        public void addEmbedRequests(SSCBulkRequestBuilder builder, UnirestInstance unirest, JsonNode record) {
            var id = record.get("id").asText();
            builder.request(getBaseRequest(unirest)
                    .routeParam("id", id)
                    .queryString("limit", "-1"), 
                response->process((ObjectNode)record, SSCInputTransformer.getDataOrSelf(response)));
        }

        protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest);
        protected abstract void process(ObjectNode record, JsonNode response);
    }
    
    private static final class SSCIssueDetailsEmbedder extends AbstractSSCIssueEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/issueDetails/{id}");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("details", response); 
        }
    }
    
    private static final class SSCIssueAuditHistoryEmbedder extends AbstractSSCIssueEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/issues/{id}/auditHistory?limit=-1");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("auditHistory", response); 
        }
    }
    
    private static final class SSCIssueCommentsEmbedder extends AbstractSSCIssueEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/issues/{id}/comments?limit=-1");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("comments", response); 
        }
    }
}
