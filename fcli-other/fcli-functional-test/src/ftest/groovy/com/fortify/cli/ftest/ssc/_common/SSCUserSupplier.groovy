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

public class SSCUserSupplier implements Closeable, AutoCloseable {
    private SSCUser user;
    
    public SSCUser getUser() {
        if ( !user ) {
            user = new SSCUser().create()
        }
        return user
    }
    
    @Override
    public void close() {
        if ( user ) {
            user.close();
            user = null;
        }
    }
    
    public static class SSCUser { 
        private final String random = System.currentTimeMillis()
        private final String fcliVariableName = "ssc_user_"+random
        private final String userName = "fcli-temp-user"+random
        
        public SSCUser create() {
            Fcli.run("ssc user create --username $userName" + 
                " --password P@ssW._ord123" + 
                " --firstname fName" + 
                " --lastname lName" + 
                " --email mail@mail.mail" + 
                " --roles admin" + 
                " --store $fcliVariableName",
                {it.expectSuccess(true, "Unable to create user")})
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
            Fcli.run("ssc user delete ::$fcliVariableName::userName",
                {it.expectSuccess(true, "Unable to delete user")}) 
        }
    }
}
