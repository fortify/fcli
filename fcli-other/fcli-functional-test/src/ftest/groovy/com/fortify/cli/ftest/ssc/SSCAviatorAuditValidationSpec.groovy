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

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("ssc.aviator.audit.validation")
class SSCAviatorAuditValidationSpec extends FcliBaseSpec {
    def "aviator ssc audit rejects skip-if-exceeding-quota with folder-priority-order"() {
        when:
            def result = Fcli.run(
                "aviator ssc audit --av BULK_AUDIT:1.6 --app qoflow2 --skip-if-exceeding-quota --folder-priority-order High",
                { it.expectSuccess(false) })
        then:
            verifyAll(result) {
                nonZeroExitCode
                stderr.any { line ->
                    line.contains("--skip-if-exceeding-quota") && line.contains("--folder-priority-order")
                }
            }
    }

    def "ssc bulkaudit action rejects skip-if-exceeding-quota with folder-priority-order"() {
        when:
            def result = Fcli.run(
                "ssc action run bulkaudit --progress=none --on-unsigned=ignore --on-invalid-version=ignore --add-aviator-tags --skip-if-exceeding-quota --folder-priority-order High",
                { it.expectSuccess(false) })
        then:
            verifyAll(result) {
                nonZeroExitCode
                stderr.any { line ->
                    line.contains("--skip-if-exceeding-quota and --folder-priority-order cannot be used together")
                }
            }
    }
}