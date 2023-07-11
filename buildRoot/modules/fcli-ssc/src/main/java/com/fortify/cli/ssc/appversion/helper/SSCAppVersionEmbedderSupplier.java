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
package com.fortify.cli.ssc.appversion.helper;

import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.ssc._common.rest.bulk.ISSCEntityEmbedder;
import com.fortify.cli.ssc._common.rest.bulk.ISSCEntityEmbedderSupplier;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.helper.SSCInputTransformer;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAppVersionAttributeHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SSCAppVersionEmbedderSupplier implements ISSCEntityEmbedderSupplier {
    attrs(SSCAppVersionAttributesEmbedder::new),
    attrsByName(SSCAppVersionAttributesByNameEmbedder::new),
    attrsByGuid(SSCAppVersionAttributesByGuidEmbedder::new),
    attrValuesByName(SSCAppVersionAttributeValuesByNameEmbedder::new),
    attrValuesByGuid(SSCAppVersionAttributeValuesByGuidEmbedder::new),
    bugtracker(SSCAppVersionBugTrackerEmbedder::new),
    customTags(SSCAppVersionCustomTagsEmbedder::new),
    filterSets(SSCAppVersionFilterSetsEmbedder::new),
    folders(SSCAppVersionFoldersEmbedder::new),
    // SSC 22.2 always seems to return an empty array, so no use in embedding this
    //pluginEngineTypes(SSCAppVersionPluginEngineTypesEmbedder::new),
    resultProcessingRules(SSCAppVersionResultProcessingRulesEmbedder::new)
    ;
    
    private final Supplier<ISSCEntityEmbedder> supplier;
    
    public ISSCEntityEmbedder createEntityEmbedder() {
        return supplier.get();
    }
    
    private static abstract class AbstractSSCAppVersionEmbedder implements ISSCEntityEmbedder {
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
    
    private static final class SSCAppVersionBugTrackerEmbedder extends AbstractSSCAppVersionEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/projectVersions/{id}/bugtracker");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("bugtracker", response==null ? null : response.get(0));   
        }
    }
    
    private static final class SSCAppVersionCustomTagsEmbedder extends AbstractSSCAppVersionEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/projectVersions/{id}/customTags");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("customTags", response);   
        }
    }
    
    private static final class SSCAppVersionFilterSetsEmbedder extends AbstractSSCAppVersionEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/projectVersions/{id}/filterSets");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("filterSets", response);   
        }
    }
    
    private static final class SSCAppVersionFoldersEmbedder extends AbstractSSCAppVersionEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/projectVersions/{id}/folders");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("folders", response);   
        }
    }
    
    /*
    private static final class SSCAppVersionPluginEngineTypesEmbedder extends AbstractSSCAppVersionEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/projectVersions/{id}/pluginEngineTypes");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("pluginEngineTypes", response);   
        }
    }
    */
    
    private static final class SSCAppVersionResultProcessingRulesEmbedder extends AbstractSSCAppVersionEmbedder {
        @Override
        protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
            return unirest.get("/api/v1/projectVersions/{id}/resultProcessingRules");
        }
        @Override
        protected void process(ObjectNode record, JsonNode response) {
            record.set("resultProcessingRules", response);   
        }
    }
    
    private static abstract class AbstractSSCAppVersionAttributeEmbedder implements ISSCEntityEmbedder {
        @Getter private SSCAppVersionAttributeHelper attributeHelper;
        @Override
        public void addEmbedRequests(SSCBulkRequestBuilder builder, UnirestInstance unirest, JsonNode record) {
            if ( attributeHelper==null ) {
                // We need to include a unique identifier in the request name in case
                // multiple attribute embedders are enabled, otherwise the consumer 
                // to initialize attributeHelper will only be invoked for the first embedder 
                var attrDefsRequestName = this.hashCode()+"_attrDefs";
                if ( !builder.hasRequest(attrDefsRequestName) ) {
                    builder.request(attrDefsRequestName, 
                        SSCAppVersionAttributeHelper.getAttributeDefinitionsRequest(unirest),
                        attrDefs->this.attributeHelper=new SSCAppVersionAttributeHelper(attrDefs));
                }
            }
            var id = record.get("id").asText();
            builder.request(unirest
                    .get("/api/v1/projectVersions/{id}/attributes")
                    .routeParam("id", id), attrs->update((ObjectNode)record, attrs));
        }
        
        protected abstract void update(ObjectNode record, JsonNode attrs);
    }
    
    private static final class SSCAppVersionAttributesEmbedder extends AbstractSSCAppVersionAttributeEmbedder {
        @Override
        protected void update(ObjectNode record, JsonNode attrs) {
            record.set("attrs", getAttributeHelper().mergeAttributeDefinitions(attrs));
        }
    }
    
    private static final class SSCAppVersionAttributesByNameEmbedder extends AbstractSSCAppVersionAttributeEmbedder {
        @Override
        protected void update(ObjectNode record, JsonNode attrs) {
            record.set("attrsByName", getAttributeHelper().getAttributesBy(attrs, "name", Function.identity()));
        }
    }
    
    private static final class SSCAppVersionAttributesByGuidEmbedder extends AbstractSSCAppVersionAttributeEmbedder {
        @Override
        protected void update(ObjectNode record, JsonNode attrs) {
            record.set("attrsByGuid", getAttributeHelper().getAttributesBy(attrs, "guid", SSCAppVersionAttributeHelper::getValuesForAttribute));
        }
    }
    
    private static final class SSCAppVersionAttributeValuesByNameEmbedder extends AbstractSSCAppVersionAttributeEmbedder {
        @Override
        protected void update(ObjectNode record, JsonNode attrs) {
            record.set("attrValuesByName", getAttributeHelper().getAttributesBy(attrs, "name", SSCAppVersionAttributeHelper::getValuesForAttribute));
        }
    }
    
    private static final class SSCAppVersionAttributeValuesByGuidEmbedder extends AbstractSSCAppVersionAttributeEmbedder {
        @Override
        protected void update(ObjectNode record, JsonNode attrs) {
            record.set("attrValuesByGuid", getAttributeHelper().getAttributesBy(attrs, "guid", Function.identity()));
        }
    }
    
    
}
