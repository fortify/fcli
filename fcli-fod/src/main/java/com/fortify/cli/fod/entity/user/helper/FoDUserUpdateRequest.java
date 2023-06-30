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
package com.fortify.cli.fod.entity.user.helper;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@ReflectiveAccess
@Getter
@ToString
public class FoDUserUpdateRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String password;
    private Integer roleId;
    private Boolean passwordNeverExpires = false;
    private Boolean isSuspended = false;
    private Boolean mustChange = false;
    private JsonNode addUserGroups;
    private JsonNode removeUserGroups;
    private JsonNode addApplications;
    private JsonNode removeApplications;

    public FoDUserUpdateRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public FoDUserUpdateRequest setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public FoDUserUpdateRequest setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public FoDUserUpdateRequest setPhoneNumber(String phoneNumber) {
        this.phoneNumber = (phoneNumber == null ? "" : phoneNumber);
        return this;
    }

    public FoDUserUpdateRequest setPassword(String password) {
        this.password = (password == null ? "" : password);
        return this;
    }

    public FoDUserUpdateRequest setRoleId(Integer roleId) {
        this.roleId = roleId;
        return this;
    }

    public FoDUserUpdateRequest setPasswordNeverExpires(Boolean passwordNeverExpires) {
        this.passwordNeverExpires = passwordNeverExpires;
        return this;
    }

    public FoDUserUpdateRequest setIsSuspended(Boolean isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }

    public FoDUserUpdateRequest setMustChange(Boolean mustChange) {
        this.mustChange = mustChange;
        return this;
    }

    public FoDUserUpdateRequest setAddUserGroups(JsonNode ids) {
        this.addUserGroups = ids;
        return this;
    }

    public FoDUserUpdateRequest setRemoveUserGroups(JsonNode ids) {
        this.removeUserGroups = ids;
        return this;
    }

    public FoDUserUpdateRequest setAddApplications(JsonNode ids) {
        this.addApplications = ids;
        return this;
    }

    public FoDUserUpdateRequest setRemoveApplications(JsonNode ids) {
        this.removeApplications = ids;
        return this;
    }

}
