package com.fortify.cli.ftest.ssc._common;

import java.nio.file.Path

import org.spockframework.runtime.extension.IGlobalExtension

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest._common.util.RuntimeResourcesHelper
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier.SSCAppVersion

import groovy.transform.CompileStatic

@CompileStatic
class SSCGlobal implements IGlobalExtension {
    @Override
    public void start() {}
    
    @Override
    public void stop() {
        AppVersion.values().each { v->v.close() }
    }
    
    public static enum AppVersion implements Closeable, AutoCloseable {
        empty(AppVersion.&initEmpty),
        eightball(AppVersion.&initEightball),
        
        private SSCAppVersion version;
        private Closure initVersion;
        private AppVersion(Closure initVersion) {
            this.initVersion = initVersion;
        }
        
        private static void initEmpty(SSCAppVersion version) {
        
        }
        
        private static void initEightball(SSCAppVersion version) {
            Path fprPath = RuntimeResourcesHelper.extractResource("runtime/shared/EightBall-22.1.0.fpr");
            Fcli.run("ssc artifact upload -f $fprPath --appversion ${version.variableRef} --store globalEightBallFpr")
            Fcli.run("ssc artifact wait-for ::globalEightBallFpr:: -i 2s")
        }
        
        public SSCAppVersion getVersion() {
            if ( !version ) {
                version = new SSCAppVersion().create()
                initVersion(version)
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
    }
}