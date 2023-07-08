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
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@Reflectable @NoArgsConstructor
@Getter @ToString
public class FoDUserGroupUpdateRequest {
    private String name;
    private Boolean addAllUsers = false;
    private Boolean removeAllUsers = false;
    private JsonNode addUsers;
    private JsonNode removeUsers;
    private JsonNode addApplications;
    private JsonNode removeApplications;

    public FoDUserGroupUpdateRequest setName(String name) {
        this.name = name;
        return this;
    }

    public FoDUserGroupUpdateRequest setAddAllUsers(Boolean addAllUsers) {
        this.addAllUsers = addAllUsers;
        return this;
    }

    public FoDUserGroupUpdateRequest setRemoveAllUsers(Boolean removeAllUsers) {
        this.removeAllUsers = removeAllUsers;
        return this;
    }

    public FoDUserGroupUpdateRequest setAddUsers(JsonNode ids) {
        this.addUsers = ids;
        return this;
    }

    public FoDUserGroupUpdateRequest setRemoveUsers(JsonNode ids) {
        this.removeUsers = ids;
        return this;
    }

    public FoDUserGroupUpdateRequest setAddApplications(JsonNode ids) {
        this.addApplications = ids;
        return this;
    }

    public FoDUserGroupUpdateRequest setRemoveApplications(JsonNode ids) {
        this.removeApplications = ids;
        return this;
    }


}
