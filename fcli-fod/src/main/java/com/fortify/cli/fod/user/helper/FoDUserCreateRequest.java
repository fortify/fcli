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
