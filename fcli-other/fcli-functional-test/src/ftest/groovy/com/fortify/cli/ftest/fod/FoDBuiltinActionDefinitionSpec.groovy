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
package com.fortify.cli.ftest.fod

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared

@Prefix("fod.action.def")
class FoDBuiltinActionDefinitionSpec extends FcliBaseSpec {
    
    def "list"() {
        def args = "fod action list -q origin=='FCLI'"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>1
                it[0].replace(' ', '').equals("NameAuthorOriginStatusSignatureUsageheader")
                // TODO Is this working correctly? Ideally, we should ignore empty lines,
                //      rather than lines not containing FCLI, but that doesn't work.
                it[2..-1].every { 
                    !it.contains('FCLI') || it.replace(' ', '').contains("FortifyFCLIVALIDVALID") 
                }
            }
    }
    
    def "help"() {
        def args = "fod action help ${action}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>1
                it.any {
                    it.contains('Origin:') && it.contains('FCLI')
                    it.contains('Signature status:') && it.contains('VALID')
                    it.contains('Author:') && it.contains('Fortify')
                    it.contains('Signed by:') && it.contains('Fortify')
                    it.contains('Certified by:') && it.contains('Fortify')
                }
            }
        where:
            action << Fcli.run("fod action list -q origin=='FCLI' -o expr={name}\\n").stdout
    }
}
