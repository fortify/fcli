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
package com.fortify.cli.ssc.entity.role.cli.mixin;

import com.fortify.cli.ssc.entity.role.helper.SSCRoleDescriptor;
import com.fortify.cli.ssc.entity.role.helper.SSCRoleHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Parameters;

public class SSCRoleResolverMixin {
    
    public static abstract class AbstractSSCRoleMixin {
        public abstract String getRoleNameOrId();

        @SneakyThrows
        public SSCRoleDescriptor getRoleDescriptor(UnirestInstance unirestInstance, String... fields){
            return SSCRoleHelper.getRoleDescriptor(unirestInstance, getRoleNameOrId(), fields);
        }
        
        public String getRoleId(UnirestInstance unirestInstance) {
            return getRoleDescriptor(unirestInstance, "id").getRoleId();
        }
    }

    public static class PositionalParameter extends AbstractSSCRoleMixin {
        @Getter
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.role.resolver.nameOrId")
        private String roleNameOrId;
    }
}
