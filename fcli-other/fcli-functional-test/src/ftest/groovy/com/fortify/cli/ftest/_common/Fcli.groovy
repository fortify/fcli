package com.fortify.cli.ftest._common;

import java.nio.file.Files
import java.nio.file.Path

import org.spockframework.runtime.IStandardStreamsListener
import org.spockframework.runtime.StandardStreamsCapturer

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TupleConstructor

@CompileStatic
public class Fcli {
    private static Path fcliDataDir;
    private static IRunner runner
    
    static void initialize() {
        System.setProperty("picocli.ansi", "false")
        fcliDataDir = Files.createTempDirectory("fcli").toAbsolutePath()
        System.setProperty("fcli.env.FORTIFY_DATA_DIR", fcliDataDir.toString())
        println("Using fcli data directory "+fcliDataDir)
        runner = createRunner()
    }
    
    /**
     * This method allows for running fcli with the given arguments,
     * returning execution results in an FcliResult object. The
     * returned object can be used by feature specs to define
     * expectations on properties like 'success' status, exit code,
     * and stdout/stderr contents.
     * @param args Arguments to pass to fcli
     * @return FcliResult describing fcli execution result
     */
    static FcliResult run(String[] args) {
        if ( !runner ) {
            throw new IllegalStateException("Runner not initialized")
        }
        new FcliOutputCapturer().start().withCloseable {
            int exitCode = runner.run(args)
            return new FcliResult(exitCode, it.stdout, it.stderr)
        }
    }
    
    /**
     * This method allows for running fcli with the given arguments,
     * throwing an exception with the given message if the fcli 
     * invocation was not successful. This method shouldn't be used 
     * by feature specs; these should call the run() method and
     * define proper expectations like expected success. 
     * @param msg Exception message to use in case of failure 
     * @param args Arguments to pass to fcli
     */
    static void runOrFail(String msg, String[] args) {
        if ( !run(args).success ) {
            throw new IllegalStateException(msg)
        }
    }
    
    static void close() {
        if ( runner ) { 
            runner.close()
        }
        try {
            Files.walk(fcliDataDir)
                .sorted(Comparator.reverseOrder())
                .map(Path.&toFile)
                .forEach(File.&delete); // For some reason this throws an exception on the
                                        // top-level directory, but the full directory tree
                                        // is being deleted anyway. As such, we just swallow
                                        // any exceptions, and print an error if the directory 
                                        // still exists afterwards. 
        } catch ( IOException e ) {}
        if ( fcliDataDir.toFile().exists() ) {
            println "Error deleting directory "+fcliDataDir+", please clean up manually";
        }
    }
    
    private static IRunner createRunner() {
        String fcli = Input.FcliCommand.get()
        String java = Input.JavaCommand.get() ?: "java"
        if ( !fcli || fcli=="build" ) {
            return new ReflectiveRunner()
        } else {
            def cmd = fcli.endsWith(".jar")
                ? [java, "-jar", fcli]
                : [fcli]
            return new ExternalRunner(cmd)
        }
    }
    
    @Immutable
    static class FcliResult {
        int exitCode;
        List<String> stdout;
        List<String> stderr;
        final boolean isSuccess() {
            exitCode==0
        }
    }
    
    private static interface IRunner extends AutoCloseable {
        int run(String... args);
    }
    
    @Immutable
    private static class ExternalRunner implements IRunner {
        List fcliCmd
        
        @Override
        int run(String... args) {
            def argsList = args as List
            def fullCmd = fcliCmd+argsList
            def proc = fullCmd.execute()
            // TODO This is the only method that works for properly
            //      getting all process output, however potentially
            //      this could wait indefinitely, for example if
            //      the process is waiting for input. So, we should
            //      implement some time-out mechanism. 
            proc.waitForProcessOutput(System.out, System.err)
            return proc.exitValue()
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
        int run(String... args) {
            if ( obj==null ) {
                obj = Class.forName("com.fortify.cli.app.runner.DefaultFortifyCLIRunner").newInstance()
            }
            return (int)obj.invokeMethod("run", args)
        }
        
        @Override
        void close() {
            if ( obj!=null ) {
                obj.invokeMethod("close", [])
            }
        }
    }
    
    private static class FcliOutputCapturer implements IStandardStreamsListener, Closeable, AutoCloseable {
        @Lazy private static final StandardStreamsCapturer capturer = createCapturer();
        private StringBuffer stdoutBuffer = null;
        private StringBuffer stderrBuffer = null;

        @Override
        void standardOut(String message) {
            stdoutBuffer.append(message)
        }

        @Override
        void standardErr(String message) {
            stderrBuffer.append(message)
        }
        
        List<String> getStdout() {
            return getLines(stdoutBuffer)
        }
        
        List<String> getStderr() {
            return getLines(stderrBuffer)
        }

        private List<String> getLines(StringBuffer sb) {
            return sb.toString().split("(\\r\\n|\\n|\\r)").toList()
        }
        
        private static final StandardStreamsCapturer createCapturer() {
            def capturer = new StandardStreamsCapturer()
            capturer.start()
            return capturer
        }

        FcliOutputCapturer start() {
            this.stdoutBuffer = new StringBuffer()
            this.stderrBuffer = new StringBuffer()
            capturer.addStandardStreamsListener(this);
            return this;
        }
        void close() {
            capturer.removeStandardStreamsListener(this);
        }
    }
}