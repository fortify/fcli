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

import spock.lang.AutoCleanup
import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.alert-definition") @FcliSession(SSC) @Stepwise
class SSCAlertDefinitionSpec extends FcliBaseSpec {
    @Shared
    static boolean definitionsExist = false;
    
    def "list"() {
        def args = "ssc alert lsd --store alertdefinitions"
        given:
            def result = Fcli.run(args)
        when:
            definitionsExist = result.stdout.size()>1
        then:
            verifyAll(result.stdout) {
                size()>=1
                if(size()>1) {
                it[0].replace(' ', '').equals("IdNameCreatedbyRecipienttypeMonitoredentitytypeTriggerdescriptionname")
                } else {
                it[0].equals("No data")
                }
            }
    }
    
    
    def "get.byId"() {
        
            def args = "ssc alert getdef ::alertdefinitions::get(0).id"
            when:
                if(!definitionsExist) {return;}
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    it.any { it.startsWith("alertTriggers:") }
                }
    }
}
