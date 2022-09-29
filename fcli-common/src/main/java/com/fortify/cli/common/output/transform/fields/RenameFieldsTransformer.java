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
