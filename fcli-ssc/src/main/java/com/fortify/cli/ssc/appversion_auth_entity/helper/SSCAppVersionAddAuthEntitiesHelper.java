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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.util.JsonHelper;
import com.fortify.cli.ssc.auth_entity.helper.SSCAuthEntitySpecPredicate;
import com.fortify.cli.ssc.auth_entity.helper.SSCAuthEntitySpecPredicate.MatchMode;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public final class SSCAppVersionAddAuthEntitiesHelper {
    public static final HttpRequest<?> generateUpdateRequest(UnirestInstance unirest, String appVersionId, String[] authEntitySpecs, boolean allowMultipleMatches) {
        return unirest
                .put(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .body(generateBody(unirest, appVersionId, authEntitySpecs, allowMultipleMatches));
    }
    
    private static final ArrayNode generateBody(UnirestInstance unirest, String appVersionId, String[] authEntitySpecs, boolean allowMultipleMatches) {
        SSCBulkResponse authEntityData = new SSCBulkRequestBuilder()
            .request("assigned", unirest.get(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId)).queryString("limit","-1"))
            .request("all", unirest.get(SSCUrls.AUTH_ENTITIES).queryString("limit","-1"))
            .execute(unirest);
        ArrayNode result = (ArrayNode)authEntityData.data("assigned");
        ArrayNode allAuthEntities = (ArrayNode)authEntityData.data("all");
        result.addAll(JsonHelper.stream(allAuthEntities)
                .filter(new SSCAuthEntitySpecPredicate(authEntitySpecs, MatchMode.INCLUDE, allowMultipleMatches))
                .collect(JsonHelper.arrayNodeCollector()));
        return result;
    }
}
