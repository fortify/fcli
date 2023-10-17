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
package com.fortify.cli.ssc.role.cli.mixin;

import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.ssc.role.helper.SSCRolePermissionDescriptor;
import com.fortify.cli.ssc.role.helper.SSCRolePermissionHelper;

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

    public static class RequiredOption extends AbstractSSCRolePermissionMixin {
        @Getter
        @Option(names = {"--permission"}, required = true, descriptionKey = "fcli.ssc.role.permission.resolver.nameOrId")
        private String rolePermissionNameOrId;
    }

    public static class PositionalParameter extends AbstractSSCRolePermissionMixin {
        @Getter
        @EnvSuffix("ROLE_PERMISSION") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.role.permission.resolver.nameOrId")
        private String rolePermissionNameOrId;
    }
}
