package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.RPCServerHelper
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared

@Prefix("core.rpc-server.import")
class RPCServerImportSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/server-import-functions.yaml") String importActionPath

    def "imported non-streaming function executes via RPC"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath} --no-defaults")
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
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath} --no-defaults")
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

    def "imported streaming function returns results via RPC"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath} --no-defaults")
        then:
            try {
                // Streaming functions return a cacheKey; results must be retrieved via rpc.getPage
                def streamResponse = server.rpcCall("fn.generateItems", [items: [0, 1, 2]], 3)
                assert streamResponse.get("result") != null
                assert streamResponse.get("error") == null
                def cacheKey = streamResponse.get("result").get("cacheKey").asText()
                assert cacheKey != null && !cacheKey.isEmpty()
                def pageResponse = server.rpcCall("rpc.getPage", [cacheKey: cacheKey, wait: true], 4)
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
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath} --no-defaults")
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

    def "rpc.listMethods shows only exported functions and rpc.listMethods when --no-defaults"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath} --no-defaults")
        then:
            try {
                def response = server.rpcCall("rpc.listMethods", null, 5)
                assert response.get("result") != null
                def methods = response.get("result").get("methods")
                def methodNames = [] as Set
                for (def m : methods) {
                    methodNames.add(m.get("name").asText())
                }
                // Should have exported functions + rpc.listMethods
                assert methodNames.contains("fn.echo")
                assert methodNames.contains("fn.multiply")
                assert methodNames.contains("fn.generateItems")
                assert methodNames.contains("rpc.listMethods")
                // Should NOT have internal function
                assert !methodNames.contains("fn._helperInternal")
                // Should NOT have default fcli methods (--no-defaults)
                assert !methodNames.contains("fcli.execute")
                assert !methodNames.contains("fcli.version")
            } finally {
                server.close()
            }
    }

    def "default methods available when --no-defaults not specified"() {
        when:
            def server = RPCServerHelper.start("util rpc-server start --import ${importActionPath}")
        then:
            try {
                def response = server.rpcCall("rpc.listMethods", null, 6)
                def methods = response.get("result").get("methods")
                def methodNames = [] as Set
                for (def m : methods) {
                    methodNames.add(m.get("name").asText())
                }
                // Should have both default methods and imported functions
                assert methodNames.contains("fcli.version")
                assert methodNames.contains("fcli.execute")
                assert methodNames.contains("fn.echo")
                assert methodNames.contains("fn.multiply")
            } finally {
                server.close()
            }
    }
}
