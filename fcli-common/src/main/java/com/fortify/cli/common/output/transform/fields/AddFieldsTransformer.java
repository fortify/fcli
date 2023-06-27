/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.common.output.transform.fields;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class AddFieldsTransformer extends AbstractJsonNodeTransformer implements IJsonNodeTransformer {
    private final Map<String, Supplier<String>> nameToValueSupplierMap;
    private boolean overwriteExisting = false;

    public AddFieldsTransformer(Map<String, Supplier<String>> nameToValueSupplierMap) {
        super(false);
        this.nameToValueSupplierMap = nameToValueSupplierMap;
    }
    
    public AddFieldsTransformer(String name, Supplier<String> valueSupplier) {
        this(Collections.singletonMap(name, valueSupplier));
    }
    
    public AddFieldsTransformer(String name, String value) {
        this(Collections.singletonMap(name, ()->value));
    }
    
    public AddFieldsTransformer(String[] nameToValueSpecs) {
        this(Stream.of(nameToValueSpecs).map(s->s.split(":")).collect(Collectors.toMap(a->a[0], a->()->a[1])));
    }
    
    public AddFieldsTransformer overwiteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
        return this;
    }
    
    @Override
    protected final ObjectNode transformObjectNode(ObjectNode input) {
        nameToValueSupplierMap.entrySet().forEach( 
            e->put(input, e)
        );
            
        return input;
    }
    
    private final void put(ObjectNode input, Map.Entry<String, Supplier<String>> e) {
        if ( overwriteExisting || !input.has(e.getKey()) ) {
            input.put(e.getKey(), e.getValue().get());
        }
    }
}
