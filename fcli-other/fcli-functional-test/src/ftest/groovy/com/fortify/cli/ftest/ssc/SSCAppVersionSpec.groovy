package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.appversion") @FcliSession(SSC) @Stepwise
class SSCAppVersionSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersion version = new SSCAppVersion().create()
    
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
        def args = "ssc appversion get " + version.appName + ":" + version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[1].equals("id: " + version.get("id"));
            }
    }
    
    def "get.byId"() {
        def args = "ssc appversion get " + version.get("id")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[4].equals("  name: \"" + version.appName + "\"");
                it[9].equals("name: \"" + version.versionName + "\"");
            }
    }
    
    def "updateName"() {
        def args = "ssc appversion update " + version.get("id") + " --name updatedVersionName --description updated1"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("updatedVersionName");
            }
    }
    
    def "updateNameWithMatchingAppName"() {
        def args = "ssc appversion update " + version.get("id") + " --name " + version.appName + ":updatedVersionName2 --description updated2"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("updatedVersionName2");
            }
    }
    
    def "updateNameWithMatchingAppNameAndCustomDelimiter"() {
        def args = "ssc appversion update " + version.get("id") + " --name " + version.appName + "|updatedVersionName3 --description updated2 --delim |"
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
        def args = "ssc appversion update " + version.get("id") + " --name nonExistingAppversion123:updatedVersionName3 --description updated3"
        when:
            def result = Fcli.run(args)
        then:
            def e = thrown(UnexpectedFcliResultException)
            verifyAll(e.result.stderr) {
                it[0].startsWith("java.lang.IllegalArgumentException: --name option must contain either a plain name or ${version.appName}:<new name>, current: nonExistingAppversion123:updatedVersionName3")
            }
    }
}