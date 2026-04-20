package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.RPCServerHelper
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared

@Prefix("core.rpc-server")
class RPCServerSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/server-import-functions.yaml") String importActionPath
    @Shared @TestResource("runtime/actions/server-global-vars.yaml") String globalVarsActionPath

    def "imported non-streaming function executes via RPC"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def result = server.executeAndWait("fn.call", [name: "echo", args: [message: "hello-rpc"]], 1, 2)
                assert result != null
                assert result.get("records")?.size() > 0
                assert result.get("records").get(0).asText().contains("hello-rpc")
            } finally {
                server.close()
            }
    }

    def "imported multiply function computes correctly via RPC"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def result = server.executeAndWait("fn.call", [name: "multiply", args: [x: 6, y: 7]], 3, 4)
                assert result != null
                assert result.get("records")?.size() > 0
                assert result.get("records").get(0).asText().contains("42")
            } finally {
                server.close()
            }
    }

    def "imported streaming function returns paged results via job.getPage"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                // fn.call always starts an async job now;
                // results are retrieved via job.getPage
                def streamResponse = server.rpcCall("fn.call", [name: "generateItems", args: [items: [0, 1, 2]], cache: [ttl: "10m"]], 3)
                assert streamResponse.get("result") != null
                assert streamResponse.get("error") == null
                def jobId = streamResponse.get("result").get("jobId").asText()
                assert jobId != null && !jobId.isEmpty()
                assert streamResponse.get("result").get("status").asText() == "started"
                // Poll job.getPage until complete
                def result = null
                def deadline = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < deadline) {
                    def pageResponse = server.rpcCall("job.getPage", [jobId: jobId, offset: 0, limit: 100], 4)
                    assert pageResponse.get("error") == null
                    result = pageResponse.get("result")
                    if (result.get("pagination")?.get("complete")?.asBoolean()) break
                    Thread.sleep(100)
                }
                assert result != null
                def records = result.get("records")
                assert records != null
                assert records.toString().contains("item-0")
                assert records.toString().contains("item-1")
                assert records.toString().contains("item-2")
            } finally {
                server.close()
            }
    }

    def "internal (export=false) function is NOT accessible via fn.call"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                // Non-exported functions are not registered; fn.call returns method not found
                def response = server.rpcCall("fn.call", [name: "_helperInternal", args: [:]], 4)
                assert response.get("error") != null
                assert response.get("error").get("code").asInt() == -32601 // method not found
            } finally {
                server.close()
            }
    }

    def "rpc.listMethods shows all default methods including fn.call and fn.list"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def response = server.rpcCall("rpc.listMethods", null, 5)
                assert response.get("result") != null
                def methods = response.get("result").get("methods")
                def methodNames = [] as Set
                for (def m : methods) {
                    methodNames.add(m.get("name").asText())
                }
                // Should have all default methods
                assert methodNames.contains("rpc.listMethods")
                assert methodNames.contains("fcli.buildInfo")
                assert methodNames.contains("fcli.execute")
                assert methodNames.contains("fcli.listCommands")
                assert methodNames.contains("fcli.getCommandDetails")
                assert methodNames.contains("job.getPage")
                assert methodNames.contains("job.cancel")
                assert methodNames.contains("job.list")
                // fn dispatch methods should always be present
                assert methodNames.contains("fn.call")
                assert methodNames.contains("fn.list")
                // Per-function methods are no longer registered; functions are accessed via fn.call
                assert !methodNames.contains("fn.echo")
                assert !methodNames.contains("fn.multiply")
                assert !methodNames.contains("fn.generateItems")
            } finally {
                server.close()
            }
    }

    def "fn.list returns all exported imported functions"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def response = server.rpcCall("fn.list", null, 6)
                assert response.get("result") != null
                assert response.get("error") == null
                def fns = response.get("result").get("functions")
                assert fns != null && fns.isArray()
                def fnNames = [] as Set
                for (def f : fns) {
                    fnNames.add(f.get("name").asText())
                }
                assert fnNames.contains("echo")
                assert fnNames.contains("multiply")
                assert fnNames.contains("generateItems")
                // Non-exported functions must not appear
                assert !fnNames.contains("_helperInternal")
            } finally {
                server.close()
            }
    }

    def "fcli.execute returns stdout via job.getPage"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def result = server.executeAndWait("fcli.execute", [command: "util sample-data list", collectRecords: false], 7, 8)
                assert result.get("exitCode").asInt() == 0
                assert result.has("stdout")
            } finally {
                server.close()
            }
    }

    def "fcli.execute with collectRecords returns records via job.getPage"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def result = server.executeAndWait("fcli.execute", [command: "util sample-data list", collectRecords: true], 9, 10)
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                assert result.get("records").isArray()
                assert result.get("records").size() > 0
            } finally {
                server.close()
            }
    }

    def "fcli.execute with collectRecords returns paginated results via job.getPage"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def startResponse = server.rpcCall("fcli.execute", [command: "util sample-data list", collectRecords: true, cache: [ttl: "10m"]], 11)
                def jobId = startResponse.get("result").get("jobId").asText()

                // Poll until complete
                def result = null
                def deadline = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < deadline) {
                    def pageResponse = server.rpcCall("job.getPage", [jobId: jobId, offset: 0, limit: 5], 12)
                    assert pageResponse.get("error") == null
                    result = pageResponse.get("result")
                    if (result.get("pagination")?.get("complete")?.asBoolean()) break
                    Thread.sleep(100)
                }
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                def pagination = result.get("pagination")
                assert pagination != null
                assert pagination.get("offset").asInt() == 0
                assert pagination.get("limit").asInt() == 5
                assert pagination.has("loadedCount")
                assert pagination.has("hasMore")
                assert pagination.get("complete").asBoolean()
            } finally {
                server.close()
            }
    }

    def "fcli.listCommands with no query returns all commands"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.listCommands", [:], 17)
                assert response.get("error") == null
                def result = response.get("result")
                assert result != null
                assert result.has("commands")
                assert result.get("commands").isArray()
                assert result.get("commands").size() > 0
                assert result.has("count")
                assert result.get("count").asInt() == result.get("commands").size()
            } finally {
                server.close()
            }
    }

    def "fcli.listCommands with SpEL query filters commands"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def allResponse = server.rpcCall("fcli.listCommands", [:], 18)
                def allCount = allResponse.get("result").get("count").asInt()

                def filteredResponse = server.rpcCall("fcli.listCommands", [query: "module=='util'"], 19)
                assert filteredResponse.get("error") == null
                def result = filteredResponse.get("result")
                assert result != null
                def commands = result.get("commands")
                assert commands.isArray()
                assert commands.size() > 0
                assert commands.size() < allCount
                for (def cmd : commands) {
                    assert cmd.get("module").asText() == "util"
                }
            } finally {
                server.close()
            }
    }

    def "fcli.listCommands with invalid SpEL query returns error"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.listCommands", [query: "!!!invalid!!!"], 20)
                assert response.get("error") != null
                assert response.get("result") == null
            } finally {
                server.close()
            }
    }

    def "fcli.execute action run has isolated global vars per invocation"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def actionCmd = "action run ${globalVarsActionPath} --on-unsigned ignore" as String
                // First invocation: set global var 'color' to 'red'; old value should be null
                def result1 = server.executeAndWait("fcli.execute",
                    [command: "${actionCmd} --key color --value red" as String], 21, 22)
                assert result1 != null : "No result in response"
                def stdout1 = result1.get("stdout")?.asText() ?: ""
                def stderr1 = result1.get("stderr")?.asText() ?: ""
                assert stdout1.contains("old=,") : "Expected old=, in stdout but got stdout='${stdout1}', stderr='${stderr1}'"
                assert stdout1.contains("new=red")

                // Second invocation: set same key again; old value should still be empty (isolated context)
                def result2 = server.executeAndWait("fcli.execute",
                    [command: "${actionCmd} --key color --value blue" as String], 23, 24)
                def stdout2 = result2.get("stdout")?.asText() ?: ""
                assert stdout2.contains("old=,") : "Expected isolated context but got: ${stdout2}"
                assert stdout2.contains("new=blue")

                // Third invocation with a different key: also should be empty (fresh context)
                def result3 = server.executeAndWait("fcli.execute",
                    [command: "${actionCmd} --key size --value large" as String], 25, 26)
                def stdout3 = result3.get("stdout")?.asText() ?: ""
                assert stdout3.contains("old=,")
                assert stdout3.contains("new=large")
            } finally {
                server.close()
            }
    }

    def "imported function shares global vars across invocations"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${globalVarsActionPath}")
        then:
            try {
                // First call: set global 'color' to 'red'; no previous value
                def r1 = server.executeAndWait("fn.call", [name: "setAndGetGlobal", args: [key: "color", value: "red"]], 24, 25)
                assert r1 != null
                assert r1.get("records")?.size() > 0
                assert r1.get("records").get(0).asText() == "old=,new=red"

                // Second call: set same key to 'blue'; should see previous value 'red'
                def r2 = server.executeAndWait("fn.call", [name: "setAndGetGlobal", args: [key: "color", value: "blue"]], 26, 27)
                assert r2.get("records").get(0).asText() == "old=red,new=blue"

                // Third call: read the value back via getGlobal
                def r3 = server.executeAndWait("fn.call", [name: "getGlobal", args: [key: "color"]], 28, 29)
                assert r3.get("records").get(0).asText() == "blue"

                // Fourth call: set a different key; 'color' should still be there
                def r4 = server.executeAndWait("fn.call", [name: "setAndGetGlobal", args: [key: "size", value: "large"]], 30, 31)
                assert r4.get("records").get(0).asText() == "old=,new=large"

                // Verify both keys are present
                def r5 = server.executeAndWait("fn.call", [name: "getGlobal", args: [key: "color"]], 32, 33)
                assert r5.get("records").get(0).asText() == "blue"

                def r6 = server.executeAndWait("fn.call", [name: "getGlobal", args: [key: "size"]], 34, 35)
                assert r6.get("records").get(0).asText() == "large"
            } finally {
                server.close()
            }
    }

    def "job.cancel on non-existent job returns success=false"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("job.cancel", [jobId: "non-existent-job-id"], 40)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("success").asBoolean() == false
                assert result.get("jobId").asText() == "non-existent-job-id"
            } finally {
                server.close()
            }
    }

    def "job.cancel cancels a running streaming job"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                // Start a job that generates items with 100ms delay each (50 items = ~5s)
                def items = (0..<50).collect { it }
                def startResponse = server.rpcCall("fn.call",
                    [name: "generateManyItems", args: [items: items]], 41)
                assert startResponse.get("error") == null
                def jobId = startResponse.get("result").get("jobId").asText()

                // Wait a bit for some items to be produced, then cancel
                Thread.sleep(500)

                def cancelResponse = server.rpcCall("job.cancel", [jobId: jobId], 42)
                assert cancelResponse.get("error") == null
                assert cancelResponse.get("result").get("success").asBoolean() == true
                assert cancelResponse.get("result").get("jobId").asText() == jobId

                // Drain any remaining notifications
                server.drainNotifications(2000)
            } finally {
                server.close()
            }
    }

    def "job.list shows tracked jobs"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                // Start a job and wait for it to complete
                server.executeAndWait("fn.call", [name: "echo", args: [message: "test"]], 50, 51)

                // Drain any notifications from the echo job
                server.drainNotifications(500)

                // List jobs — should show the completed job
                def listResponse = server.rpcCall("job.list", [:], 52)
                assert listResponse.get("error") == null
                def result = listResponse.get("result")
                assert result.get("totalJobs").asInt() >= 1
                def jobs = result.get("jobs")
                assert jobs.isArray()
                assert jobs.size() >= 1
                // Find the completed job
                def completedJob = null
                for (def j : jobs) {
                    if (j.get("completed").asBoolean()) {
                        completedJob = j
                        break
                    }
                }
                assert completedJob != null : "Expected at least one completed job in: ${jobs}"
                assert completedJob.has("jobId")
                assert completedJob.has("description")
                assert completedJob.has("createdMillis")
            } finally {
                server.close()
            }
    }

    def "job.getPage with unknown jobId returns not_found status"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("job.getPage", [jobId: "does-not-exist", offset: 0, limit: 10], 60)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("status").asText() == "not_found"
                assert result.get("jobId").asText() == "does-not-exist"
            } finally {
                server.close()
            }
    }

    def "job.getPage without jobId returns error"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("job.getPage", [:], 61)
                assert response.get("error") != null
                assert response.get("error").get("code").asInt() == -32602 // invalid params
            } finally {
                server.close()
            }
    }

    def "fcli.execute with invalid command returns non-zero exit code"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def result = server.executeAndWait("fcli.execute",
                    [command: "this-command-does-not-exist"], 70, 71)
                assert result.get("exitCode").asInt() != 0
                assert result.has("stderr")
            } finally {
                server.close()
            }
    }

    def "push notifications are received for async jobs"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                // Start a streaming function that produces multiple items with delays,
                // ensuring records arrive as push notifications before job completion
                def items = (0..<5).collect { it }
                def callResult = server.rpcCallWithNotifications("fn.call",
                    [name: "generateManyItems", args: [items: items]], 80, 5000)
                assert callResult.response != null
                assert callResult.response.get("error") == null
                def jobId = callResult.response.get("result").get("jobId").asText()

                // Collect all notifications (pre-response + post-response)
                def notifications = callResult.notifications

                // Also drain any remaining notifications
                notifications.addAll(server.drainNotifications(5000))

                // Should have job.started, job.records, and job.complete notifications
                def methods = notifications.collect { it.get("method")?.asText() } as Set
                assert methods.contains("job.started") : "Expected job.started notification, got: ${methods}"
                assert methods.contains("job.records") : "Expected job.records notification (streaming records should be pushed), got: ${methods}"
                assert methods.contains("job.complete") : "Expected job.complete notification, got: ${methods}"

                // Verify job.started has correct jobId
                def startedNotif = notifications.find { it.get("method")?.asText() == "job.started" }
                assert startedNotif.get("params").get("jobId").asText() == jobId

                // Verify job.records has correct jobId and records array
                def recordNotifs = notifications.findAll { it.get("method")?.asText() == "job.records" }
                def totalPushedRecords = 0
                for (def notif : recordNotifs) {
                    assert notif.get("params").get("jobId").asText() == jobId
                    assert notif.get("params").get("records").isArray()
                    totalPushedRecords += notif.get("params").get("records").size()
                }
                assert totalPushedRecords == 5 : "Expected 5 records total in job.records notifications, got: ${totalPushedRecords}"

                // Verify job.complete has correct jobId and exitCode
                def completeNotif = notifications.find { it.get("method")?.asText() == "job.complete" }
                assert completeNotif.get("params").get("jobId").asText() == jobId
                assert completeNotif.get("params").get("exitCode").asInt() == 0
            } finally {
                server.close()
            }
    }

    def "fcli.getCommandDetails returns usage info"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.getCommandDetails",
                    [command: "util sample-data list"], 90)
                assert response.get("error") == null
                def result = response.get("result")
                assert result != null
                assert result.has("command")
                assert result.get("command").asText().contains("sample-data")
            } finally {
                server.close()
            }
    }

    def "fcli.getCommandDetails with invalid command returns error"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.getCommandDetails",
                    [command: "nonexistent command path"], 91)
                assert response.get("error") != null
            } finally {
                server.close()
            }
    }

    def "fcli.buildInfo returns version information"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.buildInfo", [:], 95)
                assert response.get("error") == null
                def result = response.get("result")
                assert result != null
                assert result.has("version")
            } finally {
                server.close()
            }
    }

    // --- Cache and push parameter tests ---

    def "fcli.execute with cache returns cached=true and records via job.getPage"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def startResponse = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", cache: [ttl: "10m"], push: false], 100)
                assert startResponse.get("error") == null
                def startResult = startResponse.get("result")
                assert startResult.get("cached").asBoolean() == true

                def jobId = startResult.get("jobId").asText()
                def result = server.executeAndWait("fcli.execute",
                    [command: "util sample-data list", cache: [ttl: "10m"], push: false], 101, 102)
                assert result.get("exitCode").asInt() == 0
                assert result.get("records").isArray()
                assert result.get("records").size() > 0
            } finally {
                server.close()
            }
    }

    def "fcli.execute without cache returns cached=false and getPage returns not_found"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def startResponse = server.rpcCall("fcli.execute",
                    [command: "util sample-data list"], 110)
                assert startResponse.get("error") == null
                def startResult = startResponse.get("result")
                assert startResult.get("cached").asBoolean() == false

                def jobId = startResult.get("jobId").asText()

                // Wait for completion via job.getStatus polling
                def deadline = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < deadline) {
                    def statusResp = server.rpcCall("job.getStatus", [jobId: jobId], 111)
                    if (statusResp.get("result")?.get("completed")?.asBoolean()) break
                    Thread.sleep(100)
                }

                // Drain push notifications
                server.drainNotifications(1000)

                // job.getPage should return not_found since cache was not enabled
                def pageResponse = server.rpcCall("job.getPage", [jobId: jobId, offset: 0, limit: 10], 112)
                assert pageResponse.get("error") == null
                assert pageResponse.get("result").get("status").asText() == "not_found"
            } finally {
                server.close()
            }
    }

    def "fcli.execute with cache accepts different TTL values"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def startResponse = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", cache: [ttl: "5m"]], 120)
                assert startResponse.get("error") == null
                assert startResponse.get("result").get("cached").asBoolean() == true
            } finally {
                server.close()
            }
    }

    def "fcli.execute with cache:true without TTL returns error"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", cache: true], 125)
                assert response.get("error") != null
                assert response.get("error").get("code").asInt() == -32602
                assert response.get("error").get("message").asText().contains("ttl")
            } finally {
                server.close()
            }
    }

    def "fcli.execute with push false suppresses notifications"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def callResult = server.rpcCallWithNotifications("fcli.execute",
                    [command: "util sample-data list", push: false, cache: [ttl: "10m"]], 130, 3000)
                assert callResult.response.get("error") == null
                def jobId = callResult.response.get("result").get("jobId").asText()

                // Wait for job to complete
                def deadline = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < deadline) {
                    def statusResp = server.rpcCall("job.getStatus", [jobId: jobId], 131)
                    if (statusResp.get("result")?.get("completed")?.asBoolean()) break
                    Thread.sleep(100)
                }

                // Drain any remaining notifications
                def allNotifications = callResult.notifications + server.drainNotifications(1000)

                // With push:false, there should be no push notifications for this job
                def jobNotifications = allNotifications.findAll {
                    it.get("params")?.get("jobId")?.asText() == jobId
                }
                assert jobNotifications.isEmpty() : "Expected no push notifications with push:false, got: ${jobNotifications.size()}"
            } finally {
                server.close()
            }
    }

    // --- job.getStatus tests ---

    def "job.getStatus returns status for a completed job"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def result = server.executeAndWait("fcli.execute",
                    [command: "util sample-data list"], 140, 141)
                // executeAndWait uses job.getPage internally; get the jobId from a fresh call
                def startResponse = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", cache: [ttl: "10m"]], 142)
                def jobId = startResponse.get("result").get("jobId").asText()

                def deadline = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < deadline) {
                    def statusResp = server.rpcCall("job.getStatus", [jobId: jobId], 143)
                    def statusResult = statusResp.get("result")
                    if (statusResult.get("completed")?.asBoolean()) {
                        assert statusResult.get("status").asText() == "complete"
                        assert statusResult.get("jobId").asText() == jobId
                        assert statusResult.has("cached")
                        assert statusResult.has("createdMillis")
                        assert statusResult.get("exitCode").asInt() == 0
                        return
                    }
                    Thread.sleep(100)
                }
                assert false : "Job did not complete within timeout"
            } finally {
                server.close()
            }
    }

    def "job.getStatus returns not_found for unknown jobId"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("job.getStatus", [jobId: "does-not-exist"], 150)
                assert response.get("error") == null
                assert response.get("result").get("status").asText() == "not_found"
            } finally {
                server.close()
            }
    }

    // --- job.remove tests ---

    def "job.remove removes a completed job and its cache"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def result = server.executeAndWait("fcli.execute",
                    [command: "util sample-data list", cache: [ttl: "10m"]], 160, 161)
                assert result.get("exitCode").asInt() == 0

                // Get the jobId — we need to start another job since executeAndWait doesn't return it
                def startResponse = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", cache: [ttl: "10m"]], 162)
                def jobId = startResponse.get("result").get("jobId").asText()

                // Wait for completion
                def deadline = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < deadline) {
                    def statusResp = server.rpcCall("job.getStatus", [jobId: jobId], 163)
                    if (statusResp.get("result")?.get("completed")?.asBoolean()) break
                    Thread.sleep(100)
                }
                server.drainNotifications(500)

                // Remove the job
                def removeResponse = server.rpcCall("job.remove", [jobId: jobId], 164)
                assert removeResponse.get("error") == null
                assert removeResponse.get("result").get("success").asBoolean() == true

                // Verify it's gone from both job tracking and cache
                def statusResponse = server.rpcCall("job.getStatus", [jobId: jobId], 165)
                assert statusResponse.get("result").get("status").asText() == "not_found"

                def pageResponse = server.rpcCall("job.getPage", [jobId: jobId, offset: 0, limit: 10], 166)
                assert pageResponse.get("result").get("status").asText() == "not_found"
            } finally {
                server.close()
            }
    }

    def "job.remove returns failure for unknown job"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("job.remove", [jobId: "does-not-exist"], 170)
                assert response.get("error") == null
                assert response.get("result").get("success").asBoolean() == false
            } finally {
                server.close()
            }
    }

    // --- rpc.listMethods includes new methods ---

    def "rpc.listMethods includes job.getStatus and job.remove"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("rpc.listMethods", null, 180)
                assert response.get("result") != null
                def methods = response.get("result").get("methods")
                def methodNames = [] as Set
                for (def m : methods) {
                    methodNames.add(m.get("name").asText())
                }
                assert methodNames.contains("job.getStatus")
                assert methodNames.contains("job.remove")
            } finally {
                server.close()
            }
    }

    // --- wait parameter tests ---

    def "fcli.execute with wait:true returns inline results"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", collectRecords: true, wait: true], 200)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("status").asText() == "completed"
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                assert result.get("records").isArray()
                assert result.get("records").size() > 0
                assert result.has("recordCount")
                assert result.get("recordCount").asInt() == result.get("records").size()
            } finally {
                server.close()
            }
    }

    def "fcli.execute with wait:true and collectRecords:false returns stdout inline"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", collectRecords: false, wait: true], 210)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("status").asText() == "completed"
                assert result.get("exitCode").asInt() == 0
                assert result.has("stdout")
                assert !result.has("records")
            } finally {
                server.close()
            }
    }

    def "fcli.execute with wait:{timeout:'30s'} returns inline results for fast command"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", collectRecords: true, wait: [timeout: "30s"]], 220)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("status").asText() == "completed"
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                assert result.get("records").isArray()
                assert result.get("records").size() > 0
            } finally {
                server.close()
            }
    }

    def "fcli.execute with wait:{timeout:'1s'} falls back to async for slow command"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                // generateManyItems with 50 items (~5s total) should exceed a 1s timeout
                def items = (0..<50).collect { it }
                def response = server.rpcCall("fn.call",
                    [name: "generateManyItems", args: [items: items], wait: [timeout: "1s"], cache: [ttl: "10m"]], 230)
                assert response.get("error") == null
                def result = response.get("result")
                // Should fall back to async response with jobId
                assert result.has("jobId")
                assert result.get("status").asText() == "started"

                // Drain notifications and let the job finish
                server.drainNotifications(5000)
            } finally {
                server.close()
            }
    }

    def "fn.call with wait:true returns inline results"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def response = server.rpcCall("fn.call",
                    [name: "echo", args: [message: "wait-test"], wait: true], 240)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("status").asText() == "completed"
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                assert result.get("records").size() > 0
                assert result.get("records").get(0).asText().contains("wait-test")
            } finally {
                server.close()
            }
    }

    def "fn.call with wait:{timeout:'30s'} returns inline results for fast function"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def response = server.rpcCall("fn.call",
                    [name: "multiply", args: [x: 3, y: 4], wait: [timeout: "30s"]], 250)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("status").asText() == "completed"
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                assert result.get("records").size() > 0
                assert result.get("records").get(0).asText().contains("12")
            } finally {
                server.close()
            }
    }

    def "fcli.execute with wait:true and failed command returns failed status"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.execute",
                    [command: "this-command-does-not-exist", wait: true], 260)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("status").asText() == "failed"
                assert result.get("exitCode").asInt() != 0
                assert result.has("stderr")
            } finally {
                server.close()
            }
    }

    def "fcli.execute with wait:false behaves like default async mode"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.execute",
                    [command: "util sample-data list", wait: false], 270)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.has("jobId")
                assert result.get("status").asText() == "started"

                // Drain notifications
                server.drainNotifications(5000)
            } finally {
                server.close()
            }
    }
}
