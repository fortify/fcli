package com.fortify.cli.functest.common.runner;

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
public class FcliRunner {
    private static IRunner runner
    
    static void initialize() {
        System.setProperty("picocli.ansi", "false")
        runner = createRunner()
    }
    
    static boolean run(String[] args) {
        if ( !runner ) {
            throw new IllegalStateException("Runner not initialized")
        } 
        try {
            return runner.run(args)
        } catch ( Exception e ) {
            e.printStackTrace()
            return false
        }
    }
    
    static void close() {
        if ( runner ) { 
            runner.close()
        }
    }
    
    private static IRunner createRunner() {
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
    
    private static interface IRunner extends AutoCloseable {
        boolean run(String... args);
    }
    
    @Immutable
    private static class ExternalRunner implements IRunner {
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
    private static class ReflectiveRunner implements IRunner {
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