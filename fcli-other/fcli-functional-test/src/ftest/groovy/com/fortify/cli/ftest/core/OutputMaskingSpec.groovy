package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared

/**
 * Verifies that sensitive values are properly masked across all output channels,
 * and that output is routed to the correct stream (stdout vs stderr).
 *
 * Tests cover two scenarios:
 *
 * 1. Direct CLI: out.write, log.info, and log.warn all mask the sensitive --secret
 *    value. Each channel is tested individually to verify both masking and correct
 *    stream routing (stdout vs stderr).
 *
 * 2. run.fcli collection: when an inner command's stdout/stderr is collected via
 *    run.fcli's stdout/stderr: collect, the collected output is captured correctly.
 *    When the wrapper action writes the collected output back, it flows through the
 *    MaskingPrintStream so the final visible output has the secret masked.
 */
@Prefix("core.output-masking")
class OutputMaskingSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/output-masking.yaml") String actionPath
    @Shared @TestResource("runtime/actions/output-masking-run-fcli.yaml") String wrapperActionPath

    // Secret value used in all tests (>4 chars to pass LogMaskHelper minimum length check)
    static final String SECRET = "SuperSecret123"
    // Expected masking replacement: <REDACTED {OPTION_KEY} ({SOURCE})>
    // Option key "secret" is uppercased to "SECRET"; source is CLI_OPTION
    static final String MASKED = "<REDACTED SECRET (CLI_OPTION)>"

    // Standard flags for running unsigned test actions
    private static final String ACTION_FLAGS = "--on-unsigned=ignore --on-invalid-version=ignore"

    private Fcli.FcliResult runAction(String mode, boolean expectStderr = false) {
        // For modes that intentionally write to stderr (stderr, warn), use a relaxed
        // validator that only checks exit code, since the default validator treats any
        // stderr output as a failure.
        def validator = expectStderr ? { it.expectZeroExitCode() } : { it.expectSuccess() }
        Fcli.run("action run ${actionPath} --progress=none ${ACTION_FLAGS} --mode ${mode} --secret ${SECRET}", validator)
    }

    private Fcli.FcliResult runWrapper(String innerMode, boolean expectStderr = false) {
        def validator = expectStderr ? { it.expectZeroExitCode() } : { it.expectSuccess() }
        Fcli.run("action run ${wrapperActionPath} --progress=none ${ACTION_FLAGS} --action-path ${actionPath} --inner-mode ${innerMode} --secret ${SECRET}", validator)
    }

    // ==================== Direct CLI: masking and channel routing ====================

    def "out.write to stdout: secret is masked, output appears on stdout only"() {
        // out.write stdout → System.out → DelegatingPrintStream → stack → MaskingPrintStream
        when:
            def result = runAction("stdout")
        then:
            verifyAll(result) {
                // Secret must never appear in raw form on any channel
                !stdout.any { it.contains(SECRET) }
                !stderr.any { it.contains(SECRET) }
                // Masked replacement must appear on stdout (the target channel)
                stdout.any { it.contains(MASKED) }
            }
    }

    def "out.write to stderr: secret is masked, output appears on stderr only"() {
        // out.write stderr → System.err → DelegatingPrintStream → stack → MaskingPrintStream
        when:
            def result = runAction("stderr", true)
        then:
            verifyAll(result) {
                !stdout.any { it.contains(SECRET) }
                !stderr.any { it.contains(SECRET) }
                // Masked replacement must appear on stderr (the target channel)
                stderr.any { it.contains(MASKED) }
            }
    }

    def "log.info: secret is masked, routed to stdout"() {
        // log.info → progressWriter.writeInfo() → System.out.println() → masked via stack
        when:
            def result = runAction("info")
        then:
            verifyAll(result) {
                !stdout.any { it.contains(SECRET) }
                !stderr.any { it.contains(SECRET) }
                stdout.any { it.contains(MASKED) }
            }
    }

    def "log.warn: secret is masked, routed to stderr"() {
        // log.warn → progressWriter.writeWarning() → System.err.println() → masked via stack
        when:
            def result = runAction("warn", true)
        then:
            verifyAll(result) {
                !stdout.any { it.contains(SECRET) }
                !stderr.any { it.contains(SECRET) }
                stderr.any { it.contains(MASKED) }
            }
    }

    // ==================== run.fcli: collection and masking ====================

    def "run.fcli stdout:collect captures inner stdout, masking applied on final output"() {
        // run.fcli with stdout:collect pushes a CollectingPrintStream onto the stdout stack.
        // The inner command's stdout is captured there (above MaskingPrintStream, so raw).
        // When the wrapper writes the collected output back via out.write, it flows through
        // MaskingPrintStream, so the final visible output has the secret masked.
        when:
            def result = runWrapper("stdout")
        then:
            verifyAll(result) {
                // Secret must not appear in raw form
                !stdout.any { it.contains(SECRET) }
                !stderr.any { it.contains(SECRET) }
                // Collected stdout should contain the masked value
                stdout.any { it.startsWith("collected-stdout:") && it.contains(MASKED) }
                // Collected stderr should be empty (inner only wrote to stdout)
                stdout.any { it == "collected-stderr:" }
            }
    }

    def "run.fcli stderr:collect captures inner stderr, masking applied on final output"() {
        // Same as above but the inner command writes to stderr instead
        when:
            def result = runWrapper("stderr")
        then:
            verifyAll(result) {
                !stdout.any { it.contains(SECRET) }
                !stderr.any { it.contains(SECRET) }
                // Collected stdout should be empty (inner only wrote to stderr)
                stdout.any { it == "collected-stdout:" }
                // Collected stderr should contain the masked value
                stdout.any { it.startsWith("collected-stderr:") && it.contains(MASKED) }
            }
    }

    def "run.fcli: log.progress bypasses stdout/stderr collection"() {
        // log.progress → writeProgress() → progressOut (captured at ProgressWriter construction)
        // progressOut is StdioHelper.getProgressOut() which points to maskedOut — the original
        // masked stdout, NOT the CollectingPrintStream pushed by OutputHelper.
        // In the reflective test runner, progressOut writes to rawOut (the pre-install
        // System.out), which bypasses the DelegatingPrintStream and thus the test capturer,
        // so we can't assert where progress ended up. In external mode, subprocess stdout
        // is fully captured, so we can verify progress output arrived there (masked).
        when:
            def result = runWrapper("progress")
        then:
            verifyAll(result) {
                // Secret must not appear in raw form anywhere
                !stdout.any { it.contains(SECRET) }
                !stderr.any { it.contains(SECRET) }
                // Progress output must NOT be captured by run.fcli's stdout/stderr collection.
                // Both should be empty because writeProgress() goes to progressOut, not
                // through System.out/System.err (the DelegatingPrintStreams).
                stdout.any { it == "collected-stdout:" }
                stdout.any { it == "collected-stderr:" }
            }
            // In external mode, progress output is visible on the subprocess's stdout
            // (progressOut → maskedOut → rawOut, which is the process's real stdout).
            // Verify it arrived there, masked.
            if ( !Fcli.isReflective() ) {
                assert result.stdout.any { it.contains(MASKED) }
            }
    }
}
