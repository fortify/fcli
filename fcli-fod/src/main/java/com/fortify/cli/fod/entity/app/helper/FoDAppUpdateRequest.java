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
