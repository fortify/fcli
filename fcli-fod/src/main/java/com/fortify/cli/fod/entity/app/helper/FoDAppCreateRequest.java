/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
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
package com.fortify.cli.fod.entity.app.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDAppTypeOptions;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

// TODO Use @Builder instead of manually defining setter methods?
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
    private Boolean autoRequiredAttrs;

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

    public FoDAppCreateRequest setAutoReqdAttrs(Boolean autoRequiredAttrs) {
        this.autoRequiredAttrs = autoRequiredAttrs;
        return this;
    }

}
