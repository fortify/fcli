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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;

public final class SSCIssueFilterHelper {
    private final MultiValueMap<String, String> technicalFiltersByFriendlyFilter = new LinkedMultiValueMap<>();
    private final Map<String, JsonNode> filterNodesByTechnicalFilter = new LinkedHashMap<>();
    @Getter private final ArrayNode filterNodes = JsonHelper.getObjectMapper().createArrayNode();
    
    public SSCIssueFilterHelper(UnirestInstance unirest, String applicationVersionId) {
        JsonNode data = unirest.get(SSCUrls.PROJECT_VERSION_ISSUE_SELECTOR_SET(applicationVersionId))
                .asObject(JsonNode.class).getBody()
                .get("data")
                .get("filterBySet");
        data.forEach(this::processEntity);
    }
    
    public final String getFilter(String technicalOrFriendlyFilter) {
        if ( filterNodesByTechnicalFilter.containsKey(technicalOrFriendlyFilter) ) {
            return technicalOrFriendlyFilter;
        } else {
            var matchingFriendlyFilters = technicalFiltersByFriendlyFilter.get(technicalOrFriendlyFilter);
            if ( matchingFriendlyFilters==null || matchingFriendlyFilters.size()==0 ) {
                throw new IllegalArgumentException(technicalOrFriendlyFilter+" is not a supported filter");
            } else if ( matchingFriendlyFilters.size()>1 ) {
                throw new IllegalArgumentException(technicalOrFriendlyFilter+" is ambiguous.\n" +
                    "please use one of the following filters:\n"+
                    matchingFriendlyFilters.stream().map(s->s.indent(2)).collect(Collectors.joining("\n")));
            } else {
                return matchingFriendlyFilters.get(0);
            }
        }
    }
    
    public final JsonNode getFilterNode(String technicalOrFriendlyFilter) {
        return filterNodesByTechnicalFilter.get(getFilter(technicalOrFriendlyFilter));
    }

    private final void processEntity(JsonNode entity) {
        processSelectors(entity, entity.get("selectorOptions"));
    }
    
    private final void processSelectors(JsonNode entity, JsonNode selectors) {
        selectors.forEach(selector->processSelector(entity, selector));
    }

    private final void processSelector(JsonNode entity, JsonNode selector) {
        var newEntity = (ObjectNode)entity.deepCopy();
        newEntity.remove("selectorOptions");
        newEntity.set("selector", selector);
        var filter = JsonHelper.evaluateSpelExpression(newEntity, "entityType+'['+value+']:'+selector.value", String.class);
        var friendlyFilter = JsonHelper.evaluateSpelExpression(newEntity, "displayName+':'+selector.displayName?:'NONE'", String.class);
        newEntity.put("technicalFilter", filter);
        newEntity.put("friendlyFilter", friendlyFilter);
        filterNodes.add(newEntity);
        filterNodesByTechnicalFilter.put(filter, newEntity);
        technicalFiltersByFriendlyFilter.add(friendlyFilter, filter);
    }    
}
