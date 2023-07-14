package com.fortify.cli.ftest._common.extension;

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Input
import com.fortify.cli.ftest._common.spec.FcliSessionType
import com.fortify.cli.ftest._common.spec.Prefix

import groovy.transform.CompileStatic

@CompileStatic
class FcliGlobalExtension implements IGlobalExtension {
    @Override
    public void start() {
         Fcli.initialize();
    }
    
    @Override
    public void stop() {
        FcliSessionType.logoutAll()
        Fcli.close()
    }
    
    @Override
    void visitSpec(SpecInfo spec) {
        updateNames(spec)
        skipFeatures(spec)
    }
    
    private void updateNames(SpecInfo spec) {
        def prefixAnnotation = spec.getAnnotation(Prefix.class)
        if ( prefixAnnotation ) {
            spec.allFeatures.each { 
                it.name = prefixAnnotation.value()+"."+it.name
            }
            spec.name = prefixAnnotation.value()+" ("+spec.name+")"
        }
    }
    
    private void skipFeatures(SpecInfo spec) {
        // Exclude any features not matching any of the feature names listed in 
        // the fcli.run property
        // TODO Add support for skipping features based on tag include/exclude
        //      expressions
        def run = Input.TestsToRun.get()?.split(",")
        if (run) {
            spec.allFeatures.each({ feature ->
                if ( !run.any {feature.name.startsWith(it) && !feature.skipped } ) {
                    feature.skip "Not included in "+Input.TestsToRun.propertyName()+" property"
                }
            })
        }
    }
}