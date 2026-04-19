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
 * - Progress messages are sent as JSON-RPC notifications via the per-thread progress
 *   callback in StdioHelper, wired through AsyncJobManager to IJobEventListener.onProgress.
 */
@Prefix("core.output-masking-rpc")
class OutputMaskingRPCSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/output-masking.yaml") String actionPath

    // Same secret value as OutputMaskingSpec
    static final String SECRET = "SuperSecret123"
    private static final String ACTION_FLAGS = "--on-unsigned=ignore --on-invalid-version=ignore"

    // Expected masking replacement for the secret option
    static final String MASKED = "<REDACTED SECRET (CLI_OPTION)>"

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

    def "fcli.execute sends progress messages as masked JSON-RPC notifications"() {
        // log.progress → writeProgress() → per-thread callback (set by AsyncJobManager)
        // → listener.onProgress(jobId, maskedMessage) → RPCPushJobEventListener
        // → RPCNotification.jobProgress → sent as JSON-RPC notification.
        // Using --progress=simple so writeProgress() actually produces output.
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def cmd = "action run ${actionPath} --progress=simple ${ACTION_FLAGS} --mode progress --secret ${SECRET}".toString()
                // Use rpcCallWithNotifications to capture notifications alongside the response.
                // The job starts immediately; progress notification arrives shortly after.
                def callResult = server.rpcCallWithNotifications("fcli.execute",
                    [command: cmd, cache: [ttl: "10m"]], 1, 2000)
                assert callResult.response != null
                assert callResult.response.get("error") == null

                // Wait for the job to complete so all notifications are sent
                def jobId = callResult.response.get("result").get("jobId").asText()
                def deadline = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < deadline) {
                    def pageResponse = server.rpcCall("job.getPage", [jobId: jobId, offset: 0, limit: 100], 2)
                    if (pageResponse.get("result")?.get("pagination")?.get("complete")?.asBoolean()) break
                    Thread.sleep(100)
                }

                // Drain any additional notifications that arrived during polling
                def lateNotifications = server.drainNotifications(1000)
                def allNotifications = callResult.notifications + lateNotifications

                // Verify that at least one job.progress notification was received
                def progressNotifications = allNotifications.findAll {
                    it.get("method")?.asText() == "job.progress"
                }
                assert progressNotifications.size() > 0 : "Expected at least one job.progress notification, got notifications: ${allNotifications}"

                // Collect all progress messages
                def progressMessages = progressNotifications.collect {
                    it.get("params")?.get("message")?.asText()
                }

                // Verify no progress message contains the raw secret
                assert !progressMessages.any { it?.contains(SECRET) } : "No progress notification may contain raw secret"
                // Verify at least one progress message contains the masked replacement
                // (the action's log.progress step writes "value=<secret>" which should be masked)
                assert progressMessages.any { it?.contains(MASKED) } : "At least one progress notification should contain masked replacement, got: ${progressMessages}"
            } finally {
                server.close()
            }
    }
}
