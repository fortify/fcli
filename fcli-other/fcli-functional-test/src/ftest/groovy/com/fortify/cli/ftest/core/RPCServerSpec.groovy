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
                def streamResponse = server.rpcCall("fn.call", [name: "generateItems", args: [items: [0, 1, 2]]], 3)
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
                def result = server.executeAndWait("fcli.execute", [command: "util sample-data list"], 7, 8)
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
                def startResponse = server.rpcCall("fcli.execute", [command: "util sample-data list", collectRecords: true], 11)
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
}
