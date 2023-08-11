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

public class FoDApp implements Closeable, AutoCloseable {
    private final String random = System.currentTimeMillis()
    final String variableName = "fod_apprel_"+random
    final String variableRef = "::"+variableName+"::"
    final String appName = "fcli-"+random
    final String versionName = "v"+random
    final String microserviceName = "ms"+random
    final String qualifiedRelease = appName+":"+versionName
    final String qualifiedMicroserviceRelease = appName+":"+microserviceName+":"+versionName
    final String ownerId = 16225;
    
    public FoDApp createWebApp() {
        Fcli.run("fod app create $appName:$versionName "+ 
            "--description Auto\\ created\\ by\\ test " +
            "--sdlc-status=Development " + 
            "--release=$versionName "+
            "--owner=$ownerId " +
            "--app-type=Web " +
            "--business-criticality=Medium " +
            "--auto-required-attrs " +
            "--store $variableName",
            {it.expectSuccess(true, "Unable to create application release")})
        return this
    }
    
    public FoDApp createMobileApp() {
        Fcli.run("fod app create $appName:$versionName "+
            "--description Auto\\ created\\ by\\ test " +
            "--sdlc-status=Development " +
            "--release=$versionName "+
            "--owner=$ownerId " +
            "--app-type=Mobile " +
            "--business-criticality=Medium " +
            "--auto-required-attrs " +
            "--store $variableName",
            {it.expectSuccess(true, "Unable to create application release")})
        return this
    }
    
    public FoDApp createMicroservicesApp() {
        Fcli.run("fod app create $appName:$versionName "+
            "--description Auto\\ created\\ by\\ test " +
            "--sdlc-status=Development " +
            "--release=$microserviceName:$versionName "+
            "--owner=$ownerId " +
            "--app-type=Microservice " +
            "--business-criticality=Medium " +
            "--auto-required-attrs " +
            "--store $variableName",
            {it.expectSuccess(true, "Unable to create application release")})
        return this
    }
    
    public String get(String propertyPath) {
        Fcli.run("util var contents $variableName -o expr={$propertyPath}",
            {it.expectSuccess(true, "Error getting application release property "+propertyPath)})
            .stdout[0]  
    }
    
    public void close() {
        Fcli.run("fod app delete $applicationName",
            {it.expectSuccess(true, "Unable to delete application release")}) 
    }
}
