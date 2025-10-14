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
package com.fortify.cli.ssc._common.rest.ssc.bulk;

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
import com.fortify.cli.common.exception.FcliTechnicalException;

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
            throw new FcliTechnicalException(String.format("Request name '%s' was already added to bulk request", name));
        }
        String uri = request.getUrl();
        nameToIndexMap.put(name, requests.size());
        ObjectNode bulkEntry = objectMapper.createObjectNode();
        bulkEntry.put("uri", uri);
        bulkEntry.put("httpVerb", request.getHttpMethod().name());
        Optional<Body> optionalBody = request.getBody();
        if ( optionalBody.isPresent() ) {
            Body body = optionalBody.get();
            if ( body.isMultiPart() ) { throw new FcliTechnicalException("Multipart bodies are not supported for bulk requests"); }
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
     * methods. To avoid gateway or read timeouts if SSC is slow to respond on
     * large bulk requests, the requests are executed in batches of 10.
     * 
     * @return {@link SSCBulkResponse} containing the results for each of the requests in the bulk request
     */
    public SSCBulkResponse execute(UnirestInstance unirest) {
        int batchSize = 10;
        int totalRequests = requests.size();
        String[] indexToName = buildIndexToNameMap(totalRequests);
        Map<String, ObjectNode> nameToResponseMap = new HashMap<>();
        for (var batch : batches(totalRequests, batchSize)) {
            ArrayNode batchRequests = getBatchRequests(batch.start, batch.end);
            JsonNode batchResponse = sendBatch(unirest, batchRequests);
            mapBatchResponses(nameToResponseMap, batchResponse, indexToName, batch.start);
        }
        var result = new SSCBulkResponse(nameToResponseMap);
        consumers.forEach((k, v) -> v.accept(result.data(k)));
        return result;
    }

    private String[] buildIndexToNameMap(int totalRequests) {
        String[] indexToName = new String[totalRequests];
        nameToIndexMap.forEach((name, idx) -> indexToName[idx] = name);
        return indexToName;
    }

    private record Batch(int start, int end) {}

    private java.util.List<Batch> batches(int totalRequests, int batchSize) {
        return java.util.stream.IntStream.range(0, totalRequests)
                .filter(i -> i % batchSize == 0)
                .mapToObj(i -> new Batch(i, Math.min(i + batchSize, totalRequests)))
                .toList();
    }

    private ArrayNode getBatchRequests(int start, int end) {
        var batchRequests = objectMapper.createArrayNode();
        for (int j = start; j < end; j++) {
            batchRequests.add(requests.get(j));
        }
        return batchRequests;
    }

    private JsonNode sendBatch(UnirestInstance unirest, ArrayNode batchRequests) {
        var bulkRequest = objectMapper.createObjectNode();
        bulkRequest.set("requests", batchRequests);
        return unirest.post("/api/v1/bulk").body(bulkRequest)
                .asObject(JsonNode.class).getBody().get("data");
    }

    private void mapBatchResponses(Map<String, ObjectNode> nameToResponseMap, JsonNode batchResponse, String[] indexToName, int batchStart) {
        if (batchResponse != null && batchResponse.isArray()) {
            for (int j = 0; j < batchResponse.size(); j++) {
                nameToResponseMap.put(indexToName[batchStart + j], getResponseBody(batchResponse.get(j)));
            }
        }
    }

    private ObjectNode getResponseBody(JsonNode rawResponse) {
        if (rawResponse == null || !rawResponse.has("responses") || !rawResponse.get("responses").isArray() || rawResponse.get("responses").isEmpty()) {
            return null;
        }
        return (ObjectNode) rawResponse.get("responses").get(0).get("body");
    }

    public static final class SSCBulkResponse {
        private final Map<String, ObjectNode> nameToResponseMap;

        private SSCBulkResponse(Map<String, ObjectNode> nameToResponseMap) {
            this.nameToResponseMap = nameToResponseMap;
        }
        
        public ObjectNode body(String requestName) {
            return nameToResponseMap.get(requestName);
        }
        
        public JsonNode data(String requestName) {
            ObjectNode body = body(requestName);
            return body != null ? body.get("data") : null;
        }
    }
}