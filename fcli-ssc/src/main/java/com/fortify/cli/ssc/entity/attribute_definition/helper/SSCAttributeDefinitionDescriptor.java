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
package com.fortify.cli.ssc.entity.attribute_definition.helper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.ssc.entity.attribute_definition.domain.SSCAttributeDefinitionType;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@ReflectiveAccess
@Data @EqualsAndHashCode(callSuper=true)
public class SSCAttributeDefinitionDescriptor extends JsonNodeHolder {
    private String id;
    private String guid;
    private String name;
    private String category;
    private SSCAttributeDefinitionType type;
    private boolean required;
    private Map<String, SSCAttributeOptionDefinitionDescriptor> optionsByName = new LinkedHashMap<>();
    private Map<String, SSCAttributeOptionDefinitionDescriptor> optionsByGuid = new LinkedHashMap<>();
    @Accessors(fluent=true) private boolean hasDefault;
    
    @JsonProperty("options")
    public void setOptions(ArrayNode optionsNode) {
        if ( optionsNode!=null ) {
            JsonHelper.stream(optionsNode)
                .map(o->JsonHelper.treeToValue(o, SSCAttributeOptionDefinitionDescriptor.class))
                .forEach(this::addOption);
        }
    }
    
    private void addOption(SSCAttributeOptionDefinitionDescriptor descriptor) {
        optionsByName.put(descriptor.getName(), descriptor);
        optionsByGuid.put(descriptor.getGuid(), descriptor);
    }
    
    public JsonNode getOptionsAsJson() {
        return asJsonNode().get("options");
    }
    
    public String getFullName() {
        return category+":"+name;
    }
    
    public String getTypeName() {
        return type.name();
    }
    
    public void checkIsRequired() {
        if ( !required ) {
            throw new IllegalStateException("SSC attribute "+name+" must be configured as required attribute");
        }
    }
    
    public void checkType(SSCAttributeDefinitionType requiredType) {
        if ( this.type!=requiredType ) {
            throw new IllegalStateException("SSC attribute "+name+" must be configured as type "+requiredType.name());
        }
    }
    
    public void checkOptionNames(String... requiredNames) {
        var names = optionsByName.keySet();
        var requiredNamesList = Arrays.asList(requiredNames);
        if ( optionsByName.keySet().size()!=requiredNames.length || !names.containsAll(requiredNamesList) ) {
            throw new IllegalStateException("SSC attribute "+name+" must be configured to have exactly these options: "+requiredNamesList);
        }                
    }
    
    public void check(boolean required, SSCAttributeDefinitionType requiredType, String... requiredOptionNames) {
        if ( required ) { checkIsRequired(); }
        if ( requiredType!=null ) { checkType(requiredType); }
        if ( requiredOptionNames!=null ) { checkOptionNames(requiredOptionNames); }
    }
}
