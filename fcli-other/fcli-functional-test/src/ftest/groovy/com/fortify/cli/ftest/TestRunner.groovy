package com.fortify.cli.ftest;

import java.text.SimpleDateFormat
import java.util.regex.Matcher

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary

/**
 * Simple class for running functional tests. Potentially, we could use JUnit's
 * ConsoleLauncher instead (provided in org.junit.platform:junit-platform-console), 
 * for example as follows:
 * ConsoleLauncher.main("-p", "com.fortify.cli.ftest", "-n", "^.+Spec\$","--reports-dir","testReport");
 * 
 * Although this provides a nicer test execution overview, it doesn't seem to provide
 * options for redirecting stdout/stderr output from tests to a log file. Due to the 
 * amount of output, it's not useful to have this on the console, and it causes issues 
 * when running on GitHub. So, for now we provide our own 'console launcher' that is 
 * fully customizable according to our needs.
 * 
 * @author Ruud Senden
 */
public class TestRunner {
    static PrintStream orgOut = System.out
    static PrintStream orgErr = System.err
    public static void main(String[] args) {
        setSystemProperties(args)
        def exitCode = 0
        new PrintStream(new File("test.log")).withCloseable { log ->
            final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("com.fortify.cli.ftest"))
                // Potentially we could add tag-based filter support here using TagFilter,
                // but probably better to handle this in FcliGlobalExtension to skip
                // tests based on tags, such that features not matching any tags will 
                // get listed as SKIPPED, instead of not being listed at all.
                .build();
            def fcliListener = new FcliTestExecutionListener(log);
            def summaryListener = new SummaryGeneratingListener();

            try {
                System.out = log
                System.err = log
                LauncherFactory.create().execute(request, summaryListener, fcliListener);
                TestExecutionSummary summary = summaryListener.getSummary();
                new PrintWriter(log).withCloseable { summary.printTo it }
                new PrintWriter(orgOut).withCloseable { summary.printTo it }
                exitCode = summary.totalFailureCount>0 ? 1 : 0
            } finally {
                System.out = orgOut
                System.err = orgErr
            }
        }
        System.exit(exitCode) 
    }
    
    private static void setSystemProperties(String[] args) {
        if ( args!=null ) {
            args.each {
                def parsed = false
                if ( it.startsWith("-D") && it.length()>2 ) {
                    Matcher parseResult = (it =~ /-D(.+)=(.+)/)
                    if ( parseResult.size()==1 && parseResult[0].size()==3 ) {
                        System.setProperty(parseResult[0][1], parseResult[0][2])
                        parsed = true
                    }
                }
                if ( !parsed ) {
                    System.err.println("WARN: Unknown argument: "+it)
                }
            }
        }
    }
    
    private static class FcliTestExecutionListener implements TestExecutionListener {
        private final SimpleDateFormat logPrefixFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ")
        private PrintStream log;
        private final def startTimes = [:]
        
        private FcliTestExecutionListener(PrintStream log) {
            this.log = log;
        }
        
        @Override
        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            logStatus("SKIPPED: "+testIdentifier.displayName+": "+reason)
        }
        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if ( !testIdentifier.isContainer() ) {
                logStatus("STARTED: "+testIdentifier.displayName)
                startTimes[testIdentifier.uniqueId] = System.currentTimeMillis();
            }
        }
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if ( !testIdentifier.isContainer() ) {
                testExecutionResult.throwable.ifPresent({
                    it.printStackTrace(orgErr)
                    it.printStackTrace(System.err)
                })
                logStatus(testExecutionResult.status.name()+": "+testIdentifier.displayName
                    +"("+(System.currentTimeMillis()-startTimes[testIdentifier.uniqueId])+" ms)")
            }
        }
        private void logStatus(String msg) {
            orgErr.flush(); // Make sure all error output has been printed before outputting status message
            def msgWithPrefix = logPrefixFormat.format(new Date()) + msg;
            orgOut.println(msgWithPrefix)
            log.println(msgWithPrefix)
        }
    }
}
