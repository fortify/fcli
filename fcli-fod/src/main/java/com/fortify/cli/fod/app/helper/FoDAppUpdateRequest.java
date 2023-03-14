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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Use @Builder instead of manually defining setter methods?
@ReflectiveAccess
@Getter
@ToString
public class FoDAppUpdateRequest {
    private String applicationName;
    private String applicationDescription;
    private String businessCriticalityType;
    private String emailList;
    private JsonNode attributes;
    private List<String> addMicroservices;
    private List<String> deleteMicroservices;
    private Map<String,String> renameMicroservices;

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

    public FoDAppUpdateRequest setAddMicroservices(List<String> addMicroservices) {
        this.addMicroservices = addMicroservices;
        return this;
    }

    public FoDAppUpdateRequest setDeleteMicroservices(List<String> deleteMicroservices) {
        this.deleteMicroservices = deleteMicroservices;
        return this;
    }

    public FoDAppUpdateRequest setRenameMicroservices(Map<String, String> renameMicroservices) {
        this.renameMicroservices = renameMicroservices;
        return this;
    }

}
