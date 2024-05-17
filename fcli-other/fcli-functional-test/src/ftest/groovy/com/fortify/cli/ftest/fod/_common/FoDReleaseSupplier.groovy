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

import java.nio.file.Path

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.Global.IGlobalValueSupplier
import com.fortify.cli.ftest._common.util.WorkDirHelper
import com.fortify.cli.ftest.fod._common.AbstractFoDAppSupplier.FoDApp
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier.SSCAppVersion

public class FoDReleaseSupplier implements Closeable, AutoCloseable  {
    private final Closure init;
    private FoDRelease release;
    
    public FoDReleaseSupplier() {
        this({});
    }
    
    public FoDReleaseSupplier(Closure init) {
        this.init = init;
    }
    
    public FoDRelease getRelease() {
        if ( !release ) {
            release = new FoDRelease().create()
            init(release);
        }
        return release
    }
    
    @Override
    public void close() {
        if ( release ) {
            release.close();
            release = null;
        }
    }
    
    public static class FoDRelease {
        private final String random = System.currentTimeMillis()
        private final String fcliVariableName = "fod_release_"+random
        private final String releaseName = "vx"+random
        private final FoDApp app = new FoDWebAppSupplier().createInstance()
        
        public FoDRelease create() {
            Fcli.run("fod release create ${app.appName}:${releaseName} "+
                "--status Development --store ${fcliVariableName}",
                {it.expectSuccess(true, "Unable to create application release")})
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
            app.close() // Should automatically delete release?
        }
    }
    
    private static abstract class ReleaseGlobalValueSupplier implements IGlobalValueSupplier {
        private FoDReleaseSupplier releaseSupplier;
        public final FoDReleaseSupplier getValue(WorkDirHelper workDirHelper) {
            if ( !releaseSupplier ) {
                releaseSupplier = new FoDReleaseSupplier({ FoDRelease r -> initRelease(workDirHelper, r) });
            }
            return releaseSupplier
        }
        @Override
        public final void close() {
            if ( releaseSupplier ) {
                releaseSupplier.close();
                releaseSupplier = null;
            }
        }
        protected String upload(FoDRelease release, Path fprPath) {
            def varName = "global${this.class.simpleName}Fpr"
            Fcli.run("fod sast-scan import -f $fprPath --release ${release.variableRef} --store ${varName}")
            waitForScan(release)
            return varName;
        }
        protected abstract void initRelease(WorkDirHelper workDirHelper, FoDRelease version);
        
        private void waitForScan(FoDRelease release) {
            def relScanurl = Fcli.run("fod release get ${release.variableRef} -o expr=/api/v3/releases/{releaseId}/scans --store relId").stdout[0]
            def timeoutMs = 60000
            def start = System.currentTimeMillis()
            def success = false;
            while(true){
                def result = Fcli.run("fod rest call ${relScanurl}")
                if(result.stdout.findAll{element -> element.contains("analysisStatusType: Completed")}.size()>0) {
                    success=true;
                    break;
                } else if(System.currentTimeMillis()-start > timeoutMs) {
                    break;
                }
                sleep(3000)
            }
        }
    }
    
    public static class Empty extends ReleaseGlobalValueSupplier {
        @Override
        protected void initRelease(WorkDirHelper workDirHelper, FoDRelease version) {}
    }
    
    public static class EightBall extends ReleaseGlobalValueSupplier {
        @Override
        protected void initRelease(WorkDirHelper workDirHelper, FoDRelease version) {
            upload(version, workDirHelper.getResource("runtime/shared/EightBall-22.1.0.fpr"));
        }
    }
    
    public static class LoginProject extends ReleaseGlobalValueSupplier {
        @Override
        protected void initRelease(WorkDirHelper workDirHelper, FoDRelease version) {
            upload(version, workDirHelper.getResource("runtime/shared/LoginProject.fpr"));
        }
    }
}


