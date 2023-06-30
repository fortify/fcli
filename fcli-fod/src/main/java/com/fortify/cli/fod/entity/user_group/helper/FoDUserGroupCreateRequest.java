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
package com.fortify.cli.fod.entity.user_group.helper;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@ReflectiveAccess
@Getter
@ToString
public class FoDUserGroupCreateRequest {
    private String name;
    private Boolean addAllUsers = false;
    private JsonNode users;

    private JsonNode applications;

    public FoDUserGroupCreateRequest setName(String name) {
        this.name = name;
        return this;
    }

    public FoDUserGroupCreateRequest setAddAllUsers(Boolean addAllUsers) {
        this.addAllUsers = addAllUsers;
        return this;
    }

    public FoDUserGroupCreateRequest setUsers(JsonNode ids) {
        this.users = ids;
        return this;
    }

    public FoDUserGroupCreateRequest setApplications(JsonNode ids) {
        this.applications = ids;
        return this;
    }

}
