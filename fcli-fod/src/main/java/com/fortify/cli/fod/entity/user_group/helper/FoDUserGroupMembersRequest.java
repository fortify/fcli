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
package com.fortify.cli.fod.entity.user_group.helper;

import java.util.ArrayList;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@ReflectiveAccess
@Getter
@ToString
public class FoDUserGroupMembersRequest {
    private ArrayList<Integer> removeUsers;
    private ArrayList<Integer> addUsers;

    public FoDUserGroupMembersRequest setRemoveUsers(ArrayList<Integer> ids) {
        this.removeUsers = ids;
        return this;
    }

    public FoDUserGroupMembersRequest setAddUsers(ArrayList<Integer> ids) {
        this.addUsers = ids;
        return this;
    }

}
