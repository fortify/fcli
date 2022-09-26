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
package com.fortify.cli.ssc.auth_entity.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.auth_entity.helper.SSCAuthEntitySpecPredicate.MatchMode;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;

public final class SSCAuthEntitiesHelper {
    private final UnirestInstance unirest;
    private ArrayNode allAuthEntities;
    
    public SSCAuthEntitiesHelper(UnirestInstance unirest) {
        this.unirest = unirest;
    }
    
    public final ArrayNode getAllAuthEntities() {
        if ( allAuthEntities==null ) {
            allAuthEntities = (ArrayNode)unirest.get(SSCUrls.AUTH_ENTITIES)
                    .queryString("limit","-1").asObject(JsonNode.class).getBody().get("data");
        }
        return allAuthEntities;
    }
    
    public final ArrayNode getMatchingAuthEntities(boolean allowMultipleMatches, boolean failOnUnmatched, String... authEntitySpecs) {
        SSCAuthEntitySpecPredicate predicate = new SSCAuthEntitySpecPredicate(authEntitySpecs, MatchMode.INCLUDE, allowMultipleMatches);
        ArrayNode result = JsonHelper.stream(getAllAuthEntities())
                    .filter(predicate)
                    .collect(JsonHelper.arrayNodeCollector());
        if ( failOnUnmatched ) {
            predicate.checkUnmatched();
        }
        return result;
    }
}
