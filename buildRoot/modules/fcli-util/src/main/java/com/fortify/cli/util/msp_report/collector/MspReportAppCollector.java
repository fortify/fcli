/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.msp_report.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.util.Counter;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportLicenseType;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportProcessingStatus;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCProcessedAppDescriptor;
import com.fortify.cli.util.msp_report.writer.MspReportResultsWriters;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * <p>This class is responsible for collecting and outputting 
 * {@link MspReportSSCProcessedAppDescriptor} instances.</p>
 * 
 * @author rsenden
 *
 */
@RequiredArgsConstructor
public final class MspReportAppCollector {
    private final MspReportResultsWriters writers;
    private final ObjectNode summary;
    
    private Counter totalAppCounter = new Counter();
    private Counter applicationEntitlementsConsumedCounter = new Counter();
    private Counter scanEntitlementsConsumedCounter = new Counter();
    private Map<MspReportProcessingStatus, Counter> countsByProcessingStatus = new HashMap<>();
    private Map<MspReportSSCAppAttrStatus, Counter> countsByAttrStatus = new HashMap<>();
    
    
    @SneakyThrows
    public void report(IUrlConfig urlConfig, MspReportSSCProcessedAppDescriptor descriptor) {
        totalAppCounter.increase();
        var summaryDescriptor = descriptor.getAppSummaryDescriptor();
        applicationEntitlementsConsumedCounter.increase(summaryDescriptor.getConsumedApplicationEntitlementsCounter());
        scanEntitlementsConsumedCounter.increase(summaryDescriptor.getConsumedScanEntitlementsCounter());
        increaseCountByProcessingStatus(descriptor.getStatus());
        increaseMspLicenseTypeAttrStatusCounts(descriptor.getAppDescriptor().getMspLicenseType());
        writers.appsWriter().write(urlConfig, descriptor);
    }

    void writeResults() {
        writeEntitlementsConsumedCounts();
        writeAppCounts();
    }

    private void writeAppCounts() {
        ObjectNode appCounts = JsonHelper.getObjectMapper().createObjectNode();
        appCounts.put("total", totalAppCounter.getCount());
        Stream.of(MspReportProcessingStatus.values())
            .forEach(status->appCounts.put("appsWith"+StringUtils.capitalize(status.name()), getCounterByProcessingStatus(status).getCount()));
        Stream.of(MspReportSSCAppAttrStatus.values())
            .forEach(status->appCounts.put(status.name(), getCounterByAttrStatus(status).getCount()));
        summary.set("applicationCounts", appCounts);
    }
    
    private void writeEntitlementsConsumedCounts() {
        ObjectNode entitlementsConsumedCounts = JsonHelper.getObjectMapper().createObjectNode();
        entitlementsConsumedCounts.put("application", applicationEntitlementsConsumedCounter.getCount());
        entitlementsConsumedCounts.put("scan", scanEntitlementsConsumedCounter.getCount());
        summary.set("entitlementsConsumed", entitlementsConsumedCounts);
    }
    
    private void increaseMspLicenseTypeAttrStatusCounts(MspReportLicenseType mspLicenseType) {
        if ( mspLicenseType==null ) {
            throw new IllegalStateException("MSP license type not defined");
        } else {
            switch (mspLicenseType) {
            case Application: 
                increaseCountByAttrStatus(MspReportSSCAppAttrStatus.appsWithApplicationLicense);
                break;
            case Demo:
                increaseCountByAttrStatus(MspReportSSCAppAttrStatus.appsWithDemoLicense);
                break;
            case Scan:
                increaseCountByAttrStatus(MspReportSSCAppAttrStatus.appsWithScanLicense);
                break;
            }
        }
    }
    
    private void increaseCountByAttrStatus(MspReportSSCAppAttrStatus status) {
        getCounterByAttrStatus(status).increase();
    }

    private Counter getCounterByAttrStatus(MspReportSSCAppAttrStatus status) {
        return countsByAttrStatus.computeIfAbsent(status, x->new Counter());
    }
    
    private void increaseCountByProcessingStatus(MspReportProcessingStatus status) {
        getCounterByProcessingStatus(status).increase();
    }

    private Counter getCounterByProcessingStatus(MspReportProcessingStatus status) {
        return countsByProcessingStatus.computeIfAbsent(status, x->new Counter());
    }
    
    private static enum MspReportSSCAppAttrStatus {
        appsWithDemoLicense, appsWithApplicationLicense, appsWithScanLicense
    }
}
