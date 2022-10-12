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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class FoDAppUpdateRequest extends JsonNodeHolder {
    private String applicationName;
    private String applicationDescription;
    private String businessCriticalityType;
    private String emailList;
    private JsonNode attributes;

    public FoDAppUpdateRequest setApplicationName(String name) {
        this.applicationName = name;
        return this;
    }

    public FoDAppUpdateRequest setApplicationDescription(String description) {
        this.applicationDescription = description;
        return this;
    }

    public FoDAppUpdateRequest setBusinessCriticalityType(String type) {
        this.businessCriticalityType = type;
        return this;
    }

    public FoDAppUpdateRequest setEmailList(String list) {
        this.emailList = list;
        return this;
    }

    public FoDAppUpdateRequest setAttributes(JsonNode attributes) {
        this.attributes = attributes;
        return this;
    }

}
