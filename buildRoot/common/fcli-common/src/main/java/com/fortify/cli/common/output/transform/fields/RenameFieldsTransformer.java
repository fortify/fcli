/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.common.output.transform.fields;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.AbstractJsonNodeTransformer;
import com.fortify.cli.common.output.transform.IJsonNodeTransformer;

/**
 * This {@link IJsonNodeTransformer} allows for renaming fields in JSON objects or arrays.
 * For now, this only supports renaming top-level fields.
 * 
 * @author rsenden
 *
 */
public class RenameFieldsTransformer extends AbstractJsonNodeTransformer implements IJsonNodeTransformer {
    private final Map<String, String> oldToNewNameMap;

    public RenameFieldsTransformer(Map<String,String> oldToNewNameMap) {
        super(true);
        this.oldToNewNameMap = oldToNewNameMap;
    }
    
    public RenameFieldsTransformer(String oldName, String newName) {
        this(Collections.singletonMap(oldName, newName));
    }
    
    public RenameFieldsTransformer(String[] oldToNewNameSpecs) {
        this(Stream.of(oldToNewNameSpecs).map(s->s.split(":")).collect(Collectors.toMap(a->a[0], a->a[1])));
    }
    
    @Override
    protected final ObjectNode transformObjectNode(ObjectNode input) {
        ObjectNode output = new ObjectNode(JsonNodeFactory.instance);
        input.fields().forEachRemaining(e->output.set(rename(e.getKey()), e.getValue()));
        return output;
    }

    private String rename(String fieldName) {
        return oldToNewNameMap.getOrDefault(fieldName, fieldName);
    }
}
