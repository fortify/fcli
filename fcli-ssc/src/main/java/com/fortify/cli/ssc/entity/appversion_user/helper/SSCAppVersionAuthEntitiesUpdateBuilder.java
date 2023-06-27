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
package com.fortify.cli.ssc.entity.appversion_user.helper;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.entity.user.helper.SSCAuthEntitiesHelper;
import com.fortify.cli.ssc.entity.user.helper.SSCAuthEntitySpecPredicate;
import com.fortify.cli.ssc.entity.user.helper.SSCAuthEntitySpecPredicate.MatchMode;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Data;

public final class SSCAppVersionAuthEntitiesUpdateBuilder {
    private final UnirestInstance unirest;
    private final SSCAuthEntitiesHelper authEntitiesHelper;
    private ArrayNode authEntitiesToAdd = new ObjectMapper().createArrayNode();
    private Set<String> authEntitySpecsToRemove = new LinkedHashSet<String>();
    private boolean allowMultipleMatchesForRemove;
    
    @Data
    public static final class SSCAppVersionAuthEntitiesUpdater {
        private final ArrayNode authEntitiesToAdd;
        private final ArrayNode authEntitiesToRemove;
        private final HttpRequest<?> updateRequest;
    }
    
    public SSCAppVersionAuthEntitiesUpdateBuilder(UnirestInstance unirest) {
        this(unirest, new SSCAuthEntitiesHelper(unirest));
    }
    
    public SSCAppVersionAuthEntitiesUpdateBuilder(UnirestInstance unirest, SSCAuthEntitiesHelper authEntitiesHelper) {
        this.unirest = unirest;
        this.authEntitiesHelper = authEntitiesHelper;
    }
    
    public final SSCAppVersionAuthEntitiesUpdater build(String appVersionId) {
        ArrayNode currentAuthEntities = (ArrayNode)unirest.get(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .queryString("limit","-1").asObject(JsonNode.class).getBody().get("data");
        HttpRequest<?> updateRequest = unirest
                .put(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(appVersionId))
                .body(addAuthEntities(removeAuthEntities(currentAuthEntities)));
        SSCAuthEntitySpecPredicate predicate = new SSCAuthEntitySpecPredicate(authEntitySpecsToRemove.toArray(String[]::new), MatchMode.INCLUDE, allowMultipleMatchesForRemove);
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
        SSCAuthEntitySpecPredicate predicate = new SSCAuthEntitySpecPredicate(authEntitySpecsToRemove.toArray(String[]::new), MatchMode.EXCLUDE, allowMultipleMatchesForRemove);
        currentAuthEntities = JsonHelper.stream(currentAuthEntities)
                .filter(predicate)
                .collect(JsonHelper.arrayNodeCollector());
        predicate.logUnmatched("WARN: The following auth entities are not being removed as they are not assigned to the application version: ");
        return currentAuthEntities;
    }
    
    private final ArrayNode addAuthEntities(ArrayNode currentAuthEntities) {
        return currentAuthEntities.addAll(authEntitiesToAdd);
    }
    
    public final SSCAppVersionAuthEntitiesUpdateBuilder add(boolean allowMultipleMatches, String... authEntitySpecs) {
        authEntitiesToAdd.addAll(authEntitiesHelper.getAuthEntities(allowMultipleMatches, true, authEntitySpecs));
        return this;
    }
    
    public final SSCAppVersionAuthEntitiesUpdateBuilder remove(boolean allowMultipleMatches, String... authEntitySpecs) {
        this.allowMultipleMatchesForRemove |= allowMultipleMatches;
        if ( authEntitySpecs!=null && authEntitySpecs.length>0 ) {
        	authEntitySpecsToRemove.addAll(Arrays.asList(authEntitySpecs));
        }
        return this;
    }
}
