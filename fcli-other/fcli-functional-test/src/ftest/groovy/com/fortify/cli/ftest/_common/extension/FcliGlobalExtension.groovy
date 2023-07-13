package com.fortify.cli.ftest._common.extension;

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.ftest._common.Input
import com.fortify.cli.ftest._common.runner.FcliRunner
import com.fortify.cli.ftest._common.spec.Fcli
import com.fortify.cli.ftest._common.spec.FcliSessionType
import com.fortify.cli.ftest._common.spec.Prefix

import groovy.transform.CompileStatic

@CompileStatic
class FcliGlobalExtension implements IGlobalExtension {
    @Override
    public void start() {
         FcliRunner.initialize();
    }
    
    @Override
    public void stop() {
        FcliSessionType.logoutAll()
        FcliRunner.close()
    }
    
    @Override
    void visitSpec(SpecInfo spec) {
        updateNames(spec)
        setFcliField(spec)
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
    
    private void setFcliField(SpecInfo spec) {
        // Register an interceptor for setting up any fields annotated with @Fcli
        spec.allFields.each { FieldInfo field ->
            if ( field.getAnnotation(Fcli.class) ) {
                spec.addSetupInterceptor( { it ->
                    setFcliField(field, it.instance)
                    it.proceed()
                })
            }
        }
    }
    
    private void setFcliField(FieldInfo fieldInfo, Object instance) {
        instance.metaClass.setProperty(instance, fieldInfo.reflection.name, FcliRunner.&run)
    }
    
    private void skipFeatures(SpecInfo spec) {
        // Exclude any features not matching any of the feature names
        // listed in the fcli.run property
        def run = Input.TestsToRun.get()?.split(",")
        if (run) {
            if ( !run.any { spec.name.startsWith(it) } ) {
                spec.skip "Not included in "+Input.TestsToRun.propertyName()+" property"
            }
            spec.allFeatures.each({ feature ->
                if ( !run.any {feature.name.startsWith(it)} ) {
                    feature.skip "Not included in "+Input.TestsToRun.propertyName()+" property"
                }
            })
        }
    }
}