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
import com.fortify.cli.ftest.ssc._common.SSCLocalUserSupplier.SSCUser

public class SSCAppVersionSupplier implements Closeable, AutoCloseable  {
    private SSCAppVersion version;
    
    public SSCAppVersion getVersion() {
        if ( !version ) {
            version = new SSCAppVersion().create()
        }
        return version
    }
    
    @Override
    public void close() {
        if ( version ) {
            version.close();
            version = null;
        }
    }
    
    
    
    public class SSCAppVersion {
        private final String random = System.currentTimeMillis()
        private final String fcliVariableName = "ssc_appversion_"+random
        private final String appName = "fcli-"+random
        private final String versionName = "v"+random
        
        public SSCAppVersion create() {
            Fcli.run("ssc appversion create $appName:$versionName "+
                "--issue-template Prioritized\\ High\\ Risk\\ Issue\\ Template "+
                "--auto-required-attrs --store $fcliVariableName",
                {it.expectSuccess(true, "Unable to create application version")})
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
            Fcli.run("ssc appversion delete ::$fcliVariableName::",
                {it.expectSuccess(true, "Unable to delete application version")})
        }
    }
    
}


