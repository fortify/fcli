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

@Prefix("ssc.plugin") @FcliSession(SSC) @Stepwise
class SSCPluginSpec extends FcliBaseSpec {
    @Shared
    boolean pluginsExist = false;
    
    def "list"() {
        def args = "ssc plugin list"
        when:
            def result = Fcli.run(args)
            pluginsExist = result.stdout.size()>1
        then:
            verifyAll(result.stdout) {
                size()>=0
                if(size()>1) {
                    it[0].replace(' ', '').equals("IdPluginidPlugintypePluginnamePluginversionPluginstate")
                } else {
                it[0].equals("No data")
                }
            }
    }
    
    def "get.byId"() {
        
            def args = "ssc alert-definition get ::alertdefinitions::get(0).id"
            when:
                if(!pluginsExist) {return;}
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    it.any { it.startsWith("alertTriggers:") }
                }
    }
    
    //TODO add tests for install, uninstall, enable, disable, get
}
