package com.fortify.cli.ftest;

import java.text.SimpleDateFormat

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
 * @author Ruud Senden
 */
public class TestRunner {
    static def orgOut = System.out
    static def orgErr = System.err
    public static void main(String[] args) {
        def exitCode = 0
        new PrintStream(new File("test.log")).withCloseable { log ->
            final LauncherDiscoveryRequest request = 
            LauncherDiscoveryRequestBuilder.request()
                                       .selectors(DiscoverySelectors.selectPackage("com.fortify.cli.ftest"))
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
            def msgWithPrefix = logPrefixFormat.format(new Date()) + msg;
            orgOut.println(msgWithPrefix)
            log.println(msgWithPrefix)
        }
    }
}
