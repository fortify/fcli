package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.RPCServerHelper
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared

@Prefix("core.rpc-server")
class RPCServerSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/server-import-functions.yaml") String importActionPath

    def "imported non-streaming function executes via RPC"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def response = server.rpcCall("fn.echo", [message: "hello-rpc"], 1)
                assert response.get("result") != null
                assert response.get("result").asText().contains("hello-rpc")
                assert response.get("error") == null
            } finally {
                server.close()
            }
    }

    def "imported multiply function computes correctly via RPC"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def response = server.rpcCall("fn.multiply", [x: 6, y: 7], 2)
                assert response.get("result") != null
                assert response.get("result").asText().contains("42")
                assert response.get("error") == null
            } finally {
                server.close()
            }
    }

    def "imported streaming function returns paged results via async.getPage"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                // Streaming functions with async=true start a background job;
                // results must be retrieved via async.getPage
                def streamResponse = server.rpcCall("fn.generateItems", [items: [0, 1, 2], async: true], 3)
                assert streamResponse.get("result") != null
                assert streamResponse.get("error") == null
                def jobId = streamResponse.get("result").get("jobId").asText()
                assert jobId != null && !jobId.isEmpty()
                assert streamResponse.get("result").get("status").asText() == "started"
                def pageResponse = server.rpcCall("async.getPage", [jobId: jobId, wait: true], 4)
                assert pageResponse.get("result") != null
                assert pageResponse.get("error") == null
                def records = pageResponse.get("result").get("records")
                assert records != null
                assert records.toString().contains("item-0")
                assert records.toString().contains("item-1")
                assert records.toString().contains("item-2")
            } finally {
                server.close()
            }
    }

    def "internal (export=false) function is NOT registered as RPC method"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                // Call the internal function should return method not found
                def response = server.rpcCall("fn._helperInternal", [:], 4)
                assert response.get("error") != null
                assert response.get("error").get("code").asInt() == -32601 // method not found
            } finally {
                server.close()
            }
    }

    def "rpc.listMethods shows all default methods and imported functions"() {
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
                assert methodNames.contains("async.getPage")
                assert methodNames.contains("async.getResult")
                assert methodNames.contains("async.cancel")
                assert methodNames.contains("async.clear")
                // Should also have imported exported functions
                assert methodNames.contains("fn.echo")
                assert methodNames.contains("fn.multiply")
                assert methodNames.contains("fn.generateItems")
                // Should NOT have internal function
                assert !methodNames.contains("fn._helperInternal")
            } finally {
                server.close()
            }
    }

    def "fcli.execute sync returns stdout"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.execute", [command: "util sample-data list"], 7)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("exitCode").asInt() == 0
                assert result.has("stdout")
                assert !result.has("records")
            } finally {
                server.close()
            }
    }

    def "fcli.execute sync with collectRecords returns records array"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def response = server.rpcCall("fcli.execute", [command: "util sample-data list", collectRecords: true], 8)
                assert response.get("error") == null
                def result = response.get("result")
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                assert result.get("records").isArray()
                assert result.get("records").size() > 0
                assert result.has("totalRecords")
                assert result.get("totalRecords").asInt() == result.get("records").size()
                assert !result.has("stdout")
            } finally {
                server.close()
            }
    }

    def "fcli.execute async with collectRecords returns jobId, retrievable via async.getResult"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def startResponse = server.rpcCall("fcli.execute", [command: "util sample-data list", async: true, collectRecords: true], 9)
                assert startResponse.get("error") == null
                def startResult = startResponse.get("result")
                assert startResult.get("status").asText() == "started"
                assert startResult.get("jobType").asText() == "records"
                def jobId = startResult.get("jobId").asText()
                assert jobId != null && !jobId.isEmpty()

                def resultResponse = server.rpcCall("async.getResult", [jobId: jobId, wait: true], 10)
                assert resultResponse.get("error") == null
                def result = resultResponse.get("result")
                assert result.get("status").asText() == "complete"
                assert result.get("jobId").asText() == jobId
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                assert result.get("records").isArray()
                assert result.get("records").size() > 0
                assert !result.has("stdout")
            } finally {
                server.close()
            }
    }

    def "fcli.execute async stdout job retrievable via async.getPage with pagination"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def startResponse = server.rpcCall("fcli.execute", [command: "util sample-data list", async: true, collectRecords: true], 11)
                def jobId = startResponse.get("result").get("jobId").asText()

                def pageResponse = server.rpcCall("async.getPage", [jobId: jobId, wait: true, offset: 0, limit: 5], 12)
                assert pageResponse.get("error") == null
                def result = pageResponse.get("result")
                assert result.get("status").asText() == "complete"
                assert result.get("jobId").asText() == jobId
                assert result.get("exitCode").asInt() == 0
                assert result.has("records")
                assert result.has("loadedCount")
                def pagination = result.get("pagination")
                assert pagination != null
                assert pagination.get("offset").asInt() == 0
                assert pagination.get("limit").asInt() == 5
                assert pagination.has("totalRecords")
                assert pagination.has("hasMore")
                assert pagination.get("complete").asBoolean()
            } finally {
                server.close()
            }
    }

    def "async.clear removes specific job; subsequent lookup returns not_found"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start")
        then:
            try {
                def startResponse = server.rpcCall("fcli.execute", [command: "util sample-data list", async: true, collectRecords: true], 13)
                def jobId = startResponse.get("result").get("jobId").asText()
                // Ensure job completes
                server.rpcCall("async.getResult", [jobId: jobId, wait: true], 14)

                def clearResponse = server.rpcCall("async.clear", [jobId: jobId], 15)
                assert clearResponse.get("error") == null
                assert clearResponse.get("result").get("success").asBoolean()
                assert clearResponse.get("result").get("jobId").asText() == jobId

                def getResponse = server.rpcCall("async.getResult", [jobId: jobId, wait: false], 16)
                assert getResponse.get("result").get("status").asText() == "not_found"
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
}
