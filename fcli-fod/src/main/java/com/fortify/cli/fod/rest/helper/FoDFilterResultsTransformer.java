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