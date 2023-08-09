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
package com.fortify.cli.ssc.user.helper;

import java.util.ArrayList;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data @Builder
public class SSCUserCreateRequest {
    private String userName;
    private String clearPassword;
    private String email;
    private String firstName;
    private String lastName;
    @Builder.Default
    private ArrayList<SSCRoleObject> roles = new ArrayList<SSCRoleObject>();
    @Builder.Default
    private Boolean passwordNeverExpires = false;
    @Builder.Default
    private Boolean requirePasswordChange = false;
    @Builder.Default
    private Boolean suspended = false;
    
    public void addRoles(ArrayList<String> rolesToAdd) {
        if(rolesToAdd!=null) {
            rolesToAdd.forEach((n) -> roles.add(new SSCRoleObject(n)));
        }
    }
    
    public class SSCRoleObject {
        @Getter private String id;
        public SSCRoleObject(String id) {
            this.id = id;
        }
    }
}

