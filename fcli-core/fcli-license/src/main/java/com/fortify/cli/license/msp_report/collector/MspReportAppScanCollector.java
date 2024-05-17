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
package com.fortify.cli.license.msp_report.collector;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.license.msp_report.config.MspReportConfig;
import com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCAppDescriptor;
import com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCAppSummaryDescriptor;
import com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCScanDescriptor;
import com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCScanType;
import com.fortify.cli.license.msp_report.writer.MspReportResultsWriters;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * <p>This class is responsible for collecting and outputting 
 * {@link MspReportSSCScanDescriptor} instances for a single 
 * application.</p>
 * 
 * @author rsenden
 *
 */
@RequiredArgsConstructor
public final class MspReportAppScanCollector implements AutoCloseable {
    private final MspReportConfig reportConfig;
    private final MspReportResultsWriters writers;
    private final IUrlConfig urlConfig;
    private final MspReportSSCAppDescriptor appDescriptor;
    private final MspReportSSCAppSummaryDescriptor summaryDescriptor = new MspReportSSCAppSummaryDescriptor();
    private final List<MspReportSSCScanDescriptor> processedScans = new ArrayList<>();
    private final Map<MspReportSSCScanType, MspReportSSCScanDescriptor> earliestScanByType = new HashMap<>();
    private final Map<String, MspReportSSCScanDescriptor> earliestUploadByScanId = new HashMap<>();
    private final Map<MspReportSSCScanType, TreeSet<MspReportSSCScanDescriptor>> sortedScansByType = new HashMap<>();
    
    public MspReportSSCAppSummaryDescriptor summary() {
        return summaryDescriptor;
    }
    
    /**
     * Report an SSC application version scan. 
     * @return {@link Break#TRUE} is we're done processing
     *         this application version, {@link Break#TRUE}
     *         otherwise.
     */
    @SneakyThrows
    public Break report(MspReportSSCScanDescriptor scanDescriptor) {
        addScan(scanDescriptor);
        return continueOrDone(scanDescriptor);
    }
    
    /**
     * <p>For now, this method always returns CONTINUE, as it only provides a 
     * performance optimalization and needs to be revisited for correct behavior 
     * with regards to handling audit-only uploads and remediation scans (i.e., 
     * original artifact uploaded before reporting start date or even contract 
     * start date, and audit-only artifact or remediation scan uploaded within 
     * reporting period).</p>
     * 
     * <h3>Original behavior (commented out below):</h3>
     * <p>This method returns DONE if we don't need to process any further artifacts 
     * for this application version. We're done processing artifacts if:</p>
     * <ul>
     *  <li>Upload date is before contract start date.</li>
     *  <li>License type is Scan or Demo, and upload date is before reporting 
     *      start date.</li>
     * <ul>
     * <p>Note that we need to check upload date instead of scan date as artifacts 
     * are ordered by descending upload date, and upload date should always be
     * more recent than scan date. In the following example, we still want to 
     * process artifact #2 even if scan date for #1 is before reporting period 
     * start date:</p>
     * <ol>
     *  <li>uploadDate within reporting period, lastScanDate older than 
     *      reporting period start date</li>
     *  <li>Both uploadDate and lastScanDate within reporting period</li>
     * </ol> 
     */
    private Break continueOrDone(MspReportSSCScanDescriptor scanDescriptor) {
        /*
        var licenseType = appDescriptor.getMspLicenseType();
        var uploadDateTime = scanDescriptor.getArtifactUploadDate();
        if ( isBeforeContractStartDate(uploadDateTime) ) { 
            return MspReportScanCollectorState.DONE; 
        }
        else if ( licenseType!=MspReportLicenseType.Application && isBeforeReportingStartDate(uploadDateTime) ) {
            return MspReportScanCollectorState.DONE;
        }
        */
        return Break.FALSE;
    }
    
    private void addScan(MspReportSSCScanDescriptor scanDescriptor) {
        processedScans.add(scanDescriptor);
        if ( scanDescriptor.getScanType().isFortifyScan() ) {
            addSortedScanByType(scanDescriptor);
            addEarliestScanByType(scanDescriptor);
            addEarliestUploadByScanId(scanDescriptor);
        }
    }
    
    private void addSortedScanByType(MspReportSSCScanDescriptor scanDescriptor) {
        var type = scanDescriptor.getScanType();
        sortedScansByType.computeIfAbsent(type, v->new TreeSet<>(this::compareScanDate)).add(scanDescriptor);
    }
    
