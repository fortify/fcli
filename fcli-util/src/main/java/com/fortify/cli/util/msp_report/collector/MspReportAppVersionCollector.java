package com.fortify.cli.util.msp_report.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportLicenseType;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCProcessedAppVersionDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCProcessedAppVersionDescriptor.MspReportSSCAppVersionProcessingStatus;
import com.fortify.cli.util.msp_report.writer.MspReportResultsWriters;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * <p>This class is responsible for collecting and outputting 
 * {@link MspReportSSCProcessedAppVersionDescriptor} instances.</p>
 * 
 * @author rsenden
 *
 */
@RequiredArgsConstructor
public final class MspReportAppVersionCollector {
    private final MspReportResultsWriters writers;
    private final ObjectNode summary;
    
    private int totalAppVersionCount = 0;
    private int applicationEntitlementsConsumed = 0;
    private int scanEntitlementsConsumed = 0;
    private Map<MspReportSSCAppVersionProcessingStatus, Integer> countsByProcessingStatus = new HashMap<>();
    private Map<MspReportSSCAppVersionAttrStatus, Integer> countsByAttrStatus = new HashMap<>();
    
    
    @SneakyThrows
    public void report(IUrlConfig urlConfig, MspReportSSCProcessedAppVersionDescriptor descriptor) {
        totalAppVersionCount++;
        applicationEntitlementsConsumed += descriptor.getAppVersionEntitlementSummaryDescriptor().getApplicationEntitlementsConsumed();
        scanEntitlementsConsumed += descriptor.getAppVersionEntitlementSummaryDescriptor().getScanEntitlementsConsumed();
        increaseCountByProcessingStatus(descriptor.getStatus());
        increaseAttrStatusCounts(descriptor.getAppVersionDescriptor());
        writers.appVersionWriter().write(urlConfig, descriptor);
    }

    void writeResults() {
        writeAppVersionCounts();
        writeEntitlementsConsumedCounts();
    }

    private void writeAppVersionCounts() {
        ObjectNode appVersionCounts = JsonHelper.getObjectMapper().createObjectNode();
        appVersionCounts.put("total", totalAppVersionCount);
        Stream.of(MspReportSSCAppVersionProcessingStatus.values())
            .forEach(status->appVersionCounts.put(status.name(), getCountByProcessingStatus(status)));
        Stream.of(MspReportSSCAppVersionAttrStatus.values())
            .forEach(status->appVersionCounts.put(status.name(), getCountByAttrStatus(status)));
        summary.set("applicationVersionCounts", appVersionCounts);
    }
    
    private void writeEntitlementsConsumedCounts() {
        ObjectNode entitlementsConsumedCounts = JsonHelper.getObjectMapper().createObjectNode();
        entitlementsConsumedCounts.put("application", applicationEntitlementsConsumed);
        entitlementsConsumedCounts.put("scan", scanEntitlementsConsumed);
        summary.set("entitlementsConsumed", entitlementsConsumedCounts);
    }
    
    private void increaseCountByProcessingStatus(MspReportSSCAppVersionProcessingStatus status) {
        countsByProcessingStatus.put(status, getCountByProcessingStatus(status)+1);
    }

    private Integer getCountByProcessingStatus(MspReportSSCAppVersionProcessingStatus status) {
        return countsByProcessingStatus.getOrDefault(status, 0);
    }
    
    private void increaseAttrStatusCounts(MspReportSSCAppVersionDescriptor appVersionDescriptor) {
        increaseMspLicenseTypeAttrStatusCounts(appVersionDescriptor.getMspLicenseType());
        increaseEndCustomerNameAttrStatusCounts(appVersionDescriptor.getMspEndCustomerName());
        increaseEndCustomerLocationAttrStatusCounts(appVersionDescriptor.getMspEndCustomerLocation());
    }
    
    private void increaseMspLicenseTypeAttrStatusCounts(MspReportLicenseType mspLicenseType) {
        if ( mspLicenseType==null ) {
            increaseCountByAttrStatus(MspReportSSCAppVersionAttrStatus.missingLicenseType);
        } else {
            switch (mspLicenseType) {
            case Application: 
                increaseCountByAttrStatus(MspReportSSCAppVersionAttrStatus.applicationLicenseType);
                break;
            case Demo:
                increaseCountByAttrStatus(MspReportSSCAppVersionAttrStatus.demoLicenseType);
                break;
            case Scan:
                increaseCountByAttrStatus(MspReportSSCAppVersionAttrStatus.scanLicenseType);
                break;
            }
        }
    }

    private void increaseEndCustomerNameAttrStatusCounts(String mspEndCustomerName) {
        if ( StringUtils.isBlank(mspEndCustomerName) ) {
            increaseCountByAttrStatus(MspReportSSCAppVersionAttrStatus.missingEndCustomerName);
        }
    }
    
    private void increaseEndCustomerLocationAttrStatusCounts(String mspEndCustomerLocation) {
        if ( StringUtils.isBlank(mspEndCustomerLocation) ) {
            increaseCountByAttrStatus(MspReportSSCAppVersionAttrStatus.missingEndCustomerLocation);
        }
    }
    
    private void increaseCountByAttrStatus(MspReportSSCAppVersionAttrStatus status) {
        countsByAttrStatus.put(status, getCountByAttrStatus(status)+1);
    }


    private Integer getCountByAttrStatus(MspReportSSCAppVersionAttrStatus status) {
        return countsByAttrStatus.getOrDefault(status, 0);
    }
    
    private static enum MspReportSSCAppVersionAttrStatus {
        demoLicenseType, applicationLicenseType, scanLicenseType, missingLicenseType, missingEndCustomerName, missingEndCustomerLocation
    }
}
