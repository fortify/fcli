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
package com.fortify.cli.ssc.entity.role_permission.cli.mixin;

import com.fortify.cli.ssc.entity.role_permission.helper.SSCRolePermissionDescriptor;
import com.fortify.cli.ssc.entity.role_permission.helper.SSCRolePermissionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCRolePermissionResolverMixin {
    
    public static abstract class AbstractSSCRolePermissionMixin {
        public abstract String getRolePermissionNameOrId();

        @SneakyThrows
        public SSCRolePermissionDescriptor getRolePermission(UnirestInstance unirestInstance){
            return SSCRolePermissionHelper.getRolePermission(unirestInstance, getRolePermissionNameOrId(), "id", "name");
        }
        
        public String getRolePermissionId(UnirestInstance unirestInstance) {
            return getRolePermission(unirestInstance).getPermissionId();
        }
    }

    public static class Role extends AbstractSSCRolePermissionMixin {
        @Getter
        @Option(names = {"--permission"}, required = true, descriptionKey = "SSCRolePermissionMixin")
        private String rolePermissionNameOrId;
    }

    public static class PositionalParameter extends AbstractSSCRolePermissionMixin {
        @Getter
        @Parameters(index = "0", arity = "1", descriptionKey = "SSCRolePermissionMixin")
        private String rolePermissionNameOrId;
    }
}
