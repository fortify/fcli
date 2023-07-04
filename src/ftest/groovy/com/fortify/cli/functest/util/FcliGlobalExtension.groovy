package com.fortify.cli.functest.util;

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
class FcliGlobalExtension implements IGlobalExtension {
    private IRunner runner;
    
    @Override
    public void start() {
         System.setProperty("picocli.ansi", "false")
         this.runner = createRunner();
    }
    
    @Override
    public void stop() {
        this.runner.close();
    }
    
    @Override
    void visitSpec(SpecInfo spec) {
        // Register an interceptor for setting up any fields annotated with @Fcli
        spec.allFields.each { FieldInfo field ->
            if ( field.getAnnotation(Fcli.class) ) {
                spec.addSetupInterceptor( { it -> 
                    setFcliField(field, it.instance)
                    it.proceed()
                })
            }
        }
        // Exclude any features not matching any of the feature names
        // listed in the fcli.run property
        def run = ((String)System.properties["ftest.run"])?.split(",")
        if (run) {
            spec.allFeatures.each({ feature ->
                if ( !run.any {feature.name.startsWith(it)} ) {
                    feature.excluded = true
                }
            })
        }
    }
    
    void setFcliField(FieldInfo fieldInfo, Object instance) {
        instance.metaClass.setProperty(instance, fieldInfo.reflection.name, runner.&run)
    }
    
    IRunner createRunner() {
        String fcli = System.properties["ftest.fcli"]
        String java = System.properties["ftest.java"] ?: "java"
        if ( !fcli || fcli=="build" ) {
            return new ReflectiveRunner()
        } else {
            def cmd = fcli.endsWith(".jar")
                ? [java, "-jar", fcli]
                : [fcli]
            return new ExternalRunner(cmd)
        }
    }
    
    private interface IRunner extends AutoCloseable {
        boolean run(String... args);
    }
    
    @Immutable
    private class ExternalRunner implements IRunner {
        List fcliCmd
        
        @Override
        boolean run(String... args) {
            def argsList = args as List
            def fullCmd = fcliCmd+argsList 
            def proc = fullCmd.execute()
            proc.consumeProcessOutput(System.out, System.err)
            proc.waitForOrKill(60000)
            proc.exitValue()==0
        }
        
        @Override
        void close() {}
    }
    
    // We need to use reflection for instantiating/invoking DefaultFortifyCLIRunner,
    // as this class (and IFortifyCLIRunner interface) is not available if the 
    // ftest.fcli property points to an external fcli executable 
    private class ReflectiveRunner implements IRunner {
        private Object obj;
        
        @Override
        boolean run(String... args) {
            if ( obj==null ) {
                obj = Class.forName("com.fortify.cli.app.runner.DefaultFortifyCLIRunner").newInstance()
            }
            0==(int)obj.invokeMethod("run", args)
        }
        
        @Override
        void close() {
            if ( obj!=null ) {
                obj.invokeMethod("close", [])
            }
        }
    }
}