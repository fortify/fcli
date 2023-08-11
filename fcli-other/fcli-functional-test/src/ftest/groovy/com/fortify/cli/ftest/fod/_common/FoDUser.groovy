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
package com.fortify.cli.ftest.fod._common

import com.fortify.cli.ftest._common.Fcli

public class FoDUser implements Closeable, AutoCloseable {
    private final String random = System.currentTimeMillis()
    final String variableName = "fod_user_"+random
    final String variableRef = "::"+variableName+"::"
    final String groupName = "fcli-"+random
    final String userName = "fcliAutomatedTestUser"
    
    public FoDUser create() {
        Fcli.run("fod rest lookup --type Roles --store roles")
        Fcli.run("fod user create $userName --email=test@test.test --firstname=test --lastname=user --phone=1234 --role=::roles::get(0).value " +
            "--store $variableName",
            {it.expectSuccess(true, "Unable to create user")})
        return this
    }
    
    public String get(String propertyPath) {
        Fcli.run("util var contents $variableName -o expr={$propertyPath}",
            {it.expectSuccess(true, "Error getting application release property "+propertyPath)})
            .stdout[0]  
    }
    
    public void close() {
        Fcli.run("fod user delete $userName",
            {it.expectSuccess(true, "Unable to delete user")}) 
    }
}
