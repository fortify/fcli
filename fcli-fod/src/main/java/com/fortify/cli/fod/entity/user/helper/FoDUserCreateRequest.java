/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.entity.user.helper;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess
@Getter
@ToString
public class FoDUserCreateRequest {
    private String userName;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Integer roleId;
    private Boolean passwordNeverExpires = false;
    private Boolean isSuspended = false;
    private JsonNode userGroupIds;
    private JsonNode applicationIds;

    public FoDUserCreateRequest setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public FoDUserCreateRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public FoDUserCreateRequest setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public FoDUserCreateRequest setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public FoDUserCreateRequest setPhoneNumber(String phoneNumber) {
        this.phoneNumber = (phoneNumber == null ? "" : phoneNumber);
        return this;
    }

    public FoDUserCreateRequest setRoleId(Integer roleId) {
        this.roleId = roleId;
        return this;
    }

    public FoDUserCreateRequest setPasswordNeverExpires(Boolean passwordNeverExpires) {
        this.passwordNeverExpires = passwordNeverExpires;
        return this;
    }

    public FoDUserCreateRequest setIsSuspended(Boolean isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }

    public FoDUserCreateRequest setUserGroupIds(JsonNode ids) {
        this.userGroupIds = ids;
        return this;
    }

    public FoDUserCreateRequest setApplicationIds(JsonNode ids) {
        this.applicationIds = ids;
        return this;
    }

}
