package com.fortify.cli.ftest._common;

import java.nio.file.Files
import java.nio.file.Path

import org.spockframework.runtime.IStandardStreamsListener
import org.spockframework.runtime.StandardStreamsCapturer

import com.fortify.cli.ftest._common.util.TempDirHelper

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
public class Fcli {
    private static Path fcliDataDir
    private static IRunner runner
    private static Set<String> stringsToMask = []
    
    static void initialize() {
        System.setProperty("picocli.ansi", "false")
        fcliDataDir = TempDirHelper.create()
        System.setProperty("fcli.env.FORTIFY_DATA_DIR", fcliDataDir.toString())
        println("Using fcli data directory "+fcliDataDir)
        runner = createRunner()
    }
    
    /**
     * This method runs fcli with the arguments provided and returns an FcliResult
     * instance representing fcli execution result. By default, this method will
     * throw an exception if fcli returned a non-zero exit code, or if there was
     * any output on stderr. If needed, callers can provide a custom validator 
     * closure as the second argument to override this behavior.
     * @param args Arguments to pass to fcli
     * @param validate Optional closure to override validation of the fcli execution
     *        result; by default, an exception will be thrown if fcli execution was
     *        unsuccessful.
     * @return FcliResult describing fcli execution result
     */
    static FcliResult run(
        List<String> args,
        FcliResultValidator validator = {it.expectSuccess()}) 
    {
        def result = _run(args)
        validator.validate(result)
        return result
    }
    
    /**
     * This method runs fcli with the arguments provided and returns an FcliResult
     * instance representing fcli execution result. This method throws an exception 
     * if there was an error trying to execute fcli, for example if the configured
     * fcli executable cannot be found. Being private, this method can only be
     * invoked by the two run-methods above, essentially requiring callers to
     * provide a validation closure.
     * @param args Arguments to pass to fcli
     * @return FcliResult describing fcli execution result
     */
    private static final FcliResult _run(List<String> args) {
        if ( !runner ) {
            throw new IllegalStateException("Runner not initialized")
        }
        println "==> fcli "+args.collect({mask(it)}).join(" ")
        new FcliOutputCapturer().start().withCloseable {
            int exitCode = runner.run(args)
            return new FcliResult(exitCode, it.stdout, it.stderr)
        }
    }
    
    private static final String mask(String input) {
        if ( stringsToMask && stringsToMask.size()>0 ) {
            stringsToMask.each { input = input==null ? "" : input.replace(it, "*****")}
        }
        return input
    }
    
    static void close() {
        if ( runner ) { 
            runner.close()
        }
        TempDirHelper.rm(fcliDataDir)
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
        final boolean isZeroExitCode() {
            exitCode==0
        }
        final boolean isNonZeroExitCode() {
            exitCode!=0
        }
        final boolean isUnexpectedStderrOutput() {
            zeroExitCode && stderr!=null && stderr.size()>0
        }
        final boolean isSuccess() {
            zeroExitCode && !unexpectedStderrOutput
        }
        final FcliResult expectSuccess(boolean expectedSuccess=true, String msg="") {
            if ( expectedSuccess!=success ) {
                def pfx = msg.isBlank() ? "" : (msg+":\n   ")
                if ( success ) {
                    throw new IllegalStateException(pfx+"Fcli unexpectedly terminated successfully")
                } else {
                    throw new IllegalStateException(pfx+"Fcli unexpectedly terminated unsuccessfully\n   "
                        +stderr.join("\n   "))
                }
            }
            return this
        }
    }
    
    @CompileStatic
    static interface FcliResultValidator {
        void validate(FcliResult result);
    }
    
    private static interface IRunner extends AutoCloseable {
        int run(List<String> args);
    }
    
    @Immutable
    private static class ExternalRunner implements IRunner {
        List fcliCmd
        
        @Override
        int run(List<String> args) {
            def fullCmd = fcliCmd+args
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
        int run(List<String> args) {
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
            return sb.isBlank()
                ? Collections.emptyList()
                : sb.toString().split("(\\r\\n|\\n|\\r)").toList()
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