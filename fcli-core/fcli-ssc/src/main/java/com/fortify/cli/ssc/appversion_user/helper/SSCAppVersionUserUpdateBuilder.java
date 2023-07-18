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
package com.fortify.cli.ssc.appversion_user.helper;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.user.helper.SSCUserHelper;
import com.fortify.cli.ssc.user.helper.SSCUserSpecPredicate;
import com.fortify.cli.ssc.user.helper.SSCUserSpecPredicate.MatchMode;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Data;

public final class SSCAppVersionUserUpdateBuilder {
    private final UnirestInstance unirest;
    private final SSCUserHelper authEntitiesHelper;
    private ArrayNode authEntitiesToAdd = new ObjectMapper().createArrayNode();
    private Set<String> authEntitySpecsToRemove = new LinkedHashSet<String>();
    private boolean allowMultipleMatchesForRemove;
    
    @Data
    public static final class SSCAppVersionAuthEntitiesUpdater {
        private final ArrayNode authEntitiesToAdd;
        private final ArrayNode authEntitiesToRemove;
        private final HttpRequest<?> updateRequest;
    }
    
    public SSCAppVersionUserUpdateBuilder(UnirestInstance unirest) {
        this(unirest, new SSCUserHelper(unirest));
    }
    
    public SSCAppVersionUserUpdateBuilder(UnirestInstance unirest, SSCUserHelper authEntitiesHelper) {
        this.unirest = unirest;
        this.authEntitiesHelper = authEntitiesHelper;
    }
    
    public final SSCAppVersionAuthEntitiesUpdater build(String appVersionId) {
        ArrayNode currentAuthEntities = (ArrayNode)unirest.get(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .queryString("limit","-1").asObject(JsonNode.class).getBody().get("data");
        HttpRequest<?> updateRequest = unirest
                .put(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .body(addAuthEntities(removeAuthEntities(currentAuthEntities)));
        SSCUserSpecPredicate predicate = new SSCUserSpecPredicate(authEntitySpecsToRemove.toArray(String[]::new), MatchMode.INCLUDE, allowMultipleMatchesForRemove);
        ArrayNode authEntitiesToRemove = JsonHelper.stream(currentAuthEntities)
                .filter(predicate)
                .collect(JsonHelper.arrayNodeCollector());
        return new SSCAppVersionAuthEntitiesUpdater(authEntitiesToAdd, authEntitiesToRemove, updateRequest);
    }
    
    public final HttpRequest<?> buildRequest(String appVersionId) {
        ArrayNode currentAuthEntities = (ArrayNode)unirest.get(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .queryString("limit","-1").asObject(JsonNode.class).getBody().get("data");
        return unirest
                .put(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .body(addAuthEntities(removeAuthEntities(currentAuthEntities)));
    }
    
    private final ArrayNode removeAuthEntities(ArrayNode currentAuthEntities) {
        SSCUserSpecPredicate predicate = new SSCUserSpecPredicate(authEntitySpecsToRemove.toArray(String[]::new), MatchMode.EXCLUDE, allowMultipleMatchesForRemove);
        currentAuthEntities = JsonHelper.stream(currentAuthEntities)
                .filter(predicate)
                .collect(JsonHelper.arrayNodeCollector());
        predicate.logUnmatched("WARN: The following auth entities are not being removed as they are not assigned to the application version: ");
        return currentAuthEntities;
    }
    
    private final ArrayNode addAuthEntities(ArrayNode currentAuthEntities) {
        return currentAuthEntities.addAll(authEntitiesToAdd);
    }
    
    public final SSCAppVersionUserUpdateBuilder add(boolean allowMultipleMatches, String... authEntitySpecs) {
        authEntitiesToAdd.addAll(authEntitiesHelper.getAuthEntities(allowMultipleMatches, true, authEntitySpecs));
        return this;
    }
    
    public final SSCAppVersionUserUpdateBuilder remove(boolean allowMultipleMatches, String... authEntitySpecs) {
        this.allowMultipleMatchesForRemove |= allowMultipleMatches;
        if ( authEntitySpecs!=null && authEntitySpecs.length>0 ) {
        	authEntitySpecsToRemove.addAll(Arrays.asList(authEntitySpecs));
        }
        return this;
    }
}
