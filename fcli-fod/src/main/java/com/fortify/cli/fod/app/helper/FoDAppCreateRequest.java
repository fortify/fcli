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
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess
@Getter
@ToString
public class FoDAppCreateRequest {
    private String applicationName;
    private String applicationDescription;
    private String businessCriticalityType;
    private String emailList;
    private String releaseName;
    private String releaseDescription;
    private String sdlcStatusType;
    private Integer ownerId;
    private String applicationType;
    private Boolean hasMicroservices;
    private JsonNode microservices;
    private String releaseMicroserviceName;
    private JsonNode attributes;
    private JsonNode userGroupIds;

    public FoDAppCreateRequest setApplicationName(String name) {
        this.applicationName = name;
        return this;
    }

    public FoDAppCreateRequest setApplicationDescription(String description) {
        this.applicationDescription = (description == null ? "" : description);
        return this;
    }

    public FoDAppCreateRequest setBusinessCriticalityType(String type) {
        this.businessCriticalityType = type;
        return this;
    }

    public FoDAppCreateRequest setEmailList(String list) {
        this.emailList = list;
        return this;
    }

    public FoDAppCreateRequest setReleaseName(String name) {
        this.releaseName = name;
        return this;
    }

    public FoDAppCreateRequest setReleaseDescription(String description) {
        this.releaseDescription = (description == null ? "" : description);
        return this;
    }

    public FoDAppCreateRequest setSdlcStatusType(String type) {
        this.sdlcStatusType = type;
        return this;
    }

    public FoDAppCreateRequest setOwnerId(Integer id) {
        this.ownerId = id;
        return this;
    }

    public FoDAppCreateRequest setApplicationType(String type) {
        if (type.equals(FoDAppTypeOptions.FoDAppType.Microservice.getName())) {
            this.hasMicroservices = true;
        } else {
            this.applicationType = type;
        }
        return this;
    }

    public FoDAppCreateRequest setHasMicroservices(Boolean hasMicroservices) {
        this.hasMicroservices = hasMicroservices;
        return this;
    }

    public FoDAppCreateRequest setMicroservices(JsonNode microservices) {
        this.microservices = microservices;
        return this;
    }

    public FoDAppCreateRequest setReleaseMicroserviceName(String name) {
        this.releaseMicroserviceName = (name == null ? "" : name);
        return this;
    }

    public FoDAppCreateRequest setAttributes(JsonNode attributes) {
        this.attributes = attributes;
        return this;
    }

    public FoDAppCreateRequest setUserGroupIds(JsonNode ids) {
        this.userGroupIds = ids;
        return this;
    }

}
