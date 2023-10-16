package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FoDMicroservicesAppSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("fod.release") @FcliSession(FOD) @Stepwise
class FoDReleaseSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FoDMicroservicesAppSupplier app = new FoDMicroservicesAppSupplier()

    def "list"() {
        def args = "fod release list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=1
                it[0].replace(' ', '').equals("IdNameMicroserviceApplicationSDLCStatus")
            }
    }

    def "create"() {
        def args = "fod release create --release=${app.get().qualifiedMicroserviceName}:testrel --sdlc-status=Development --store testrel"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
            }
    }

    def "verifyCreated"() {
        def args = "fod release list --store releases"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains(app.get().appName) \
                    && it.contains(app.get().microserviceName) \
                    && it.contains("testrel") }
            }
    }

    def "get.byId"() {
        def args = "fod release get ::testrel::releaseId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.any { it.contains("releaseName: \"testrel\"") }
                it.any { it.contains("applicationName: \"${app.get().appName}\"") }
            }
    }

    def "get.byName"() {
        def args = "fod release get ${app.get().qualifiedMicroserviceName}:testrel"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.any { it.contains("releaseName: \"testrel\"") }
                it.any { it.contains("applicationName: \"${app.get().appName}\"") }
            }
    }


    def "update"() {
        def args = "fod release update ${app.get().qualifiedMicroserviceName}:testrel --sdlc-status QA"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }

    def "verifyUpdated"() {
        def args = "fod release get ::testrel::releaseId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any {it.equals("sdlcStatusType: \"QA\"") }
            }
    }

    def "delete"() {
        def args = "fod release delete ${app.get().qualifiedMicroserviceName}:testrel"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }

    def "verifyDeleted"() {
        def args = "fod release list --app ${app.get().appName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                !it.any { it.contains("testrel") }
            }
    }

}

