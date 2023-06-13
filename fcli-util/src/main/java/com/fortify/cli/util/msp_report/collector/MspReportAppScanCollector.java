package com.fortify.cli.util.msp_report.collector;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.config.MspReportConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportLicenseType;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppSummaryDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCScanDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCScanType;
import com.fortify.cli.util.msp_report.writer.MspReportResultsWriters;

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
    
    public MspReportSSCAppSummaryDescriptor summary() {
        return summaryDescriptor;
    }
    
    /**
     * Report an SSC application version scan. 
     * @return {@link MspReportScanCollectorState#DONE} is we're done processing
     *         this application version, {@link MspReportScanCollectorState#DONE}
     *         otherwise.
     */
    @SneakyThrows
    public MspReportScanCollectorState report(MspReportSSCScanDescriptor scanDescriptor) {
        processedScans.add(scanDescriptor);
        addEarliestScans(scanDescriptor);
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
    private MspReportScanCollectorState continueOrDone(MspReportSSCScanDescriptor scanDescriptor) {
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
        return MspReportScanCollectorState.CONTINUE;
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
    
    private void addEarliestScans(MspReportSSCScanDescriptor scanDescriptor) {
        if ( scanDescriptor.getScanType().isFortifyScan() ) {
            addEarliestScanByType(scanDescriptor.getScanType(), scanDescriptor);
            addEarliestUploadByScanId(scanDescriptor.getScanId(), scanDescriptor);
        }
    }
    
    private void addEarliestScanByType(MspReportSSCScanType type, MspReportSSCScanDescriptor scanDescriptor) {
        var previousEarliestScanDescriptor = earliestScanByType.get(type);
        if ( previousEarliestScanDescriptor==null || previousEarliestScanDescriptor.getScanDate().isAfter(scanDescriptor.getScanDate()) ) {
           earliestScanByType.put(type, scanDescriptor);
        }
    }
    
    private void addEarliestUploadByScanId(String scanId, MspReportSSCScanDescriptor scanDescriptor) {
        var previousEarliestScanDescriptor = earliestUploadByScanId.get(scanId);
        if ( previousEarliestScanDescriptor==null || previousEarliestScanDescriptor.getArtifactUploadDate().isAfter(scanDescriptor.getArtifactUploadDate()) ) {
            earliestUploadByScanId.put(scanId, scanDescriptor);
        }
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
        var entitlementConsumedReason = getEntitlementConsumedReason(descriptor, type);
        var entitlementConsumptionDate = getEntitlementConsumptionDate(descriptor, type, entitlementConsumedReason);
        return new MspReportProcessedScanDescriptor(urlConfig, appDescriptor, descriptor, type, entitlementConsumedReason, entitlementConsumptionDate);
    }
    
    private MspReportArtifactEntitlementConsumedReason getEntitlementConsumedReason(MspReportSSCScanDescriptor descriptor, MspReportSSCScanType type) {
        var scanDate = descriptor.getScanDate();
        
        if ( isBeforeContractStartDate(scanDate) ) {
            return MspReportArtifactEntitlementConsumedReason.beforeContractStartDate;
        } else if ( isBeforeReportingStartDate(scanDate) ) {
            return MspReportArtifactEntitlementConsumedReason.beforeReportingPeriod;
        } else if ( isAfterReportingEndDate(scanDate) ) {
            return MspReportArtifactEntitlementConsumedReason.afterReportingPeriod;
        } 
        if ( !type.isFortifyScan() ) {
            return MspReportArtifactEntitlementConsumedReason.noFortifyScan;
        } else {
            switch ( appDescriptor.getMspLicenseType() ) {
            case Demo: 
                return MspReportArtifactEntitlementConsumedReason.demoScan;
            case Scan: 
                if ( descriptor.equals(earliestUploadByScanId.get(descriptor.getScanId())) ) {
                    return MspReportArtifactEntitlementConsumedReason.mspScanLicenseConsumed;
                } else {
                    return MspReportArtifactEntitlementConsumedReason.mspScanLicenseConsumedEarlier;
                }
            case Application:
                if ( descriptor.equals(earliestScanByType.get(type)) ) {
                    return MspReportArtifactEntitlementConsumedReason.mspApplicationLicenseConsumed;
                } else {
                    return MspReportArtifactEntitlementConsumedReason.mspApplicationLicenseConsumedEarlier;
                }
            default: throw new RuntimeException("Unable to determine entitlement consumed reason");
            }
        }
    }

    private LocalDateTime getEntitlementConsumptionDate(MspReportSSCScanDescriptor descriptor, MspReportSSCScanType type, MspReportArtifactEntitlementConsumedReason entitlementConsumedReason) {
        var isDemoLicense = appDescriptor.getMspLicenseType()==MspReportLicenseType.Demo;
        switch ( entitlementConsumedReason.getEntitlementConsumed() ) {
        case none:
            if ( isDemoLicense || !type.isFortifyScan() ) { return null; }
            // Intentionally no break;
        case application:
            return earliestScanByType.get(type).getScanDate();
        case scan:
            return earliestUploadByScanId.get(descriptor.getScanId()).getScanDate();
        default: throw new RuntimeException("Unable to determine entitlement consumed date");
        }
    }


    public static enum MspReportScanCollectorState {
        CONTINUE, DONE
    }
    
    @RequiredArgsConstructor
    public static enum MspReportArtifactEntitlementConsumedReason {
        beforeContractStartDate(MspReportArtifactEntitlementConsumed.none, true),
        beforeReportingPeriod(MspReportArtifactEntitlementConsumed.none, true),
        afterReportingPeriod(MspReportArtifactEntitlementConsumed.none, true),
        noFortifyScan(MspReportArtifactEntitlementConsumed.none, false),
        demoScan(MspReportArtifactEntitlementConsumed.none, false),
        mspScanLicenseConsumed(MspReportArtifactEntitlementConsumed.scan, false),
        mspScanLicenseConsumedEarlier(MspReportArtifactEntitlementConsumed.none, false),
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
