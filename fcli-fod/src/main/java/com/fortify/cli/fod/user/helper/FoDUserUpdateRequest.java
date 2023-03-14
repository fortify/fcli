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
package com.fortify.cli.fod.user.helper;

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