    private void addEarliestScanByType(MspReportSSCScanDescriptor scanDescriptor) {
        var type = scanDescriptor.getScanType();
        var previousEarliestScanDescriptor = earliestScanByType.get(type);
        if ( previousEarliestScanDescriptor==null || previousEarliestScanDescriptor.getScanDate().isAfter(scanDescriptor.getScanDate()) ) {
           earliestScanByType.put(type, scanDescriptor);
        }
    }
    
    private void addEarliestUploadByScanId(MspReportSSCScanDescriptor scanDescriptor) {
        var scanId = scanDescriptor.getScanId();
        var previousEarliestScanDescriptor = earliestUploadByScanId.get(scanId);
        if ( previousEarliestScanDescriptor==null || previousEarliestScanDescriptor.getArtifactUploadDate().isAfter(scanDescriptor.getArtifactUploadDate()) ) {
            earliestUploadByScanId.put(scanId, scanDescriptor);
        }
    }
    
    private boolean isAfterReportingEndDate(LocalDateTime dateTime) {
        var reportingEndDateTime = reportConfig.getReportingEndDate().atTime(LocalTime.MAX);
        return dateTime.isAfter(reportingEndDateTime);
    }
    
    private boolean isBeforeReportingStartDate(LocalDateTime dateTime) {
        var reportingStartDateTime = reportConfig.getReportingStartDate().atStartOfDay();
        return dateTime.isBefore(reportingStartDateTime);
    }
    
    private boolean isBeforeContractStartDate(LocalDateTime dateTime) {
        var reportingStartDateTime = reportConfig.getContractStartDate().atStartOfDay();
        return dateTime.isBefore(reportingStartDateTime);
    }
    
    /** 
     * Compare scan dates
     */
    private int compareScanDate(MspReportSSCScanDescriptor s1, MspReportSSCScanDescriptor s2) {
        return s1.getScanDate().compareTo(s2.getScanDate());
    }

    public void close() {
        processedScans.stream()
            .map(this::getProcessedScanDescriptor)
            .forEach(this::write);
    }
    
    private void write(MspReportProcessedScanDescriptor descriptor) {
        var writer = writers.processedScansWriter();
        summaryDescriptor.getScansProcessedCounter().increase();
        writer.writeProcessed(descriptor);
        if ( !descriptor.getEntitlementConsumedReason().isOutsideReportingPeriod() ) {
            summaryDescriptor.getScansInReportingPeriodCounter().increase();
            writer.writeInReportingPeriod(descriptor);
        }
        if ( descriptor.getEntitlementConsumed()==MspReportArtifactEntitlementConsumed.application ) {
            summaryDescriptor.getConsumedApplicationEntitlementsCounter().increase();
            writer.writeEntitlementConsuming(descriptor);
        }
        if ( descriptor.getEntitlementConsumed()==MspReportArtifactEntitlementConsumed.scan ) {
            summaryDescriptor.getConsumedScanEntitlementsCounter().increase();
            writer.writeEntitlementConsuming(descriptor);
        }
    }
    
    private MspReportProcessedScanDescriptor getProcessedScanDescriptor(MspReportSSCScanDescriptor descriptor) {
        var type = descriptor.getScanType();
        var scanDate = descriptor.getScanDate();
        MspReportArtifactEntitlementConsumedReason consumptionReason = null;
        LocalDateTime consumptionDate = null;
        
        if ( isBeforeContractStartDate(scanDate) ) {
            consumptionReason = MspReportArtifactEntitlementConsumedReason.beforeContractStartDate;
        } else if ( isBeforeReportingStartDate(scanDate) ) {
            consumptionReason = MspReportArtifactEntitlementConsumedReason.beforeReportingPeriod;
        } else if ( isAfterReportingEndDate(scanDate) ) {
            consumptionReason = MspReportArtifactEntitlementConsumedReason.afterReportingPeriod;
        } else if ( !type.isFortifyScan() ) {
            consumptionReason = MspReportArtifactEntitlementConsumedReason.noFortifyScan;
        } else {
            switch ( appDescriptor.getMspLicenseType() ) {
            case Demo: 
                consumptionReason = MspReportArtifactEntitlementConsumedReason.demoScan; 
                break;
            case Scan: 
                if ( isAuditOnlyScan(descriptor) ) {
                    consumptionReason = MspReportArtifactEntitlementConsumedReason.auditOnlyScan;
                    consumptionDate = earliestUploadByScanId.get(descriptor.getScanId()).getScanDate();
                } else if ( isRemediationScan(descriptor) ) {
                    consumptionReason = MspReportArtifactEntitlementConsumedReason.remediationScan;
                    consumptionDate = getPreviousScan(descriptor).getScanDate();
                } else {
                    consumptionReason = MspReportArtifactEntitlementConsumedReason.mspScanLicenseConsumed;
                    consumptionDate = scanDate;
                }
                break;
            case Application:
                if ( descriptor.equals(earliestScanByType.get(type)) ) {
                    consumptionReason = MspReportArtifactEntitlementConsumedReason.mspApplicationLicenseConsumed;
                    consumptionDate = scanDate;
                } else {
                    consumptionReason = MspReportArtifactEntitlementConsumedReason.mspApplicationLicenseConsumedEarlier;
                    consumptionDate = earliestScanByType.get(type).getScanDate();
                }
                break;
            default: throw new RuntimeException("Unable to determine entitlement consumed reason");
            }
        }
        return new MspReportProcessedScanDescriptor(urlConfig, appDescriptor, descriptor, type, consumptionReason, consumptionDate);
    }


