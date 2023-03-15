/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
