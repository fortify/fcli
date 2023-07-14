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

import groovy.transform.builder.Builder

public class SSCAppVersion implements Closeable, AutoCloseable {
    private final String random = System.currentTimeMillis()
    private final String fcliVariableName = "ssc_appversion_"+random
    private final String appName = "fcli-"+random
    private final String versionName = "v"+random
    
    public SSCAppVersion create() {
        Fcli.runOrFail("Unable to create application version", 
            "ssc", "appversion", "create", appName+":"+versionName, 
            "--issue-template", "Prioritized High Risk Issue Template",
            "--auto-required-attrs", "--store", fcliVariableName)
        return this
    }
    
    public void close() {
        Fcli.runOrFail("Unable to delete application version",
            "ssc", "appversion", "delete", "::"+fcliVariableName+"::")
    }
}
