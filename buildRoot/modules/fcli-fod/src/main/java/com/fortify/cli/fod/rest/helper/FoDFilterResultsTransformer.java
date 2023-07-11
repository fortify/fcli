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

package com.fortify.cli.fod.rest.helper;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.AbstractJsonNodeTransformer;
import com.fortify.cli.common.output.transform.IJsonNodeTransformer;

// TODO Maybe this class is no longer needed if we merge all scan-related commands into
//      a single 'scan' entity with a generic 'list' command that has the generic -q option
//      for filtering by scan type etcetera?
public class FoDFilterResultsTransformer extends AbstractJsonNodeTransformer implements IJsonNodeTransformer {
    private final Map<String, String> fieldValueMap;

    public FoDFilterResultsTransformer(Map<String,String> fieldValueMap) {
        super(true);
        this.fieldValueMap = fieldValueMap;
    }

    public FoDFilterResultsTransformer(String[] fieldValueSpecs) {
        this(Stream.of(fieldValueSpecs).map(s->s.split(":")).collect(Collectors.toMap(a->a[0], a->a[1])));
    }

    @Override
    protected final ObjectNode transformObjectNode(ObjectNode input) {
        // TODO output variable is not used
        ObjectNode output = new ObjectNode(JsonNodeFactory.instance);
        boolean found = true;
        for (String key : fieldValueMap.keySet()) {
            JsonNode value = input.findValue(key);
            if (value != null && (fieldValueMap.get(key).equals("*") || fieldValueMap.get(key).equals(value.asText()))) {
                //System.out.printf("Key %s exists? %s --> value=%s%n", key, value != null, value == null ? null : value.asText());
            } else {
                found = false;
            }
        }
        return (found ? input : null);
    }

}