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
package com.fortify.cli.sc_dast.scan_policy.helper;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;

import kong.unirest.UnirestInstance;

public class SCDastScanPolicyHelper {
    private static final String[] FIELD_RENAMES = {"policyName:name", "policyDescription:description"};
    private SCDastScanPolicyHelper() {}
    
    public static final SCDastScanPolicyDescriptor getScanPolicyDescriptor(UnirestInstance unirest, String scanPolicyNameOrId) {
        ArrayNode matchingPolicies = getScanPoliciesStream(unirest)
            .filter(record->matches(record, scanPolicyNameOrId))
            .collect(JsonHelper.arrayNodeCollector());
        if ( matchingPolicies.size()==0 ) {
            throw new IllegalArgumentException("No scan policy found with name or id "+scanPolicyNameOrId);
        } else if ( matchingPolicies.size()>1 ) {
            throw new IllegalArgumentException("Multiple scan policies match name or id "+scanPolicyNameOrId);
        }
        return getDescriptor(matchingPolicies.get(0));
    }
    
    private static boolean matches(JsonNode record, String scanPolicyNameOrId) {
        return record.get("id").asText().equals(scanPolicyNameOrId) ||
                record.get("name").asText().equals(scanPolicyNameOrId);
    }

    public static final ArrayNode getScanPolicies(UnirestInstance unirest) {
        return getScanPoliciesStream(unirest).collect(JsonHelper.arrayNodeCollector());
    }

    private static final Stream<JsonNode> getScanPoliciesStream(UnirestInstance unirest) {
        return JsonHelper.stream(
            (ArrayNode)unirest.get("/api/v2/policies").asObject(JsonNode.class).getBody()
        )
        .flatMap(SCDastScanPolicyHelper::mapPolicyNodes);
    }
    
    private static final Stream<JsonNode> mapPolicyNodes(JsonNode node) {
        final String policyCategory = node.get("policyCategory").asText();
        return JsonHelper.stream((ArrayNode)node.get("policies"))
                .map(ObjectNode.class::cast)
                .map(o->o.put("category", policyCategory))
                .map(new RenameFieldsTransformer(FIELD_RENAMES)::transform);
    }

    private static final SCDastScanPolicyDescriptor getDescriptor(JsonNode sensorNode) {
        return JsonHelper.treeToValue(sensorNode, SCDastScanPolicyDescriptor.class);
    }
}
