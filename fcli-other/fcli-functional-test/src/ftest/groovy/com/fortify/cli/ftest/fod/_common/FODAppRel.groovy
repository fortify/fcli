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

public class FODAppRel implements Closeable, AutoCloseable {
    private final String random = System.currentTimeMillis()
    private final String fcliVariableName = "fod_apprel_"+random
    private final String appName = "fcli-"+random
    private final String versionName = "v"+random
    private final String microserviceName = "ms"+random
    private final String ownerId = 16225;
    
    public FODAppRel createWebApp() {
        Fcli.run("fod app create $appName:$versionName "+ 
            "--description Auto\\ created\\ by\\ test " +
            "--sdlc-status=Development " + 
            "--release=$versionName "+
            "--owner=$ownerId " +
            "--app-type=Web " +
            "--business-criticality=Medium " +
            "--auto-required-attrs " +
            "--store $fcliVariableName",
            {it.expectSuccess(true, "Unable to create application release")})
        return this
    }
    
    public FODAppRel createMobileApp() {
        Fcli.run("fod app create $appName:$versionName "+
            "--description Auto\\ created\\ by\\ test " +
            "--sdlc-status=Development " +
            "--release=$versionName "+
            "--owner=$ownerId " +
            "--app-type=Mobile " +
            "--business-criticality=Medium " +
            "--auto-required-attrs " +
            "--store $fcliVariableName",
            {it.expectSuccess(true, "Unable to create application release")})
        return this
    }
    
    public FODAppRel createMicroservicesApp() {
        Fcli.run("fod app create $appName:$versionName "+
            "--description Auto\\ created\\ by\\ test " +
            "--sdlc-status=Development " +
            "--release=$microserviceName:$versionName "+
            "--owner=$ownerId " +
            "--app-type=Microservice " +
            "--business-criticality=Medium " +
            "--auto-required-attrs " +
            "--store $fcliVariableName",
            {it.expectSuccess(true, "Unable to create application release")})
        return this
    }
    
    public String get(String propertyPath) {
        Fcli.run("util var contents $fcliVariableName -o expr={$propertyPath}",
            {it.expectSuccess(true, "Error getting application release property "+propertyPath)})
            .stdout[0]  
    }
    
    public String getVariableName() {
        return fcliVariableName
    }
    
    public String getVariableRef() {
        return "::"+fcliVariableName+"::"
    }
    
    public void close() {
        Fcli.run("fod app delete $applicationName",
            {it.expectSuccess(true, "Unable to delete application release")}) 
    }
}
