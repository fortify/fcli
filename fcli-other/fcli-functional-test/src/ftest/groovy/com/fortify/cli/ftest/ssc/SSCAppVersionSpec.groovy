package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.appversion") @FcliSession(SSC) @Stepwise
class SSCAppVersionSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersionSupplier versionSupplier = new SSCAppVersionSupplier()
    @Shared @AutoCleanup SSCAppVersionSupplier versionSupplier2 = new SSCAppVersionSupplier()
    
    def "list"() {
        def args = "ssc appversion list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(" ","").equals("IdApplicationnameNameIssuetemplatenameCreatedby");
            }
    }
    
    def "get.byName"() {
        def args = "ssc appversion get ${versionSupplier.version.appName}:${versionSupplier.version.versionName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[1].equals("id: " + versionSupplier.version.get("id"));
            }
    }
    
    def "get.byId"() {
        def args = "ssc appversion get ${versionSupplier.version.get("id")}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[4].equals("  name: \"" + versionSupplier.version.appName + "\"");
                it[9].equals("name: \"" + versionSupplier.version.versionName + "\"");
            }
    }
    
    def "updateName"() {
        def args = "ssc appversion update ${versionSupplier.version.get("id")} --name updatedVersionName --description updated1 --attrs=DevPhase=Retired -o table=name,description"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("updatedVersionName");
            }
    }
    
    def "updateNameWithMatchingAppName"() {
        def args = "ssc appversion update ${versionSupplier.version.get("id")} --name ${versionSupplier.version.appName}:updatedVersionName2 --description updated2 -o table=name,description"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("updatedVersionName2");
            }
    }
    
    def "updateNameWithMatchingAppNameAndCustomDelimiter"() {
        def args = "ssc appversion update ${versionSupplier.version.get("id")} --name ${versionSupplier.version.appName}|updatedVersionName3 --description updated2 --delim | -o table=name,description"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("updatedVersionName3");
                !it[1].contains("|")
            }
    }
    
    def "updateNameWithNonMatchingAppName"() {
        def args = "ssc appversion update ${versionSupplier.version.get("id")} --name nonExistingAppversion123:updatedVersionName3 --description updated3"
        when:
            def result = Fcli.run(args)
        then:
            def e = thrown(UnexpectedFcliResultException)
            verifyAll(e.result.stderr) {
                it[0].startsWith("java.lang.IllegalArgumentException: --name option must contain either a plain name or ${versionSupplier.version.appName}:<new name>, current: nonExistingAppversion123:updatedVersionName3")
            }
    }
    
    /*
    def "createWithCopy"() {
        def args = "ssc appversion create --from=10060 --auto-required-attrs --issue-template=Prioritized\\ High\\ Risk\\ Issue\\ Template ${versionSupplier.version.appName}:copied --store=copied"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].endsWith("CREATED ")
            }
    }
    
    def "verifyCopy"() {
        Thread.sleep(5000)
        def args = "ssc issue count --appversion=::copied::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==3
                it[1].contains("High        2")
            }
    }
    
    def "copy-state"() {
        def args = "ssc appversion copy-state --from=10060 --to=${versionSupplier2.version.get("id")}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].endsWith("COPY_REQUESTED ")
            }
    }
    
    def "verifyCopy2"() {
        Thread.sleep(5000)
        def args = "ssc issue count --appversion=${versionSupplier2.version.get("id")}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==3
                it[1].contains("High        2")
            }
    }*/
    //TODO add tests to verify copying of attributes once that is implemented 
    //the copy action in the UI using the /bulk endpoint sets a copyVersionAttributes flag which doesnt seem to do anything atm
    //waiting for feedback from PM if that is supposed to be working, if not Alex plans to implement it client side
}
