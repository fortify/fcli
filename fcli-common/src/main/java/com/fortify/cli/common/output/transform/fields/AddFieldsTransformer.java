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
    
    @Override
    protected final ObjectNode transformObjectNode(ObjectNode input) {
        nameToValueSupplierMap.entrySet().forEach( 
            e->input.put(e.getKey(), e.getValue().get())
        );
            
        return input;
    }
}