    /**
     * Check if an older scan with the same scan id was found; if so,
     * then this is an audit-only scan.
     */
    private boolean isAuditOnlyScan(MspReportSSCScanDescriptor descriptor) {
        return !descriptor.equals(earliestUploadByScanId.get(descriptor.getScanId()));
    }
    
    /**
     * Check if a scan of the same type exists that is less than 30 days older
     * that the given scan, and which itself is not a remediation scan.
     */
    private boolean isRemediationScan(MspReportSSCScanDescriptor descriptor) {
        var previousScan = getPreviousScan(descriptor);
        return previousScan!=null 
                && ChronoUnit.DAYS.between(previousScan.getScanDate(), descriptor.getScanDate())<30
                && !isRemediationScan(previousScan);
    }
    
    /**
     * Return the previous scan of the same type as the given scan. If there is no previous scan,
     * or if the previous scan is before contract start date, this method returns null. 
     */
    private MspReportSSCScanDescriptor getPreviousScan(MspReportSSCScanDescriptor descriptor) {
        var previousScan = sortedScansByType.get(descriptor.getScanType()).lower(descriptor);
        if ( previousScan==null || isBeforeContractStartDate(previousScan.getScanDate()) ) {
            return null;
        }
        return previousScan;
    }

    @RequiredArgsConstructor
    public static enum MspReportArtifactEntitlementConsumedReason {
        beforeContractStartDate(MspReportArtifactEntitlementConsumed.none, true),
        beforeReportingPeriod(MspReportArtifactEntitlementConsumed.none, true),
        afterReportingPeriod(MspReportArtifactEntitlementConsumed.none, true),
        noFortifyScan(MspReportArtifactEntitlementConsumed.none, false),
        demoScan(MspReportArtifactEntitlementConsumed.none, false),
        mspScanLicenseConsumed(MspReportArtifactEntitlementConsumed.scan, false),
        auditOnlyScan(MspReportArtifactEntitlementConsumed.none, false),
        remediationScan(MspReportArtifactEntitlementConsumed.none, false),
        mspApplicationLicenseConsumed(MspReportArtifactEntitlementConsumed.application, false),
        mspApplicationLicenseConsumedEarlier(MspReportArtifactEntitlementConsumed.none, false),
        ;
        
        @Getter private final MspReportArtifactEntitlementConsumed entitlementConsumed;
        @Getter private final boolean outsideReportingPeriod;
    }
    
    public static enum MspReportArtifactEntitlementConsumed {
        scan, application, none
    }
    
    @Data
    public static final class MspReportProcessedScanDescriptor {
        private final IUrlConfig urlConfig; 
        private final MspReportSSCAppDescriptor appDescriptor;
        private final MspReportSSCScanDescriptor scanDescriptor;
        private final MspReportSSCScanType entitlementScanType;
        private final MspReportArtifactEntitlementConsumedReason entitlementConsumedReason;
        private final LocalDateTime entitlementConsumptionDate;
        private ObjectNode reportNode;
        
        public MspReportArtifactEntitlementConsumed getEntitlementConsumed() {
            return entitlementConsumedReason.getEntitlementConsumed();
        }
        
        public ObjectNode getReportNode() {
            if ( reportNode==null ) {
                var entitlementConsumedReason = getEntitlementConsumedReason();
                var entitlementConsumed = getEntitlementConsumed();
                var entitlementConsumptionDate = getEntitlementConsumptionDate();
                reportNode = 
                    getScanDescriptor().updateReportRecord(
                        getAppDescriptor().updateReportRecord(
                                JsonHelper.getObjectMapper().createObjectNode()
                                    .put("url", getUrlConfig().getUrl())))
                    .put("entitlementScanType", getEntitlementScanType().name())
                    .put("entitlementConsumed", entitlementConsumed.name())
                    .put("entitlementConsumedReason", entitlementConsumedReason.name())
                    .put("entitlementConsumptionDate", entitlementConsumptionDate==null?"N/A":entitlementConsumptionDate.toString());
            }
            return reportNode;
        }
    }
}
