package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FoDMicroservicesAppSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("fod.microservice") @FcliSession(FOD) @Stepwise
class FoDMicroserviceSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FoDMicroservicesAppSupplier app = new FoDMicroservicesAppSupplier()

    @Shared
    boolean appsExist = false;

    def "list"() {
        def args = "fod microservice list --app=${app.get().appName}"
        when:
            def result = Fcli.run(args)
            appsExist = result.stdout.size()>1
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains(app.get().appName)
            }
    }

    def "create"() {
        def args = "fod microservice create ${app.get().appName}:testservice"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
            }
    }

    def "verifyCreated"() {
        def args = "fod microservice list --app=${app.get().appName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("testservice") }
            }
    }

    def "update"() {
        def args = "fod microservice update ${app.get().appName}:testservice --name=updatedtestservice"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }

    def "verifyUpdated"() {
        def args = "fod microservice list --app=${app.get().appName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains(app.get().appName) && it.contains("updatedtestservice") }
            }
    }

    def "delete"() {
        def args = "fod microservice delete ${app.get().appName}:updatedtestservice"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }

    def "verifyDeleted"() {
        def args = "fod microservice list --app=${app.get().appName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                !it.any { it.contains("updatedtestservice") }
            }
    }

}

