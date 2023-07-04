package com.fortify.cli.functest.common.extension;

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.functest.common.runner.FcliRunner
import com.fortify.cli.functest.common.spec.Fcli
import com.fortify.cli.functest.common.spec.FcliSessionType

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
        setFcliField(spec)
        skipFeatures(spec)
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
        def run = ((String)System.properties["ftest.run"])?.split(",")
        if (run) {
            spec.allFeatures.each({ feature ->
                if ( !run.any {feature.name.startsWith(it)} ) {
                    feature.skip "Not included in ftest.run property"
                }
            })
        }
    }
}