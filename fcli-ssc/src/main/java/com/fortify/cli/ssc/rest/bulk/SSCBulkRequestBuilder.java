/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.rest.bulk;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.Body;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

/**
 * This class allows for building and executing SSC bulk requests
 */
public class SSCBulkRequestBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ArrayNode requests = objectMapper.createArrayNode();
    private final Map<String,Integer> nameToIndexMap = new HashMap<>();
    private final Map<String, Consumer<JsonNode>> consumers = new LinkedHashMap<>();
    
    /**
     * Check whether this SSCBulkRequestBuilder instance already has a request
     * with the given name.
     * @param name to check
     * @return true if a request with the given name is already present, false otherwise
     */
    public boolean hasRequest(String name) {
        return nameToIndexMap.containsKey(name);
    }
    
    /**
     * Add a request to the list of bulk requests to be executed. When the
     * bulk request is executed by calling the {@link #execute(UnirestInstance)}
     * method, the given {@link Consumer} will be invoked with the response
     * data for the given request. Consumers are invoked in the order they were
     * added.
     * 
     * @param request {@link HttpRequest} to be added to the list of bulk requests
     * @param consumer {@link Consumer} to be invoked for consuming the response of the request
     * @return Self for chaining
     */
    public SSCBulkRequestBuilder request(HttpRequest<?> request, Consumer<JsonNode> consumer) {
        return request("_consumableRequest."+consumers.size(), request, consumer);
    }
    
    /**
     * Add a request to the list of bulk requests to be executed. When the
     * bulk request is executed by calling the {@link #execute(UnirestInstance)}
     * method, the given {@link Consumer} will be invoked with the response
     * data for the given request. Consumers are invoked in the order they were
     * added.
     * 
     * @param name for the request
     * @param request {@link HttpRequest} to be added to the list of bulk requests
     * @param consumer {@link Consumer} to be invoked for consuming the response of the request
     * @return Self for chaining
     */
    public SSCBulkRequestBuilder request(String name, HttpRequest<?> request, Consumer<JsonNode> consumer) {
        consumers.put(name, consumer);
        request(name, request);
        return this;
    }
    
    /**
     * Add a request to the list of bulk requests to be executed, identified
     * by the given name. If a request with the given name has already been 
     * added, an {@link IllegalArgumentException} will be thrown.
     * 
     * @param name for this request
     * @param request {@link HttpRequest} to be added to the list of bulk requests
     * @return Self for chaining
     */
    public SSCBulkRequestBuilder request(String name, HttpRequest<?> request) {
        if ( request==null ) { return this; }
        if ( nameToIndexMap.containsKey(name) ) {
            throw new IllegalArgumentException(String.format("Request name '%s' was already added to bulk request", name));
        }
        String uri = request.getUrl();
        nameToIndexMap.put(name, requests.size());
        ObjectNode bulkEntry = objectMapper.createObjectNode();
        bulkEntry.put("uri", uri);
        bulkEntry.put("httpVerb", request.getHttpMethod().name());
        Optional<Body> optionalBody = request.getBody();
        if ( optionalBody.isPresent() ) {
            Body body = optionalBody.get();
            if ( body.isMultiPart() ) { throw new IllegalArgumentException("Multipart bodies are not supported for bulk requests"); }
            Object bodyValue = body.uniPart().getValue();
            if ( bodyValue instanceof String ) {
                // If bodyValue is a String, we expect this to be already serialized JSON
                bulkEntry.putRawValue("postData", new RawValue((String)bodyValue));
            } else if ( bodyValue instanceof JsonNode ) {
                bulkEntry.set("postData", (JsonNode)bodyValue);
            } else {
                bulkEntry.set("postData", objectMapper.valueToTree(bodyValue));
            }
        }
        requests.add(bulkEntry);
        return this;
    }
    
    /**
     * Execute the bulk requests that were previously added using the 
     * {@link #request(String, String)} or {@link #request(String, String, Object)}
     * methods.
     * 
     * @return {@link SSCBulkResponse} containing the results for each of the requests in the bulk request
     */
    public SSCBulkResponse execute(UnirestInstance unirest) {
        ObjectNode bulkRequest = objectMapper.createObjectNode();
        bulkRequest.set("requests", this.requests);
        var result = new SSCBulkResponse(nameToIndexMap, 
                unirest.post("/api/v1/bulk").body(bulkRequest)
                .asObject(JsonNode.class).getBody().get("data"));
        consumers.entrySet().forEach(e->
            e.getValue().accept(result.data(e.getKey())));
        return result;
    }
    
    public static final class SSCBulkResponse {
        private final JsonNode bulkResponse;
        private final Map<String, Integer> nameToIndexMap;

        private SSCBulkResponse(Map<String, Integer> nameToIndexMap, JsonNode bulkResponse) {
            this.nameToIndexMap = nameToIndexMap;
            this.bulkResponse = bulkResponse;
        }
        
        public JsonNode fullBody() {
            return bulkResponse;
        }
        
        public ObjectNode body(String requestName) {
            Integer index = nameToIndexMap.get(requestName);
            // TODO Calling 'get(%s)' works, but ideally we should be able to use
            //      standard SpEL indexing '[%s]'. Any way to make this work? The
            //      SpEL Indexer class seems to explicitly check for arrays or 
            //      collections, and throws an exception if we use '[%s]'.
            String path = String.format("get(%s).responses[0].body", index);
            return JsonHelper.evaluateSpelExpression(bulkResponse, path, ObjectNode.class);
        }
        
        public JsonNode data(String requestName) {
            return body(requestName).get("data");
        }
    }
}