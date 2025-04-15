package com.fortify.cli.ftest.sc_sast;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("sc-sast.scan") @FcliSession(SSC) @Stepwise
@Requires({System.getProperty('ft.ssc.user') && System.getProperty('ft.ssc.password')})
class SCSastScanSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/shared/EightBall-package.zip") String packageZip
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.mbs") String packageMbs
    @Shared @TestResource("runtime/shared/Filter.txt") String fileFilter
    String user = System.getProperty('ft.ssc.user');
    String pass = System.getProperty('ft.ssc.password');
    
    def "help"() {
        def args = "sc-sast scan -h"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=19
                it[0].equals("Manage ScanCentral SAST scans.")
                it.any { it.contains("cancel    Cancel a previously submitted scan request.") }
                it.any { it.contains("start     Start a SAST scan.") }
                it.any { it.contains("status    Get status for a previously submitted scan request.") }
                it.any { it.contains("wait-for  Wait for one or more scans to reach or exit specified scan statuses.") }
            }
    }
    
    def "listSensors"() {
        def args = "sc-sast sensor list -q state=='ACTIVE'&&cloudPool!=null --store sensors"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>1
                it[0].replace(' ', '').equals("HostnameStatePoolnameIpaddressScaversionLastseenSensorstarttime")
            }
    }
    
    def "extractHighestVersionSensor"() {
        def args = "util variable contents sensors -q uuid==#var('sensors').get(#var('sensors').size()-1).uuid --store highestSensor -o table=hostName,scaVersion"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(' ', '').equals("hostNamescaVersion")
            }
    }
	
	def "startScanFilter"() {
		def args = "sc-sast scan start -v ::highestSensor::get(0).scaVersion -f=$packageZip --sargs -quick\\ -filter\\ @$fileFilter --store scanfilter"
		when:
			def result = Fcli.run(args)
		then:
			verifyAll(result.stdout) {
				size()==2
				it[0].replace(' ', '').equals("JobtokenHasfilesScanstatePublishstateSSCprocessingstateEndpointversionAction")
			}
	}
	
	def "wait-for"() {
		// Depending on externalmetadata versions in FPR and on SSC, approval
		// may be required
		def args = "sc-sast scan wait-for ::scanfilter:: -i 2s --until=all-match --any-scan-state=COMPLETED,CANCELLED,FAILED"
		when:
			def result = Fcli.run(args)
		then:
			verifyAll(result.stdout) {
				//
			}
	}
	
	def "verifyScanRule"() {
		def args = "ssc rest call ::scanfilter::'/api/v1/cloudjobs/'+jobToken -o expr={scaArgs}"
		when:
			def result = Fcli.run(args)
		then:
			verifyAll(result.stdout) {
				it[0].contains("-quick")
				it[0].contains("-filter")
			}
	}
}