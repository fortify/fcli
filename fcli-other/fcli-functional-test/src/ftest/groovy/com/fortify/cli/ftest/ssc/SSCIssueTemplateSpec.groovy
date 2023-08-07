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
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.issue-template") @FcliSession(SSC) @Stepwise
class SSCIssueTemplateSpec extends FcliBaseSpec {
    
    def "list"() {
        def args = "ssc issue-template list --store templates"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdNameInuseDefaulttemplatePublishversionOriginalfilenameDescription")
                it.any { it.startsWith(" PCI") }
            }
    }
    
    def "get.byId"() {
        def args = "ssc issue-template get ::templates::get(0).id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.any { it.startsWith("defaultTemplate:") }
            }
    }
    
    def "get.byName"() {
        def args = "ssc issue-template get ::templates::get(0).name"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.any { it.startsWith("defaultTemplate:") }
            }
    }
    //TODO add tests for create,delete,download,update
}