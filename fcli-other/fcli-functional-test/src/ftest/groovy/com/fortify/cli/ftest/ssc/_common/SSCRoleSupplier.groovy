/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.ftest.ssc._common

import com.fortify.cli.ftest._common.Fcli

public class SSCRoleSupplier implements Closeable, AutoCloseable {
    private SSCRole role;
    
    public SSCRole getRole() {
        if ( !role ) {
            role = new SSCRole().create()
        }
        return role
    }
    
    @Override
    public void close() {
        if ( role ) {
            role.close();
            role = null;
        }
    }

    public class SSCRole {
        private final String random = System.currentTimeMillis()
        private final String fcliVariableName = "ssc_role_"+random
        private final String roleName = "fcli-temp-role"+random
        
        public SSCRole create() {
            Fcli.run("ssc role create $roleName" + 
                " --description auto\\ created\\ by\\ test" + 
                " --permission-ids user_view,user_manage" + 
                " --store $fcliVariableName",
                {it.expectSuccess(true, "Unable to create role")})
            return this
        }
        
        public String get(String propertyPath) {
            Fcli.run("util var contents $fcliVariableName -o expr={$propertyPath}",
                {it.expectSuccess(true, "Error getting application version property "+propertyPath)})
                .stdout[0]  
        }
        
        public String getVariableName() {
            return fcliVariableName
        }
        
        public String getVariableRef() {
            return "::"+fcliVariableName+"::"
        }
        
        public void close() {
            Fcli.run("ssc role delete ::$fcliVariableName::id",
                {it.expectSuccess(true, "Unable to delete role")}) 
        }
    }

}