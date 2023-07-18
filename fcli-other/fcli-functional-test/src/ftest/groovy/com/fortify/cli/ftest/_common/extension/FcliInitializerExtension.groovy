package com.fortify.cli.ftest._common.extension;

import org.spockframework.runtime.extension.IGlobalExtension

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliSessionType

import groovy.transform.CompileStatic

@CompileStatic
class FcliInitializerExtension implements IGlobalExtension {
    @Override
    public void start() {
         Fcli.initialize();
    }
    
    @Override
    public void stop() {
        FcliSessionType.logoutAll()
        Fcli.close()
    }
}