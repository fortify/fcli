package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.fod._common.FoDWebAppSupplier
import com.fortify.cli.ftest.fod._common.FoDUserSupplier
import com.fortify.cli.ftest.fod._common.FoDUserGroupSupplier

import com.fasterxml.jackson.databind.ObjectMapper

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.rest") @FcliSession(FOD) @Stepwise
class FoDRestSpec extends FcliBaseSpec {
    @Shared @TempDir("fod/rest") String tempDir;
    @Shared @TestResource("runtime/actions/rest-call-response-types.yaml") String responseTypesActionPath
    
    def "list"() {
        def args = "fod rest call /api/v3/tenants"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=4
                it[1].contains("tenantName:")
            }
    }
    
    def "call.response-file"() {
        def file = new File(tempDir, "tenants.json")
        def args = "fod rest call /api/v3/tenants --response-file=${file.absolutePath} -o json"
        when:
            def result = Fcli.run(args)
        then:
            def output = new ObjectMapper().readTree(result.stdout.join("\n"))
            def record = output.isArray() ? output.get(0) : output
            verifyAll(record) {
                get("status").asInt() == 200
                get("file").asText() == file.absolutePath
                get("size").asLong() > 0
            }
            file.exists()
            new ObjectMapper().readTree(file) != null
    }
    
    def "action.rest-call.response-types"() {
        def file = new File(tempDir, "tenants-action.json")
        // The temporary directory is outside the working directory, so unrestricted paths must be allowed
        def args = "fod action run ${responseTypesActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --allow-unrestricted-file-paths --file ${file.absolutePath}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("FILE-OK status=200") }
                it.any { it == "TEXT-OK nonempty" }
                it.any { it == "AUTO-OK json" }
            }
            file.exists()
            new ObjectMapper().readTree(file).has("tenantName")
    }
}
