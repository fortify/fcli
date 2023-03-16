/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.role_permission.cli.mixin;

import com.fortify.cli.ssc.role_permission.helper.SSCRolePermissionDescriptor;
import com.fortify.cli.ssc.role_permission.helper.SSCRolePermissionHelper;

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
