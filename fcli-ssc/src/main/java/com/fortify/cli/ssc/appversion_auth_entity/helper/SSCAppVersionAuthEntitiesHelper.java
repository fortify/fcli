/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.appversion_auth_entity.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.auth_entity.helper.SSCAuthEntitySpecPredicate;
import com.fortify.cli.ssc.auth_entity.helper.SSCAuthEntitySpecPredicate.MatchMode;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public final class SSCAppVersionAuthEntitiesHelper {
    private final UnirestInstance unirest;
    private final String appVersionId;
    private ArrayNode currentAuthEntities;
    private ArrayNode allAuthEntities;
    
    public SSCAppVersionAuthEntitiesHelper(UnirestInstance unirest, String appVersionId) {
        this.unirest = unirest;
        this.appVersionId = appVersionId;
        this.currentAuthEntities = (ArrayNode)unirest.get(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .queryString("limit","-1").asObject(JsonNode.class).getBody().get("data");
    }
    
    public final HttpRequest<?> generateUpdateRequest() {
        return unirest
                .put(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .body(currentAuthEntities);
    }
    
    public final SSCAppVersionAuthEntitiesHelper add(boolean allowMultipleMatches, String... authEntitySpecs) {
        if ( allAuthEntities==null ) {
            allAuthEntities = (ArrayNode)unirest.get(SSCUrls.AUTH_ENTITIES)
                    .queryString("limit","-1").asObject(JsonNode.class).getBody().get("data");
        }
        currentAuthEntities.addAll(
                JsonHelper.stream(allAuthEntities)
                    .filter(new SSCAuthEntitySpecPredicate(authEntitySpecs, MatchMode.INCLUDE, allowMultipleMatches))
                    .collect(JsonHelper.arrayNodeCollector()));
        return this;
    }
    
    public final SSCAppVersionAuthEntitiesHelper remove(boolean allowMultipleMatches, String... authEntitySpecs) {
        currentAuthEntities = JsonHelper.stream(currentAuthEntities)
                .filter(new SSCAuthEntitySpecPredicate(authEntitySpecs, MatchMode.EXCLUDE, allowMultipleMatches))
                .collect(JsonHelper.arrayNodeCollector());
        return this;
    }
}
