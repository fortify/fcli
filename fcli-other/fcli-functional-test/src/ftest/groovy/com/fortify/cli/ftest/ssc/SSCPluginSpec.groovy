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

@Prefix("ssc.plugin") @FcliSession(SSC) @Stepwise
class SSCPluginSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/ssc/fortify-ssc-parser-sample-1.0.2.jar") String samplePlugin
    
    
    def "listAll"() {
        def args = "ssc plugin list --store plugins"
        when:
            def result = Fcli.run(args)
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
    
    def "install"() {
        
            def args = "ssc plugin install -f $samplePlugin"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    it.last().contains("INSTALLED")
                }
    }
    
    def "listNewlyInstalled"() {
        def args = "ssc plugin list --store plugin -q pluginId=='com.example.ssc.parser.sample.alternative'"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0].replace(' ', '').equals("IdPluginidPlugintypePluginnamePluginversionPluginstate")
            }
    }
    
    def "disable"() {
        
            def args = "ssc plugin disable ::plugin::get(0).id"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    it.any { it.contains("DISABLED") }
                }
    }
    
    def "verifyDisabled"() {
        
            def args = "ssc plugin get ::plugin::get(0).id"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    it.any { it.equals("pluginName: \"Alternative sample parser plugin\"") }
                    it.any { it.equals("pluginState: \"STOPPED\"") }
                }
    }
    
    def "get.byId"() {
        
            def args = "ssc plugin get ::plugin::get(0).id"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    it.any { it.equals("pluginName: \"Alternative sample parser plugin\"") }
                    it.any { it.equals("pluginState: \"STOPPED\"") }
                }
    }
    
    def "enable"() {
        
            def args = "ssc plugin enable ::plugin::get(0).id"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    it.any { it.contains("ENABLED") }
                }
    }
    
    def "verifyEnabled"() {
        
            def args = "ssc plugin get ::plugin::get(0).id"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    it.any { it.equals("pluginName: \"Alternative sample parser plugin\"") }
                    it.any { it.equals("pluginState: \"STARTED\"") }
                }
    }
    
    def "uninstall"() {
        
            def args = "ssc plugin uninstall ::plugin::get(0).id"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()==2
                    it[1].contains("UNINSTALLED")
                }
    }
    
    def "verifyUninstalled"() {
        
            def args = "ssc plugin list"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    if(size()>1) {
                        !it.any { it.contains("com.example.ssc.parser.sample.alternative") }
                    } else {
                        it[0].equals("No data")
                    }
                }
    }
}
