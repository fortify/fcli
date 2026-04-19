package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.RPCServerHelper
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared

/**
 * Verifies output behavior when commands are executed through the RPC server.
 *
 * Key requirements:
 * - No command output (stdout, stderr, progress) may leak onto the JSON-RPC protocol
 *   channel (stdout). Protocol integrity is implicitly verified by the ability to
 *   send/receive valid JSON-RPC messages throughout the test.
 * - fcli.execute must capture the inner command's stdout and stderr in the job result
 *   so the RPC client can access them.
 * - Progress messages should be sent as JSON-RPC notifications. Note: the notification
 *   plumbing exists (IJobEventListener.onProgress → RPCPushJobEventListener →
 *   RPCNotification.jobProgress) but is not yet wired — onProgress is never called.
 *   A future enhancement should connect progress writers to the notification system.
 */
@Prefix("core.output-masking-rpc")
class OutputMaskingRPCSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/output-masking.yaml") String actionPath

    // Same secret value as OutputMaskingSpec
    static final String SECRET = "SuperSecret123"
    private static final String ACTION_FLAGS = "--on-unsigned=ignore --on-invalid-version=ignore"

    def "fcli.execute captures stdout from action, protocol channel stays clean"() {
        // The action writes "value=<secret>" to stdout. fcli.execute runs this on a
        // background thread with stdout collection. The test verifies:
        // 1. Valid JSON-RPC exchange works (no output leaked to protocol channel)
        // 2. The job result includes captured stdout
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                // Use .toString() to convert GString to String; Jackson's valueToTree
                // doesn't serialize GString correctly, resulting in an empty command.
                def cmd = "action run ${actionPath} --progress=none ${ACTION_FLAGS} --mode stdout --secret ${SECRET}".toString()
                def result = server.executeAndWait("fcli.execute", [command: cmd], 1, 2)
                assert result != null

                // Verify stdout was captured in the job result
                def stdout = result.get("stdout")?.asText()
                assert stdout != null : "fcli.execute should capture stdout in job result"
                assert !stdout.isEmpty() : "stdout should not be empty for mode=stdout"
                assert stdout.contains("value=") : "stdout should contain the expected output format"
            } finally {
                server.close()
            }
    }

    def "fcli.execute captures stderr from log.warn, protocol channel stays clean"() {
        // log.warn writes to System.err via progressWriter.writeWarning(). The RPC server
        // collects stderr separately. This verifies stderr capture works and that warning
        // output doesn't leak to the JSON-RPC protocol channel.
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def cmd = "action run ${actionPath} --progress=none ${ACTION_FLAGS} --mode warn --secret ${SECRET}".toString()
                def result = server.executeAndWait("fcli.execute", [command: cmd], 1, 2)
                assert result != null

                // Verify stderr was captured in the job result
                def stderr = result.get("stderr")?.asText()
                assert stderr != null : "fcli.execute should capture stderr in job result"
                assert !stderr.isEmpty() : "stderr should not be empty for mode=warn"
                assert stderr.contains("value=") : "stderr should contain the expected output format"
            } finally {
                server.close()
            }
    }

    def "fcli.execute with log.info routes output to stdout capture, not protocol"() {
        // log.info calls writeInfo() which writes to System.out. In the RPC context,
        // this should be captured as part of the job's stdout, not sent to the raw
        // protocol channel. Verifies info messages don't corrupt JSON-RPC communication.
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def cmd = "action run ${actionPath} --progress=none ${ACTION_FLAGS} --mode info --secret ${SECRET}".toString()
                def result = server.executeAndWait("fcli.execute", [command: cmd], 1, 2)
                assert result != null

                // log.info → writeInfo → System.out.println → should be captured as stdout
                def stdout = result.get("stdout")?.asText()
                assert stdout != null : "log.info output should be captured in job stdout"
                assert stdout.contains("value=") : "stdout should contain the log.info message"
            } finally {
                server.close()
            }
    }
}
