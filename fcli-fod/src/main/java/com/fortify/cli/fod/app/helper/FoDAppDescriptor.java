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
package com.fortify.cli.fod.app.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.fod.app_attribute.helper.FoDAppAttributeDescriptor;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ReflectiveAccess
@Data
@EqualsAndHashCode(callSuper = true)
public class FoDAppDescriptor extends JsonNodeHolder {
    @JsonProperty("applicationId")
    private Integer applicationId;
    @JsonProperty("applicationName")
    private String applicationName;
    @JsonProperty("applicationDescription")
    private String description;
    @JsonProperty("businessCriticalityType")
    private String criticality;
    @JsonProperty("attributes")
    private ArrayList<FoDAppAttributeDescriptor> attributes;
    @JsonProperty("emailList")
    private String emailList;
    @JsonProperty("releaseId")
    private Integer releaseId;
    @JsonProperty("microserviceId")
    private Integer microserviceId;

    public  Map<String, String> attributesAsMap() {
        Map<String, String> attrMap = new HashMap<>();
        for (FoDAppAttributeDescriptor attr : attributes) {
            attrMap.put(attr.getAttributeId(), attr.getValue());
            System.out.println(attr.getAttributeId() + ":" + attr.getValue());
        }
        return  attrMap;
    }

}
