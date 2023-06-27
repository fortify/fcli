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
package com.fortify.cli.ssc.entity.role_permission.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
@EqualsAndHashCode(callSuper=true)
@Data
public class SSCRolePermissionDescriptor extends JsonNodeHolder {
    @JsonProperty("id") private String permissionId;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("assignByDefault") private Boolean assignByDefault;
    @JsonProperty("dependsOnPermission") private SSCRolePermissionDescriptor[] dependsOnPermissionIds;
}
