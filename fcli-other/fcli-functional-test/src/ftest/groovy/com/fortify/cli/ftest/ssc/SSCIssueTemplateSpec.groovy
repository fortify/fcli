/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.ftest.ssc

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.issue-template") @FcliSession(SSC) @Stepwise
class SSCIssueTemplateSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/ssc/issueTemplate.xml") String templateFile
    private static final String random = System.currentTimeMillis()
    private static final String templateName = "fcli-test-Template"+random
    
    def "list"() {
        def args = "ssc issue-template list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdNameInuseDefaulttemplatePublishversionOriginalfilenameDescription")
                it.any { it.startsWith(" PCI") }
            }
    }
    
    def "create"() {
        def args = "ssc issue-template create $templateName -f $templateFile -d auto\\ created\\ by\\ test"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdNameInuseDefaulttemplatePublishversionOriginalfilenameDescriptionAction")
                it[1].contains(templateName)
                
            }
    }
    
    def "get.byName"() {
        def args = "ssc issue-template get $templateName --store template"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].equals("name: \"" + templateName + "\"")
            }
    }
    
    def "get.byId"() {
        def args = "ssc issue-template get ::template::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].equals("name: \"" + templateName + "\"")
            }
    }
    
    def "update"() {
        def args = "ssc issue-template update ::template::id -n updatedName -d updatedDescr --set-as-default"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[1].replace(" ", "").contains("updatedNamefalsetrue")
                it[1].contains("updatedDescr")
            }
    }
    
    def "verifyUpdate"() {
        def args = "ssc issue-template get ::template::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].equals("name: \"updatedName\"")
            }
    }
    
    def "download"() {
        def args = "ssc issue-template download ::template::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.last().contains("DOWNLOADED")
            }
    }
    
    def "delete"() {
        def args = "ssc issue-template delete ::template::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdNameInuseDefaulttemplatePublishversionOriginalfilenameDescriptionAction")
                it[1].contains("DELETED")
            }
    }
}
